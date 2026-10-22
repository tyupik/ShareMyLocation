package ru.tyupik.sharemylocation.data.map

import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ru.tyupik.sharemylocation.business.map.FriendLocation
import ru.tyupik.sharemylocation.business.map.MapQueryDeniedException
import ru.tyupik.sharemylocation.business.map.MapRepository
import ru.tyupik.sharemylocation.business.sharing.SharingError
import ru.tyupik.sharemylocation.business.sharing.SharingException
import ru.tyupik.sharemylocation.business.sharing.SharingSettings
import ru.tyupik.sharemylocation.data.auth.FirebaseBackend
import ru.tyupik.sharemylocation.data.sharing.SharingRepositoryImpl

/**
 * Реализация карты поверх [FirebaseBackend].
 * Вся работа с Firestore вынесена в Java-класс: пайплайн компиляции AGP 9.4 не может
 * резолвить firebase-артефакты из Kotlin-источников, поэтому Kotlin-слой видит
 * только наш тип [MapRepository] (см. KDoc FriendsRepositoryImpl; helper toSharingException
 * совпадает с [SharingRepositoryImpl] — единый способ заворачивать ошибки backend'а).
 *
 * Realtime-слушатели backend'а проброшены наружу через callbackFlow: данные уходят
 * в канал через trySend, ошибка слушателя закрывает flow — перезапуск возможен
 * по повторной подписке (например, по ScreenShown).
 */
@Singleton
class MapRepositoryImpl @Inject constructor(
    private val backend: FirebaseBackend,
) : MapRepository {

    override fun observeLocationsByUids(uids: List<String>): Flow<Map<String, FriendLocation>> =
        callbackFlow {
            if (uids.isEmpty()) {
                // whereIn с пустым списком Firestore запрещает — пустой список видимых друзей
                // означает пустой результат без слушателей.
                trySend(emptyMap())
                awaitClose { }
                return@callbackFlow
            }
            val chunks = CopyOnWriteArrayList<LocationsChunk>()
            val registrations = CopyOnWriteArrayList<ListenerRegistration>()

            /** Объединённый слепок всех чанков — единственное, что уходит наружу. */
            fun mergedLocations(): Map<String, FriendLocation> =
                chunks.fold(HashMap<String, FriendLocation>()) { acc, chunk ->
                    acc.putAll(chunk.locations)
                    acc
                }

            /**
             * Fallback (rules are not filters): whereIn-чанк с get()-правилом друга
             * не provable и отклоняется целиком (MapQueryDeniedException). По-документные
             * слушатели оцениваются правилами по конкретному документу и работают
             * под тем же правилом — видимость по-прежнему гейтит Firestore.
             */
            fun observeChunkDocuments(chunk: LocationsChunk) {
                chunk.uids.forEach { uid ->
                    registrations.add(
                        backend.listenLocationByUid(uid) { locations, error ->
                            if (error != null) {
                                close(toSharingException(error))
                            } else {
                                chunk.apply(locations.orEmpty())
                            }
                        },
                    )
                }
            }

            uids.chunked(MAX_WHERE_IN_UIDS).forEach { chunk ->
                val locationsChunk = LocationsChunk(chunk) { trySend(mergedLocations()) }
                chunks.add(locationsChunk)
                try {
                    registrations.add(
                        backend.listenLocationsByUids(chunk) { locations, error ->
                            when {
                                // Слушатель после ошибки терминально мёртв — просто уходим
                                // на fallback; регистрация снимется в awaitClose.
                                error is MapQueryDeniedException -> observeChunkDocuments(locationsChunk)
                                error != null -> close(toSharingException(error))
                                else -> locationsChunk.apply(locations.orEmpty())
                            }
                        },
                    )
                } catch (throwable: Throwable) {
                    close(toSharingException(throwable))
                }
            }
            awaitClose { registrations.forEach { registration -> registration.remove() } }
        }

    /** SharingException от backend проходит как есть, остальное заворачивается в Unexpected. */
    private fun toSharingException(throwable: Throwable): SharingException {
        val cause = (throwable as? ExecutionException)?.cause ?: throwable
        return cause as? SharingException
            ?: SharingException(SharingError.Unexpected(cause.message), cause)
    }

    /**
     * Realtime-настройки шаринга по uid: по одному по-документному слушателю на uid
     * (паттерн fallback-ветки observeLocationsByUids), слияние в общую карту и trySend
     * объединённого слепка на каждое обновление. Отсутствующий/запрещённый документ backend
     * уже преобразовал в default («не шарит») — сюда приходят только данные или обрыв.
     */
    override fun observeSharingSettingsByUids(uids: List<String>): Flow<Map<String, SharingSettings>> =
        callbackFlow {
            if (uids.isEmpty()) {
                // Слушателей не создаём: пустой список друзей означает пустой слепок настроек.
                trySend(emptyMap())
                awaitClose { }
                return@callbackFlow
            }
            val mergedSettings = HashMap<String, SharingSettings>()
            val registrations = CopyOnWriteArrayList<ListenerRegistration>()
            uids.forEach { uid ->
                try {
                    registrations.add(
                        backend.listenSharingSettingsByUid(uid) { settings, error ->
                            if (error != null) {
                                // Слушатель после ошибки терминально мёртв — закрываем flow;
                                // перезапуск возможен по повторной подписке (ScreenShown).
                                close(toSharingException(error))
                            } else {
                                mergedSettings[uid] = settings
                                trySend(mergedSettings.toMap())
                            }
                        },
                    )
                } catch (throwable: Throwable) {
                    close(toSharingException(throwable))
                }
            }
            awaitClose { registrations.forEach { registration -> registration.remove() } }
        }

    /**
     * Чанк whereIn-запроса: копит свою часть объединённой карты локаций и уведомляет
     * об обновлении, чтобы наружу уходил полный слепок.
     */
    private class LocationsChunk(
        val uids: List<String>,
        private val onUpdated: () -> Unit,
    ) {
        val locations = HashMap<String, FriendLocation>()

        /** Заменяет свою часть свежим снапшотом и переэмитирует объединённую карту. */
        fun apply(locations: Map<String, FriendLocation>) {
            uids.forEach { uid -> this.locations.remove(uid) }
            this.locations.putAll(locations)
            onUpdated()
        }
    }

    private companion object {
        /** Лимит Firestore на whereIn по documentId. */
        const val MAX_WHERE_IN_UIDS = 10
    }
}
