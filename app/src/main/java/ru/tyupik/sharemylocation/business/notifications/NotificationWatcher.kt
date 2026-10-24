package ru.tyupik.sharemylocation.business.notifications

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import ru.tyupik.sharemylocation.business.auth.AuthInteractor
import ru.tyupik.sharemylocation.business.common.User
import ru.tyupik.sharemylocation.business.friends.FriendRequest
import ru.tyupik.sharemylocation.business.friends.FriendsInteractor
import ru.tyupik.sharemylocation.business.map.MapInteractor
import ru.tyupik.sharemylocation.business.map.VisibleFriend

/**
 * Application-scoped наблюдатель уведомлений: живёт весь срок процесса и публикует локальные
 * уведомления о двух событиях — входящая заявка в друзья и «друг начал делиться геопозицией»
 * (uid появился в наборе видимых друзей). Стартует один раз из [ru.tyupik.sharemylocation.ShareMyLocationApp].
 *
 * Подписки пересобираются от realtime-потока аутентификации: пользователь не задан — дедуп-наборы
 * сброшены и слушателей нет; смена пользователя (logout/login) сбрасывает наборы — событие
 * считается новым только для текущего аккаунта. Ошибка любого Firestore-слушателя закрывает
 * внутренний поток — [retryWhen] пережидает [RETRY_DELAY_MS] и переподписывается, дедуп-наборы
 * при этом сохраняются (сброс — только по auth-переходу); первый снапшот каждой (пере)подписки
 * — baseline: наполняет набор, но не уведомляет.
 *
 * Системные права и публикация — вне ответственности watcher'а: только [NotificationPublisher]
 * (data-слой), поэтому без права на SDK ≥ 33 события просто не доходят до системной шторы.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NotificationWatcher @Inject constructor(
    private val authInteractor: AuthInteractor,
    private val friendsInteractor: FriendsInteractor,
    private val mapInteractor: MapInteractor,
    private val publisher: NotificationPublisher,
) {

    /**
     * Собственный scope процесса (не GlobalScope): watcher не привязан ни к одному экрану,
     * а app-level scope в DI проекта отсутствует — новый не вводим (компромисс плана).
     */
    private val watcherScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Uid отправителей из предыдущего снапшота входящих заявок (дедуп «по предыдущему набору»). */
    private val previousRequestUids = mutableSetOf<String>()

    /** Uid друзей из предыдущего снапшота видимых (дедуп «по предыдущему набору»). */
    private val previousVisibleUids = mutableSetOf<String>()

    /** Uid, для которого наполнены наборы; смена (logout/login) сбрасывает их. */
    private var dedupUid: String? = null

    @Volatile
    private var started = false

    /** Идемпотентный старт наблюдения: повторные вызовы — no-op. */
    fun start() {
        if (started) {
            return
        }
        started = true
        watcherScope.launch {
            observeAuthAndData()
        }
    }

    private suspend fun observeAuthAndData() {
        authInteractor.observeAuthState()
            .flatMapLatest { user ->
                resetDedupFor(user)
                if (user == null) {
                    emptyFlow()
                } else {
                    observeDataSources()
                }
            }
            .retryWhen { cause, _ ->
                Log.w(TAG, "Поток уведомлений упал — переподписка через $RETRY_DELAY_MS мс", cause)
                delay(RETRY_DELAY_MS)
                true
            }
            // Вся логика — в onEach выше; терминальный collect просто держит подписку живой.
            .collect { }
    }

    /** Сброс дедуп-наборов при смене пользователя; переподписка того же uid наборы сохраняет. */
    private fun resetDedupFor(user: User?) {
        if (user?.uid == dedupUid) {
            return
        }
        Log.d(TAG, "resetDedupFor: uid=${user?.uid}")
        previousRequestUids.clear()
        previousVisibleUids.clear()
        dedupUid = user?.uid
    }

    /**
     * Realtime-потоки заявок и видимых друзей, сведённые в один поток-насос.
     * Baseline-флаги локальны здесь: каждая (пере)подписка — в том числе после retry —
     * начинает с baseline-снапшота, не уведомляющего о текущем состоянии.
     */
    private fun observeDataSources(): Flow<Unit> {
        var isRequestsBaseline = true
        var isSharingBaseline = true
        val requestsFlow = friendsInteractor.observeIncomingFriendRequests()
            .onEach { requests ->
                handleRequestsSnapshot(requests, isBaseline = isRequestsBaseline)
                isRequestsBaseline = false
            }
            .map { }
        val sharingFlow = mapInteractor.observeVisibleFriends()
            .onEach { friends ->
                handleSharingSnapshot(friends, isBaseline = isSharingBaseline)
                isSharingBaseline = false
            }
            .map { }
        return merge(requestsFlow, sharingFlow)
    }

    /** Новые входящие заявки: uid, которых не было в предыдущем снапшоте. */
    private fun handleRequestsSnapshot(requests: List<FriendRequest>, isBaseline: Boolean) {
        val uids = requests.map(FriendRequest::fromUid).toSet()
        if (isBaseline) {
            replaceDedupSet(previousRequestUids, uids)
            return
        }
        val requestsByUid = requests.associateBy(FriendRequest::fromUid)
        for (uid in uids - previousRequestUids) {
            val request = requestsByUid[uid] ?: continue
            publisher.publish(
                AppNotification.FriendRequestReceived(fromUid = uid, fromName = request.fromName),
            )
        }
        replaceDedupSet(previousRequestUids, uids)
    }

    /** Друзья, начавшие шарить: uid, которых не было в предыдущем снапшоте видимых. */
    private fun handleSharingSnapshot(friends: List<VisibleFriend>, isBaseline: Boolean) {
        val uids = friends.map(VisibleFriend::uid).toSet()
        if (isBaseline) {
            replaceDedupSet(previousVisibleUids, uids)
            return
        }
        val friendsByUid = friends.associateBy(VisibleFriend::uid)
        for (uid in uids - previousVisibleUids) {
            val friend = friendsByUid[uid] ?: continue
            publisher.publish(
                AppNotification.FriendStartedSharing(uid = uid, displayName = friend.displayName),
            )
        }
        replaceDedupSet(previousVisibleUids, uids)
    }

    /** Предыдущий набор заменяется целиком: исчезнувший uid при повторном появлении — снова событие. */
    private fun replaceDedupSet(target: MutableSet<String>, uids: Set<String>) {
        target.clear()
        target.addAll(uids)
    }

    private companion object {
        const val TAG = "NotificationWatcher"

        /** Пауза перед переподпиской упавших Firestore-слушателей. */
        const val RETRY_DELAY_MS = 30_000L
    }
}
