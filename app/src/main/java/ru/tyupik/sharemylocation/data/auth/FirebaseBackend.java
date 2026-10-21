package ru.tyupik.sharemylocation.data.auth;

import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.tyupik.sharemylocation.business.auth.AuthError;
import ru.tyupik.sharemylocation.business.auth.AuthException;
import ru.tyupik.sharemylocation.business.common.User;
import ru.tyupik.sharemylocation.business.friends.FriendRequest;
import ru.tyupik.sharemylocation.business.friends.FriendRequestStatus;
import ru.tyupik.sharemylocation.business.friends.FriendsError;
import ru.tyupik.sharemylocation.business.friends.FriendsException;
import ru.tyupik.sharemylocation.business.groups.Group;
import ru.tyupik.sharemylocation.business.groups.GroupsError;
import ru.tyupik.sharemylocation.business.groups.GroupsException;
import ru.tyupik.sharemylocation.business.profile.ProfileError;
import ru.tyupik.sharemylocation.business.profile.ProfileException;
import ru.tyupik.sharemylocation.business.sharing.SharingAudience;
import ru.tyupik.sharemylocation.business.sharing.SharingError;
import ru.tyupik.sharemylocation.business.sharing.SharingException;
import ru.tyupik.sharemylocation.business.sharing.SharingSettings;

/**
 * Вся работа с Firebase Auth и Firestore собрана в одном Java-классе.
 * Java выбрана сознательно: пайплайн компиляции AGP 9.4 не может резолвить firebase-артефакты
 * из Kotlin-источников, а javac читает те же jar без проблем.
 * Методы блокирующие (Tasks.await с таймаутом), поэтому вызывать только из Dispatchers.IO —
 * см. AuthRepositoryImpl.
 */
public final class FirebaseBackend {

    private static final String TAG = "FirebaseBackend";
    private static final String USERS_COLLECTION = "users";
    private static final String AVATARS_COLLECTION = "avatars";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_DISPLAY_NAME = "displayName";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final String FIELD_PHONE = "phone";
    private static final String FIELD_AVATAR_VERSION = "avatarVersion";
    private static final String FIELD_AVATAR_BLOB = "jpeg";
    private static final String FIELD_HAS_COMPLETED_ONBOARDING = "hasCompletedOnboarding";
    private static final String FRIEND_REQUESTS_COLLECTION = "friendRequests";
    private static final String FIELD_FRIEND_UIDS = "friendUids";
    private static final String FIELD_FROM_UID = "fromUid";
    private static final String FIELD_TO_UID = "toUid";
    private static final String FIELD_FROM_NAME = "fromName";
    private static final String FIELD_FROM_AVATAR_VERSION = "fromAvatarVersion";
    private static final String FIELD_STATUS = "status";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String REQUEST_ID_SEPARATOR = "_";
    private static final String GROUPS_COLLECTION = "groups";
    private static final String FIELD_OWNER_ID = "ownerId";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_MEMBER_UIDS = "memberUids";
    private static final String SHARING_SETTINGS_COLLECTION = "sharingSettings";
    private static final String FIELD_IS_ENABLED = "isEnabled";
    private static final String FIELD_AUDIENCE = "audience";
    private static final String FIELD_ALLOWED_UIDS = "allowedUids";
    private static final String FIELD_ALLOWED_GROUP_IDS = "allowedGroupIds";
    private static final String AUDIENCE_ALL_FRIENDS = "ALL_FRIENDS";
    private static final String AUDIENCE_CUSTOM = "CUSTOM";

    /**
     * Максимальный размер JPEG аватара: лимит документа Firestore — 1 МБ,
     * запас оставлен под overhead остальных полей и метаданных документа.
     */
    private static final int MAX_AVATAR_BYTES = 900_000;

    /** Таймаут ожидания ответов Firebase, чтобы зависший вызов не держал спиннер вечно. */
    private static final long AWAIT_TIMEOUT_SECONDS = 30;

