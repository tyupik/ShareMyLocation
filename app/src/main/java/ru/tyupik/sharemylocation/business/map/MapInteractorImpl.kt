package ru.tyupik.sharemylocation.business.map

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ru.tyupik.sharemylocation.business.auth.AuthInteractor
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.friends.FriendsInteractor
import ru.tyupik.sharemylocation.business.groups.Group
import ru.tyupik.sharemylocation.business.groups.GroupsError
import ru.tyupik.sharemylocation.business.groups.GroupsException
import ru.tyupik.sharemylocation.business.groups.GroupsInteractor
import ru.tyupik.sharemylocation.business.sharing.SharingAudience
import ru.tyupik.sharemylocation.business.sharing.SharingSettings

/**
 * Реализация интерактора карты: realtime-конвейер «друзья → их настройки шаринга →
 * набор видимых uid → их точки локаций».
 *
 * Имена/аватар-версии и uid берутся из одного потока [FriendsInteractor.observeFriends]
 * (интерактор не отдаёт сырые uid, а дублирующий запрос профилей не нужен).
 * Настройки шаринга друзей — realtime-слепок observeSharingSettingsByUids: выключение/
 * включение шаринга и смена аудитории/allowedUids у друга меняют набор видимых без
 * перезахода на вкладку. [distinctUntilChanged] по проекции Map<uid, Boolean> гасит
 * промежуточные эмиссии слепка: locations-слушатели перезапускаются только при фактической
 * смене набора видимых. Состав групп — одноразовый get при пересборке проекции (обновится
 * на следующий ScreenShown). Тики проходят сквозь combine сами — переэмитация каждые
 * 5 секунд старит «как давно» на экране, не перезапуская слушатели.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class MapInteractorImpl @Inject constructor(
    private val mapRepository: MapRepository,
    private val friendsInteractor: FriendsInteractor,
    private val groupsInteractor: GroupsInteractor,
    private val authInteractor: AuthInteractor,
) : MapInteractor {

    override fun observeVisibleFriends(): Flow<List<VisibleFriend>> =
        friendsInteractor.observeFriends()
            .distinctUntilChanged()
            .flatMapLatest { friends ->
                mapRepository.observeSharingSettingsByUids(friends.map(User::uid))
                    .map { settingsByUid ->
                        val myUid = authInteractor.getCurrentUser()?.uid
                        friends
                            .filter { friend -> isVisible(friend, myUid, settingsByUid[friend.uid]) }
                            .map(User::uid)
                    }
                    .distinctUntilChanged()
                    .flatMapLatest { visibleUids ->
                        combine(
                            mapRepository.observeLocationsByUids(visibleUids),
                            visibilityTicker(),
                        ) { locations, _ ->
                            friends.mapNotNull { friend ->
                                locations[friend.uid]?.let { location ->
                                    VisibleFriend(
                                        uid = friend.uid,
                                        displayName = friend.displayName.orEmpty(),
                                        avatarVersion = friend.avatarVersion,
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        accuracyMeters = location.accuracyMeters,
                                        updatedAtMillis = location.updatedAtMillis,
                                    )
                                }
                            }
                        }
                    }
            }

    /**
     * Видит ли текущий пользователь локацию [friend]: настройка из realtime-слепка активна
     * и аудитория пускает. Отсутствующий слепок (слушатель ещё не доставил документ) —
     * «не видно» до первой доставки. Сетевые ошибки слушателя не глотаются — падение
     * закрывает поток целиком (экран покажет состояние ошибки); deny/отсутствие документа
     * уже преобразованы в default («не шарит») в data-слое.
     */
    private suspend fun isVisible(
        friend: User,
        myUid: String?,
        settings: SharingSettings?,
    ): Boolean {
        if (myUid == null || settings == null) return false
        if (!settings.isEnabled) return false
        return when (settings.audience) {
            SharingAudience.ALL_FRIENDS -> true
            SharingAudience.CUSTOM ->
                myUid in settings.allowedUids || hasGroupMembership(settings.allowedGroupIds, myUid)
        }
    }

    /** Состоит ли [myUid] в memberUids хотя бы одной группы из [groupIds]. */
    private suspend fun hasGroupMembership(groupIds: List<String>, myUid: String): Boolean {
        for (groupId in groupIds) {
            val group = resolveGroup(groupId) ?: continue
            if (myUid in group.memberUids) return true
        }
        return false
    }

    /**
     * Группа по id; протухший allowedGroupIds (удалена/недоступна — GroupNotFound) — null,
     * группа не учитывается. Остальные ошибки не глотаются: закрывают поток.
     */
    private suspend fun resolveGroup(groupId: String): Group? =
        groupsInteractor.getGroup(groupId).getOrElse { throwable ->
            if ((throwable as? GroupsException)?.error == GroupsError.GroupNotFound) {
                null
            } else {
                throw throwable
            }
        }

    /** Тик каждые [TICK_INTERVAL_MS]: переэмиссия списка, чтобы «как давно» старело. */
    private fun visibilityTicker(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_INTERVAL_MS)
        }
    }

    private companion object {
        /** Период переэмиссии видимых друзей для старения метки «как давно». */
        const val TICK_INTERVAL_MS = 5_000L
    }
}
