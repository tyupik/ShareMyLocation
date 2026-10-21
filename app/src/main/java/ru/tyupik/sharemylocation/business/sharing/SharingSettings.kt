package ru.tyupik.sharemylocation.business.sharing

/**
 * Бизнес-модель настроек шаринга геолокации: документ sharingSettings/{uid} в Firestore.
 *
 * Семантика для сервиса отправки локации (Фаза 6):
 * - [isEnabled] == false — «не отправлять никому», [audience] и списки игнорируются;
 * - isEnabled == true и audience == [SharingAudience.ALL_FRIENDS] — видят все friendUids владельца;
 * - isEnabled == true и audience == [SharingAudience.CUSTOM] — получатель видит локацию, если его
 *   uid ∈ [allowedUids] ИЛИ он состоит в memberUids какой-либо группы из [allowedGroupIds];
 *   пересечения/дубли допустимы — union, дедупликация на стороне читателя. Пустые списки
 *   при CUSTOM — «не видно никому» (валидно, на экране есть подсказка-Effect).
 *
 * Границы (осознанно НЕ делаем): сервис отправки (Фаза 6), чтение настроек другими участниками
 * (Фаза 7), каскадная чистка при удалении друга (TODO остаётся в FriendsInteractor),
 * realtime-слушатель настроек, аватары в чекбоксах.
 * Риск: Фаза 6 не должна доверять [allowedUids]/[allowedGroupIds] без перевалидации —
 * взаимную дружбу и актуальный состав групп проверять при отправке (унаследованный риск
 * самозаписи в чужой friendUids, см. firestore.rules).
 *
 * [updatedAt] — millis последней записи; 0 — поле отсутствует.
 */
data class SharingSettings(
    val uid: String,
    val isEnabled: Boolean,
    val audience: SharingAudience,
    val allowedUids: List<String>,
    val allowedGroupIds: List<String>,
    val updatedAt: Long = 0L,
) {
    companion object {

        /**
         * Дефолт отсутствующего документа: шаринг выключен, аудитория ALL_FRIENDS, выбор пуст.
         * [JvmName] нужен, потому что «default» — зарезервированное слово Java,
         * а фабрику вызывает FirebaseBackend (data/auth).
         */
        @JvmStatic
        @JvmName("defaultOf")
        fun default(uid: String): SharingSettings = SharingSettings(
            uid = uid,
            isEnabled = false,
            audience = SharingAudience.ALL_FRIENDS,
            allowedUids = emptyList(),
            allowedGroupIds = emptyList(),
        )
    }
}

/** Аудитория шаринга: все друзья либо выбранные люди и группы. */
enum class SharingAudience {
    ALL_FRIENDS,
    CUSTOM,
}