    public void signInWithEmail(String email, String password) throws AuthException {
        try {
            Log.d(TAG, "signInWithEmail: start");
            var result = await(
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            );
            Log.d(TAG, "signInWithEmail: auth ok, uid=" + result.getUser().getUid());
            ensureUserDocument();
            Log.d(TAG, "signInWithEmail: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toAuthException(e);
        }
    }

    public void signUpWithEmail(String email, String password, String displayName) throws AuthException {
        try {
            Log.d(TAG, "signUpWithEmail: start");
            var result = await(
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            );
            FirebaseUser firebaseUser = result.getUser();
            if (firebaseUser == null) {
                throw new AuthException(
                    new AuthError.Unexpected("createUserWithEmailAndPassword вернул null-пользователя"),
                    null
                );
            }
            Log.d(TAG, "signUpWithEmail: account created, uid=" + firebaseUser.getUid());
            await(
                firebaseUser.updateProfile(
                    new UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                )
            );
            ensureUserDocument();
            Log.d(TAG, "signUpWithEmail: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toAuthException(e);
        }
    }

    public void signInWithGoogle(String idToken) throws AuthException {
        try {
            Log.d(TAG, "signInWithGoogle: start");
            var credential = GoogleAuthProvider.getCredential(idToken, null);
            var result = await(
                FirebaseAuth.getInstance().signInWithCredential(credential)
            );
            Log.d(TAG, "signInWithGoogle: auth ok, uid=" + result.getUser().getUid());
            ensureUserDocument();
            Log.d(TAG, "signInWithGoogle: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toAuthException(e);
        }
    }

    /** Текущий пользователь сохранённой сессии либо null. */
    public User getCurrentUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            return null;
        }
        return new User(
            firebaseUser.getUid(),
            firebaseUser.getEmail(),
            firebaseUser.getDisplayName(),
            null,
            0L,
            false
        );
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    /** Профиль текущего пользователя: данные сессии, дополненные документом users/{uid}. */
    public User getUserProfile() throws ProfileException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw new ProfileException(
                new ProfileError.Unexpected("Нет текущего пользователя Firebase при чтении профиля"),
                null
            );
        }
        Log.d(TAG, "getUserProfile: start");
        try {
            DocumentSnapshot snapshot = await(
                FirebaseFirestore.getInstance()
                    .collection(USERS_COLLECTION)
                    .document(firebaseUser.getUid())
                    .get()
            );
            Log.d(TAG, "getUserProfile: done, exists=" + snapshot.exists());
            Long avatarVersion = snapshot.getLong(FIELD_AVATAR_VERSION);
            return new User(
                firebaseUser.getUid(),
                firebaseUser.getEmail(),
                firstNonEmpty(snapshot.getString(FIELD_DISPLAY_NAME), firebaseUser.getDisplayName()),
                snapshot.getString(FIELD_PHONE),
                avatarVersion != null ? avatarVersion : 0L,
                Boolean.TRUE.equals(snapshot.getBoolean(FIELD_HAS_COMPLETED_ONBOARDING))
            );
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toProfileException(e);
        }
    }

