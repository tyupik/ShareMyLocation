package ru.tyupik.sharemylocation.business.map

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import ru.tyupik.sharemylocation.business.auth.AuthInteractor
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.friends.FriendsInteractor
import ru.tyupik.sharemylocation.business.groups.Group
import ru.tyupik.sharemylocation.business.groups.GroupsError
import ru.tyupik.sharemylocation.business.groups.GroupsException
import ru.tyupik.sharemylocation.business.groups.GroupsInteractor
import ru.tyupik.sharemylocation.business.sharing.SharingAudience
import ru.tyupik.sharemylocation.business.sharing.SharingInteractor
import ru.tyupik.sharemylocation.business.sharing.SharingSettings

/**
 * Реализация интерактора карты: комбинирует realtime-список друзей (uid + профиль),
 * одноразовые чтения настроек шаринга и групп (свежесть — каждый ScreenShown)
 * и realtime-точки локаций.
 *
 * Имена/аватар-версии и uid берутся из одного потока [FriendsInteractor.observeFriends]
 * (интерактор не отдаёт сырые uid, а дублирующий запрос профилей не нужен).
 * Настройки/группы читаются заново при каждой подписке на flow{}-блок, тики проходят
 * сквозь combine сами — переэмитация каждые 5 секунд старит «как давно» на экране.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class MapInteractorImpl @Inject constructor(
    private val mapRepository: MapRepository,
    private val friendsInteractor: FriendsInteractor,
    private val sharingInteractor: SharingInteractor,
    private val groupsInteractor: GroupsInteractor,
    private val authInteractor: AuthInteractor,
) : MapInteractor {

    override fun observeVisibleFriends(): Flow<List<VisibleFriend>> =
        friendsInteractor.observeFriends()
            .distinctUntilChanged()
            .flatMapLatest { friends ->
                flow {
                    val myUid = authInteractor.getCurrentUser()?.uid
                    val visibleUids = friends
                        .filter { friend -> isVisible(friend, myUid) }
                        .map(User::uid)
                    emitAll(
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
                        },
                    )
                }
            }

    /**
     * Видит ли текущий пользователь локацию [friend]: настройка активна и аудитория пускает.
     * Сетевая ошибка чтения настроек не глотается — падение закрывает поток целиком
     * (экран покажет состояние ошибки), тогда как deny/отсутствие документа уже
     * преобразованы в default («не шарит») в data-слое.
     */
    private suspend fun isVisible(friend: User, myUid: String?): Boolean {
        if (myUid == null) return false
        val settings = sharingInteractor.getSettingsByUid(friend.uid).getOrThrow()
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
