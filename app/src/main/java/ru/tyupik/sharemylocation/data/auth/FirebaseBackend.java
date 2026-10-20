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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_DISPLAY_NAME = "displayName";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_UPDATED_AT = "updatedAt";

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
        return new User(firebaseUser.getUid(), firebaseUser.getEmail(), firebaseUser.getDisplayName());
    }

    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    /** Создаёт users/{uid} с createdAt, если документа ещё нет, иначе обновляет поля (merge). */
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