    /**
     * Загружает подготовленный JPEG в документ avatars/{uid} и инкрементирует avatarVersion
     * в users/{uid}; обе записи выполняются в одной Firestore-транзакции,
     * возвращает новую версию аватара.
     */
    public long uploadAvatar(byte[] jpegBytes) throws ProfileException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw new ProfileException(
                new ProfileError.Unexpected("Нет текущего пользователя Firebase при загрузке аватара"),
                null
            );
        }
        if (jpegBytes.length > MAX_AVATAR_BYTES) {
            throw new ProfileException(ProfileError.AvatarTooLarge.INSTANCE, null);
        }
        Log.d(TAG, "uploadAvatar: start");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        var usersDocument = firestore.collection(USERS_COLLECTION).document(firebaseUser.getUid());
        var avatarsDocument = firestore.collection(AVATARS_COLLECTION).document(firebaseUser.getUid());
        try {
            long newVersion = await(firestore.runTransaction(transaction -> {
                DocumentSnapshot usersSnapshot = transaction.get(usersDocument);
                Long storedVersion = usersSnapshot.getLong(FIELD_AVATAR_VERSION);
                long oldVersion = storedVersion != null ? storedVersion : 0L;

                Map<String, Object> avatarData = new HashMap<>();
                avatarData.put(FIELD_AVATAR_BLOB, Blob.fromBytes(jpegBytes));
                avatarData.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
                transaction.set(avatarsDocument, avatarData, SetOptions.merge());

                Map<String, Object> usersData = new HashMap<>();
                usersData.put(FIELD_AVATAR_VERSION, oldVersion + 1);
                usersData.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
                transaction.set(usersDocument, usersData, SetOptions.merge());

                return oldVersion + 1;
            }));
            Log.d(TAG, "uploadAvatar: done, version=" + newVersion);
            return newVersion;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toProfileException(e);
        }
    }

    /** Байты JPEG из документа avatars/{uid}; null — документа или блоба нет. */
    public byte[] getAvatarBytes(String uid) throws ProfileException {
        Log.d(TAG, "getAvatarBytes: start");
        try {
            DocumentSnapshot snapshot = await(
                FirebaseFirestore.getInstance()
                    .collection(AVATARS_COLLECTION)
                    .document(uid)
                    .get()
            );
            Log.d(TAG, "getAvatarBytes: done, exists=" + snapshot.exists());
            Blob blob = snapshot.getBlob(FIELD_AVATAR_BLOB);
            return blob != null ? blob.toBytes() : null;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toProfileException(e);
        }
    }

    /** Сохраняет поля профиля в users/{uid} (merge). */
    public void saveProfile(
        String displayName,
        String phone,
        boolean hasCompletedOnboarding
    ) throws ProfileException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw new ProfileException(
                new ProfileError.Unexpected("Нет текущего пользователя Firebase при сохранении профиля"),
                null
            );
        }
        Log.d(TAG, "saveProfile: start");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put(FIELD_DISPLAY_NAME, displayName != null ? displayName : "");
            data.put(FIELD_PHONE, phone != null ? phone : "");
            data.put(FIELD_HAS_COMPLETED_ONBOARDING, hasCompletedOnboarding);
            data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
            await(
                FirebaseFirestore.getInstance()
                    .collection(USERS_COLLECTION)
                    .document(firebaseUser.getUid())
                    .set(data, SetOptions.merge())
            );
            Log.d(TAG, "saveProfile: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toProfileException(e);
        }
    }

    /** Первый непустой аргумент: значение документа приоритетнее значения из сессии. */
    private static String firstNonEmpty(String primary, String fallback) {
        if (primary != null && !primary.isEmpty()) {
            return primary;
        }
        return fallback;
    }

    // ------------------------------------------------------------------
    // Фича «Друзья»: поиск, заявки, список друзей.
    // ------------------------------------------------------------------

    /**
     * Поиск пользователей по точному совпадению нормализованного телефона.
     * Пустой список — совпадений нет (коллизии телефонов разрешает вызывающий слой).
     */
    public List<User> findUsersByPhone(String phone) throws FriendsException {
        Log.d(TAG, "findUsersByPhone: start");
        try {
            QuerySnapshot snapshot = await(
                FirebaseFirestore.getInstance()
                    .collection(USERS_COLLECTION)
                    .whereEqualTo(FIELD_PHONE, phone)
                    .get()
            );
            List<User> users = new ArrayList<>();
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                users.add(toUser(document));
            }
            Log.d(TAG, "findUsersByPhone: done, size=" + users.size());
            return users;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toFriendsException(e);
        }
    }

    /**
     * Профили пользователей по uid одним запросом whereIn по FieldPath.documentId.
     * Лимит whereIn — 10 элементов на запрос; чанкование выполняет Kotlin-репозиторий.
     */
    public List<User> getUsersByUids(List<String> uids) throws FriendsException {
        if (uids == null || uids.isEmpty()) {
            return new ArrayList<>();
        }
        Log.d(TAG, "getUsersByUids: start, size=" + uids.size());
        try {
            QuerySnapshot snapshot = await(
                FirebaseFirestore.getInstance()
                    .collection(USERS_COLLECTION)
                    .whereIn(FieldPath.documentId(), uids)
                    .get()
            );
            List<User> users = new ArrayList<>();
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                users.add(toUser(document));
            }
            Log.d(TAG, "getUsersByUids: done, size=" + users.size());
            return users;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toFriendsException(e);
        }
    }

    /** Список друзей текущего пользователя из поля friendUids его документа users (пустой, если поля нет). */
    public List<String> getMyFriendUids() throws FriendsException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUser();
        }
        Log.d(TAG, "getMyFriendUids: start");
        try {
            DocumentSnapshot snapshot = await(
                FirebaseFirestore.getInstance()
                    .collection(USERS_COLLECTION)
                    .document(firebaseUser.getUid())
                    .get()
            );
            List<String> uids = toStringList(snapshot.get(FIELD_FRIEND_UIDS));
            Log.d(TAG, "getMyFriendUids: done, size=" + uids.size());
            return uids;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toFriendsException(e);
        }
    }

    /**
     * Заявка friendRequests/{fromUid_toUid} либо null, если документа нет.
     * Используется для проверок «заявка уже отправлена / встречная заявка» по свежим данным.
     */
    public FriendRequest getFriendRequest(String fromUid, String toUid) throws FriendsException {
        Log.d(TAG, "getFriendRequest: start");
        try {
            DocumentSnapshot snapshot = await(
                FirebaseFirestore.getInstance()
                    .collection(FRIEND_REQUESTS_COLLECTION)
                    .document(requestId(fromUid, toUid))
                    .get()
            );
            Log.d(TAG, "getFriendRequest: done, exists=" + snapshot.exists());
            return snapshot.exists() ? toFriendRequest(snapshot) : null;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toFriendsException(e);
        }
    }

    /**
     * Создаёт PENDING-заявку friendRequests/{fromUid_toUid} со снапшотом профиля отправителя
     * (displayName, avatarVersion из своего документа users на момент отправки).
     * Проверки AlreadyFriends/PENDING-барьеров и удаление stale-документов пары
     * делает репозиторий (см. KDoc FriendsRepositoryImpl.sendFriendRequest).
     */
    public void sendFriendRequest(String toUid) throws FriendsException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUser();
        }
        Log.d(TAG, "sendFriendRequest: start");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        try {
            DocumentSnapshot mySnapshot = await(
                firestore.collection(USERS_COLLECTION).document(firebaseUser.getUid()).get()
            );
            if (!mySnapshot.exists()) {
                throw new FriendsException(
                    new FriendsError.Unexpected("Собственный документ users не найден при отправке заявки"),
                    null
                );
            }
            Long avatarVersion = mySnapshot.getLong(FIELD_AVATAR_VERSION);
            String fromName = firstNonEmpty(
                mySnapshot.getString(FIELD_DISPLAY_NAME),
                firebaseUser.getDisplayName()
            );
            Map<String, Object> data = new HashMap<>();
            data.put(FIELD_FROM_UID, firebaseUser.getUid());
            data.put(FIELD_TO_UID, toUid);
            data.put(FIELD_FROM_NAME, fromName != null ? fromName : "");
            data.put(FIELD_FROM_AVATAR_VERSION, avatarVersion != null ? avatarVersion : 0L);
            data.put(FIELD_STATUS, STATUS_PENDING);
            data.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
            data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
            await(
                firestore.collection(FRIEND_REQUESTS_COLLECTION)
                    .document(requestId(firebaseUser.getUid(), toUid))
                    .set(data)
            );
            Log.d(TAG, "sendFriendRequest: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toFriendsException(e);
        }
    }

    /**
     * Принимает заявку от [fromUid]: транзакция ставит статус ACCEPTED и updatedAt в документе
     * заявки и добавляет участников в friendUids ОБОИХ users-документов.
     * Повторное принятие обработанной или удалённой заявки — идемпотентный успех.
     */
    public void acceptFriendRequest(String fromUid) throws FriendsException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUser();
        }
        String myUid = firebaseUser.getUid();
        Log.d(TAG, "acceptFriendRequest: start");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference requestDocument = firestore
            .collection(FRIEND_REQUESTS_COLLECTION)
            .document(requestId(fromUid, myUid));
        try {
            await(firestore.runTransaction(transaction -> {
                DocumentSnapshot requestSnapshot = transaction.get(requestDocument);
                if (!requestSnapshot.exists()
                        || !STATUS_PENDING.equals(requestSnapshot.getString(FIELD_STATUS))) {
                    // Заявку уже обработали или удалили — считаем приём успешным (идемпотентность).
                    Log.d(TAG, "acceptFriendRequest: request already handled");
                    return Boolean.FALSE;
                }
                Map<String, Object> requestData = new HashMap<>();
                requestData.put(FIELD_STATUS, STATUS_ACCEPTED);
                requestData.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
                transaction.set(requestDocument, requestData, SetOptions.merge());

                unionFriendUid(transaction, firestore.collection(USERS_COLLECTION).document(myUid), fromUid);
                unionFriendUid(transaction, firestore.collection(USERS_COLLECTION).document(fromUid), myUid);
                return Boolean.TRUE;
            }));
            Log.d(TAG, "acceptFriendRequest: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toFriendsException(e);
        }
    }

    /** Отклоняет заявку: удаляет документ friendRequests/{fromUid_toUid} (статус DECLINED не персистится). */
    public void declineFriendRequest(String fromUid) throws FriendsException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUser();
        }
        Log.d(TAG, "declineFriendRequest: start");
        try {
            await(
                FirebaseFirestore.getInstance()
                    .collection(FRIEND_REQUESTS_COLLECTION)
                    .document(requestId(fromUid, firebaseUser.getUid()))
                    .delete()
            );
            Log.d(TAG, "declineFriendRequest: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toFriendsException(e);
        }
    }

    /**
     * Блокирующий delete документа friendRequests/{fromUid}_{toUid}. Вызывается только когда
     * препроверка подтвердила существование документа: для несуществующего resource == null,
     * resource-based delete-правило даст deny. Семантику отклонения (declineFriendRequest) не несёт.
     */
    public void deleteFriendRequest(String fromUid, String toUid) throws FriendsException {
        Log.d(TAG, "deleteFriendRequest: start");
        try {
            await(
                FirebaseFirestore.getInstance()
                    .collection(FRIEND_REQUESTS_COLLECTION)
                    .document(requestId(fromUid, toUid))
                    .delete()
            );
            Log.d(TAG, "deleteFriendRequest: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toFriendsException(e);
        }
    }

    /**
     * Удаляет друга: транзакция убирает участников друг у друга из friendUids (arrayRemove)
     * в обоих users-документах и «гигиенически» удаляет stale-документы заявки пары
     * (документ заявки отражает только последнюю заявку — см. KDoc FriendsRepositoryImpl).
     * Delete строго guarded (exists-проверка): для несуществующего документа resource == null
     * и resource-based delete-правило deny'ит всю транзакцию; чтения выполняются до записей.
     */
    public void removeFriend(String friendUid) throws FriendsException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUser();
        }
        String myUid = firebaseUser.getUid();
        Log.d(TAG, "removeFriend: start");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DocumentReference outgoingDocument = firestore
            .collection(FRIEND_REQUESTS_COLLECTION)
            .document(requestId(myUid, friendUid));
        DocumentReference incomingDocument = firestore
            .collection(FRIEND_REQUESTS_COLLECTION)
            .document(requestId(friendUid, myUid));
        try {
            await(firestore.runTransaction(transaction -> {
                boolean outgoingPresent = transaction.get(outgoingDocument).exists();
                boolean incomingPresent = transaction.get(incomingDocument).exists();
                if (outgoingPresent) {
                    transaction.delete(outgoingDocument);
                }
                if (incomingPresent) {
                    transaction.delete(incomingDocument);
                }
                removeFriendUid(transaction, firestore.collection(USERS_COLLECTION).document(myUid), friendUid);
                removeFriendUid(transaction, firestore.collection(USERS_COLLECTION).document(friendUid), myUid);
                return Boolean.TRUE;
            }));
            Log.d(TAG, "removeFriend: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toFriendsException(e);
        }
    }

    /**
     * Realtime-слушатель входящих PENDING-заявок текущего пользователя.
     * Без orderBy: сортировка по createdAt выполняется на клиенте, поэтому
     * композитные индексы для этого запроса не нужны.
     */
    public ListenerRegistration listenIncomingFriendRequests(FriendRequestsListener listener)
        throws FriendsException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUser();
        }
        Log.d(TAG, "listenIncomingFriendRequests: start");
        return FirebaseFirestore.getInstance()
            .collection(FRIEND_REQUESTS_COLLECTION)
            .whereEqualTo(FIELD_TO_UID, firebaseUser.getUid())
            .whereEqualTo(FIELD_STATUS, STATUS_PENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.w(TAG, "listenIncomingFriendRequests: error", error);
                    listener.onUpdated(null, error);
                    return;
                }
                List<FriendRequest> requests = new ArrayList<>();
                if (snapshots != null) {
                    for (DocumentSnapshot document : snapshots.getDocuments()) {
                        FriendRequest request = toFriendRequest(document);
                        if (request != null) {
                            requests.add(request);
                        }
                    }
                }
                listener.onUpdated(requests, null);
            });
    }

    /** Realtime-слушатель поля friendUids своего документа users (отсутствие поля — пустой список). */
    public ListenerRegistration listenMyFriendUids(FriendUidsListener listener) throws FriendsException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUser();
        }
        Log.d(TAG, "listenMyFriendUids: start");
        return FirebaseFirestore.getInstance()
            .collection(USERS_COLLECTION)
            .document(firebaseUser.getUid())
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    Log.w(TAG, "listenMyFriendUids: error", error);
                    listener.onUpdated(null, error);
                    return;
                }
                List<String> uids = snapshot != null && snapshot.exists()
                    ? toStringList(snapshot.get(FIELD_FRIEND_UIDS))
                    : new ArrayList<>();
                listener.onUpdated(uids, null);
            });
    }

    /** Колбэк realtime-слушателя входящих заявок; error != null означает обрыв потока данных. */
    public interface FriendRequestsListener {
        void onUpdated(List<FriendRequest> requests, Exception error);
    }

    /** Колбэк realtime-слушателя списка друзей (поле friendUids своего документа users). */
    public interface FriendUidsListener {
        void onUpdated(List<String> uids, Exception error);
    }

    /** Добавляет друга в friendUids users-документа внутри транзакции (merge, arrayUnion). */
    private void unionFriendUid(Transaction transaction, DocumentReference userDocument, String friendUid) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_FRIEND_UIDS, FieldValue.arrayUnion(friendUid));
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
        transaction.set(userDocument, data, SetOptions.merge());
    }

    /** Убирает друга из friendUids users-документа внутри транзакции (merge, arrayRemove). */
    private void removeFriendUid(Transaction transaction, DocumentReference userDocument, String friendUid) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_FRIEND_UIDS, FieldValue.arrayRemove(friendUid));
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
        transaction.set(userDocument, data, SetOptions.merge());
    }

    /** Id документа заявки: "fromUid_toUid". */
    private static String requestId(String fromUid, String toUid) {
        return fromUid + REQUEST_ID_SEPARATOR + toUid;
    }

    /** DocumentSnapshot → User по полям документа users. */
    private User toUser(DocumentSnapshot snapshot) {
        Long avatarVersion = snapshot.getLong(FIELD_AVATAR_VERSION);
        return new User(
            snapshot.getId(),
            snapshot.getString(FIELD_EMAIL),
            snapshot.getString(FIELD_DISPLAY_NAME),
            snapshot.getString(FIELD_PHONE),
            avatarVersion != null ? avatarVersion : 0L,
            Boolean.TRUE.equals(snapshot.getBoolean(FIELD_HAS_COMPLETED_ONBOARDING))
        );
    }

    /** DocumentSnapshot → FriendRequest; null, если в документе нет fromUid или toUid. */
    private FriendRequest toFriendRequest(DocumentSnapshot snapshot) {
        String fromUid = snapshot.getString(FIELD_FROM_UID);
        String toUid = snapshot.getString(FIELD_TO_UID);
        if (fromUid == null || toUid == null) {
            Log.w(TAG, "toFriendRequest: в документе " + snapshot.getId() + " нет fromUid/toUid");
            return null;
        }
        Long avatarVersion = snapshot.getLong(FIELD_FROM_AVATAR_VERSION);
        Timestamp createdAt = snapshot.getTimestamp(FIELD_CREATED_AT);
        return new FriendRequest(
            fromUid,
            toUid,
            firstNonEmpty(snapshot.getString(FIELD_FROM_NAME), ""),
            avatarVersion != null ? avatarVersion : 0L,
            STATUS_ACCEPTED.equals(snapshot.getString(FIELD_STATUS))
                ? FriendRequestStatus.ACCEPTED
                : FriendRequestStatus.PENDING,
            createdAt != null
                ? createdAt.getSeconds() * 1000L + createdAt.getNanoseconds() / 1_000_000L
                : 0L
        );
    }

    /** Приводит значение поля friendUids к списку строк; не-строковые элементы пропускаются. */
    private static List<String> toStringList(Object raw) {
        List<String> result = new ArrayList<>();
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (item instanceof String) {
                    result.add((String) item);
                }
            }
        }
        return result;
    }

    private FriendsException noCurrentUser() {
        return new FriendsException(
            new FriendsError.Unexpected("Нет текущего пользователя Firebase в операциях друзей"),
            null
        );
    }

    private FriendsException toFriendsException(Throwable throwable) {
        Throwable cause = unwrapCause(throwable);
        if (cause instanceof FriendsException) {
            return (FriendsException) cause;
        }
        if (isNetworkError(cause)) {
            return new FriendsException(FriendsError.NetworkUnavailable.INSTANCE, cause);
        }
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new FriendsException(new FriendsError.Unexpected(describeCause(cause)), cause);
    }

    /** Разворачивает ExecutionException до исходной причины. */
    private static Throwable unwrapCause(Throwable throwable) {
        if (throwable instanceof ExecutionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    /**
     * Классификатор «сетевая ли ошибка»: NetworkUnavailable — только недоступность транспорта
     * (Firestore UNAVAILABLE/DEADLINE_EXCEEDED, FirebaseNetworkException, IOException, таймаут).
     * Остальные коды Firestore (PERMISSION_DENIED, FAILED_PRECONDITION, NOT_FOUND, INTERNAL
     * и т.п.) — не сеть: попадут в Unexpected с кодом в reason (см. describeCause).
     */
    private static boolean isNetworkError(Throwable cause) {
        if (cause instanceof FirebaseNetworkException
                || cause instanceof IOException
                || cause instanceof TimeoutException) {
            return true;
        }
        if (cause instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) cause).getCode();
            return code == FirebaseFirestoreException.Code.UNAVAILABLE
                || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED;
        }
        return false;
    }

    /** «КОД: сообщение» для reason у Firestore-ошибок (чтобы код не терялся), иначе message. */
    private static String describeCause(Throwable cause) {
        if (cause instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) cause;
            return firestoreException.getCode() + ": " + cause.getMessage();
        }
        return cause.getMessage();
    }

    // ------------------------------------------------------------------
    // Фича «Группы»: личные «папки» владельца (ownerId); участники групп о них не знают.
    // ------------------------------------------------------------------

    /**
     * Группа по id: одноразовый get. PERMISSION_DENIED и NOT_FOUND мапятся в GroupNotFound
     * ДО общей классификации — только здесь, не в toGroupsException: правило groups читает
     * resource.data, поэтому get несуществующего/чужого документа даёт PERMISSION_DENIED
     * (NOT_FOUND возможен из локального кэша отсутствия) — для клиента это одно состояние;
     * listen/create/update/delete сохраняют общую классификацию (Unexpected с кодом).
     * Ветка !exists() — защита на случай снимка без deny (например, после смягчения правил).
     */
    public Group getGroup(String groupId) throws GroupsException {
        Log.d(TAG, "getGroup: start");
        try {
            DocumentSnapshot snapshot = await(
                FirebaseFirestore.getInstance()
                    .collection(GROUPS_COLLECTION)
                    .document(groupId)
                    .get()
            );
            Log.d(TAG, "getGroup: done, exists=" + snapshot.exists());
            if (!snapshot.exists()) {
                throw new GroupsException(GroupsError.GroupNotFound.INSTANCE, null);
            }
            return toGroup(snapshot);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Throwable cause = unwrapCause(e);
            if (isGroupAccessDenied(cause)) {
                throw new GroupsException(GroupsError.GroupNotFound.INSTANCE, cause);
            }
            throw toGroupsException(e);
        }
    }

    /**
     * Создаёт groups/{autoId} с ownerId текущего пользователя, именем и составом memberUids
     * (валидацию состава выполняют create-правила: memberUids ⊆ friendUids владельца, ≤ 100);
     * createdAt/updatedAt — serverTimestamp. Авто-id генерируется офлайн, поэтому groupId
     * известен сразу после успешного set.
     */
    public String createGroup(String name, List<String> memberUids) throws GroupsException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUserForGroups();
        }
        Log.d(TAG, "createGroup: start");
        DocumentReference document = FirebaseFirestore.getInstance()
            .collection(GROUPS_COLLECTION)
            .document();
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_OWNER_ID, firebaseUser.getUid());
        data.put(FIELD_NAME, name != null ? name : "");
        data.put(FIELD_MEMBER_UIDS, memberUids != null ? memberUids : new ArrayList<String>());
        data.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
        data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
        try {
            await(document.set(data));
            Log.d(TAG, "createGroup: done, id=" + document.getId());
            return document.getId();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toGroupsException(e);
        }
    }

    /**
     * Обновляет группу merge'ем только {name, memberUids, updatedAt}: ownerId и createdAt
     * не трогаются, поэтому правило update (affectedKeys().hasOnly) пропускает запись.
     */
    public void updateGroup(String groupId, String name, List<String> memberUids) throws GroupsException {
        Log.d(TAG, "updateGroup: start");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put(FIELD_NAME, name != null ? name : "");
            data.put(FIELD_MEMBER_UIDS, memberUids != null ? memberUids : new ArrayList<String>());
            data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
            await(
                FirebaseFirestore.getInstance()
                    .collection(GROUPS_COLLECTION)
                    .document(groupId)
                    .set(data, SetOptions.merge())
            );
            Log.d(TAG, "updateGroup: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toGroupsException(e);
        }
    }

    /**
     * Блокирующий delete документа groups/{groupId}. Id приходит только из realtime-списка
     * (документ существует): для несуществующего resource == null, resource-based
     * delete-правило даст deny — как declineFriendRequest.
     */
    public void deleteGroup(String groupId) throws GroupsException {
        Log.d(TAG, "deleteGroup: start");
        try {
            await(
                FirebaseFirestore.getInstance()
                    .collection(GROUPS_COLLECTION)
                    .document(groupId)
                    .delete()
            );
            Log.d(TAG, "deleteGroup: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toGroupsException(e);
        }
    }

    /**
     * Realtime-слушатель групп текущего пользователя. Один equality-фильтр по ownerId,
     * без orderBy: сортировка по имени выполняется на клиенте, композитные индексы не нужны.
     */
    public ListenerRegistration listenMyGroups(GroupsListener listener) throws GroupsException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUserForGroups();
        }
        Log.d(TAG, "listenMyGroups: start");
        return FirebaseFirestore.getInstance()
            .collection(GROUPS_COLLECTION)
            .whereEqualTo(FIELD_OWNER_ID, firebaseUser.getUid())
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.w(TAG, "listenMyGroups: error", error);
                    listener.onUpdated(null, error);
                    return;
                }
                List<Group> groups = new ArrayList<>();
                if (snapshots != null) {
                    for (DocumentSnapshot document : snapshots.getDocuments()) {
                        groups.add(toGroup(document));
                    }
                }
                listener.onUpdated(groups, null);
            });
    }

    /** Колбэк realtime-слушателя групп; error != null означает обрыв потока данных. */
    public interface GroupsListener {
        void onUpdated(List<Group> groups, Exception error);
    }

    /** DocumentSnapshot → Group: Timestamp в millis, memberUids через toStringList. */
    private Group toGroup(DocumentSnapshot snapshot) {
        Timestamp createdAt = snapshot.getTimestamp(FIELD_CREATED_AT);
        return new Group(
            snapshot.getId(),
            firstNonEmpty(snapshot.getString(FIELD_NAME), ""),
            toStringList(snapshot.get(FIELD_MEMBER_UIDS)),
            createdAt != null
                ? createdAt.getSeconds() * 1000L + createdAt.getNanoseconds() / 1_000_000L
                : 0L
        );
    }

    private GroupsException noCurrentUserForGroups() {
        return new GroupsException(
            new GroupsError.Unexpected("Нет текущего пользователя Firebase в операциях групп"),
            null
        );
    }

    /** GroupsException проходит как есть, сеть — NetworkUnavailable, остальное — Unexpected с кодом. */
    private GroupsException toGroupsException(Throwable throwable) {
        Throwable cause = unwrapCause(throwable);
        if (cause instanceof GroupsException) {
            return (GroupsException) cause;
        }
        if (isNetworkError(cause)) {
            return new GroupsException(GroupsError.NetworkUnavailable.INSTANCE, cause);
        }
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new GroupsException(new GroupsError.Unexpected(describeCause(cause)), cause);
    }

    /**
     * Только для getGroup: resource-based read-правило groups даёт PERMISSION_DENIED
     * для несуществующего/чужого документа, NOT_FOUND — для отсутствия по кэшу;
     * оба кода означают «группы нет для этого пользователя».
     */
    private static boolean isGroupAccessDenied(Throwable cause) {
        if (!(cause instanceof FirebaseFirestoreException)) {
            return false;
        }
        FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) cause).getCode();
        return code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            || code == FirebaseFirestoreException.Code.NOT_FOUND;
    }

    // ------------------------------------------------------------------
    // Фича «Шаринг геолокации»: настройки sharingSettings/{uid} (Фаза 5).
    // ------------------------------------------------------------------

    /**
     * Настройки шаринга текущего пользователя: одноразовый get sharingSettings/{uid} (await, 30 c).
     * Отсутствующий документ — НЕ ошибка: возвращается дефолт (SharingSettings.defaultOf);
     * правило владельческое и resource не читает, поэтому PERMISSION_DENIED на отсутствующем
     * документе невозможен и отдельный маппинг кодов, как в getGroup, не нужен.
     */
    public SharingSettings getSharingSettings() throws SharingException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUserForSharing();
        }
        Log.d(TAG, "getSharingSettings: start");
        try {
            DocumentSnapshot snapshot = await(
                FirebaseFirestore.getInstance()
                    .collection(SHARING_SETTINGS_COLLECTION)
                    .document(firebaseUser.getUid())
                    .get()
            );
            Log.d(TAG, "getSharingSettings: done, exists=" + snapshot.exists());
            if (!snapshot.exists()) {
                return SharingSettings.defaultOf(firebaseUser.getUid());
            }
            Timestamp updatedAt = snapshot.getTimestamp(FIELD_UPDATED_AT);
            return new SharingSettings(
                firebaseUser.getUid(),
                Boolean.TRUE.equals(snapshot.getBoolean(FIELD_IS_ENABLED)),
                toSharingAudience(snapshot.getString(FIELD_AUDIENCE)),
                toStringList(snapshot.get(FIELD_ALLOWED_UIDS)),
                toStringList(snapshot.get(FIELD_ALLOWED_GROUP_IDS)),
                updatedAt != null
                    ? updatedAt.getSeconds() * 1000L + updatedAt.getNanoseconds() / 1_000_000L
                    : 0L
            );
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toSharingException(e);
        }
    }

    /**
     * Записывает полный снимок настроек шаринга: merge всех пяти полей
     * {isEnabled, audience, allowedUids, allowedGroupIds, updatedAt = serverTimestamp}.
     * Merge безопасен: правило владельческое и не использует resource/affectedKeys().
     * audience — строка имени enum; состав списков валидирует клиент (ViewModel).
     */
    public void saveSharingSettings(
        boolean isEnabled,
        String audience,
        List<String> allowedUids,
        List<String> allowedGroupIds
    ) throws SharingException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw noCurrentUserForSharing();
        }
        Log.d(TAG, "saveSharingSettings: start");
        try {
            Map<String, Object> data = new HashMap<>();
            data.put(FIELD_IS_ENABLED, isEnabled);
            data.put(FIELD_AUDIENCE, audience != null ? audience : AUDIENCE_ALL_FRIENDS);
            data.put(FIELD_ALLOWED_UIDS, allowedUids != null ? allowedUids : new ArrayList<String>());
            data.put(FIELD_ALLOWED_GROUP_IDS, allowedGroupIds != null
                ? allowedGroupIds
                : new ArrayList<String>());
            data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
            await(
                FirebaseFirestore.getInstance()
                    .collection(SHARING_SETTINGS_COLLECTION)
                    .document(firebaseUser.getUid())
                    .set(data, SetOptions.merge())
            );
            Log.d(TAG, "saveSharingSettings: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toSharingException(e);
        }
    }

    /** Строка документа → SharingAudience; неизвестное значение трактуется как ALL_FRIENDS. */
    private static SharingAudience toSharingAudience(String raw) {
        if (AUDIENCE_CUSTOM.equals(raw)) {
            return SharingAudience.CUSTOM;
        }
        if (raw != null && !AUDIENCE_ALL_FRIENDS.equals(raw)) {
            Log.w(TAG, "toSharingAudience: неизвестная аудитория " + raw + " — трактую как ALL_FRIENDS");
        }
        return SharingAudience.ALL_FRIENDS;
    }

    private SharingException noCurrentUserForSharing() {
        return new SharingException(
            new SharingError.Unexpected("Нет текущего пользователя Firebase в операциях шаринга"),
            null
        );
    }

    /** SharingException проходит как есть, сеть — NetworkUnavailable, остальное — Unexpected с кодом. */
    private SharingException toSharingException(Throwable throwable) {
        Throwable cause = unwrapCause(throwable);
        if (cause instanceof SharingException) {
            return (SharingException) cause;
        }
        if (isNetworkError(cause)) {
            return new SharingException(SharingError.NetworkUnavailable.INSTANCE, cause);
        }
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new SharingException(new SharingError.Unexpected(describeCause(cause)), cause);
    }

    private ProfileException toProfileException(Throwable throwable) {
        Throwable cause = unwrapCause(throwable);
        if (cause instanceof ProfileException) {
            return (ProfileException) cause;
        }
        if (isNetworkError(cause)) {
            return new ProfileException(ProfileError.NetworkUnavailable.INSTANCE, cause);
        }
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new ProfileException(new ProfileError.Unexpected(describeCause(cause)), cause);
    }

    /**
     * Создаёт users/{uid} с createdAt и hasCompletedOnboarding = false, если документа ещё нет,
     * иначе обновляет поля (merge).
     */
    private void ensureUserDocument() throws AuthException {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            throw new AuthException(
                new AuthError.Unexpected("Нет текущего пользователя Firebase после успешной аутентификации"),
                null
            );
        }
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        var document = firestore.collection(USERS_COLLECTION).document(firebaseUser.getUid());
        try {
            Log.d(TAG, "ensureUserDocument: start");
            DocumentSnapshot snapshot = await(document.get());
            Map<String, Object> data = new HashMap<>();
            data.put(FIELD_EMAIL, firebaseUser.getEmail());
            data.put(FIELD_DISPLAY_NAME, firebaseUser.getDisplayName() != null
                ? firebaseUser.getDisplayName()
                : "");
            data.put(FIELD_UPDATED_AT, FieldValue.serverTimestamp());
            if (!snapshot.exists()) {
                data.put(FIELD_CREATED_AT, FieldValue.serverTimestamp());
                data.put(FIELD_HAS_COMPLETED_ONBOARDING, false);
            }
            await(document.set(data, SetOptions.merge()));
            Log.d(TAG, "ensureUserDocument: done");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw toAuthException(e);
        }
    }

    private static <T> T await(com.google.android.gms.tasks.Task<T> task)
        throws ExecutionException, InterruptedException, TimeoutException {
        return Tasks.await(task, AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private AuthException toAuthException(Throwable throwable) {
        Throwable cause = unwrapCause(throwable);
        if (cause instanceof AuthException) {
            return (AuthException) cause;
        }
        if (cause instanceof FirebaseAuthInvalidUserException
                || cause instanceof FirebaseAuthInvalidCredentialsException) {
            return new AuthException(AuthError.InvalidCredentials.INSTANCE, cause);
        }
        if (cause instanceof FirebaseAuthUserCollisionException) {
            return new AuthException(AuthError.EmailAlreadyInUse.INSTANCE, cause);
        }
        // Классификацию сети/кодов Firestore ставим ПОСЛЕ специфичных Firebase Auth-веток,
        // чтобы не изменить их приоритет.
        if (isNetworkError(cause)) {
            return new AuthException(AuthError.NetworkUnavailable.INSTANCE, cause);
        }
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new AuthException(new AuthError.Unexpected(describeCause(cause)), cause);
    }
}
