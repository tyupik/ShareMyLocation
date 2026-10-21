package ru.tyupik.sharemylocation.data.auth;

import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.tyupik.sharemylocation.business.auth.AuthError;
import ru.tyupik.sharemylocation.business.auth.AuthException;
import ru.tyupik.sharemylocation.business.common.User;
import ru.tyupik.sharemylocation.business.profile.ProfileError;
import ru.tyupik.sharemylocation.business.profile.ProfileException;

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

    private ProfileException toProfileException(Throwable throwable) {
        Throwable cause = throwable;
        if (throwable instanceof ExecutionException && throwable.getCause() != null) {
            cause = throwable.getCause();
        }
        if (cause instanceof ProfileException) {
            return (ProfileException) cause;
        }
        if (cause instanceof FirebaseFirestoreException
                || cause instanceof FirebaseNetworkException
                || cause instanceof IOException
                || cause instanceof TimeoutException) {
            return new ProfileException(ProfileError.NetworkUnavailable.INSTANCE, cause);
        }
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new ProfileException(new ProfileError.Unexpected(cause.getMessage()), cause);
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
        Throwable cause = throwable;
        if (throwable instanceof ExecutionException && throwable.getCause() != null) {
            cause = throwable.getCause();
        }
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
        if (cause instanceof FirebaseNetworkException
                || cause instanceof IOException
                || cause instanceof TimeoutException) {
            return new AuthException(AuthError.NetworkUnavailable.INSTANCE, cause);
        }
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new AuthException(new AuthError.Unexpected(cause.getMessage()), cause);
    }
}
