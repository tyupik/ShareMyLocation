package ru.tyupik.sharemylocation.ui.sharing

/**
 * SSOT экрана настроек шаринга геолокации. Ключевое решение — автосохранение без кнопки
 * «Сохранить»: переключатель — обещание приватности «выключено = уже выключено», каждое
 * изменение немедленно пишет полный снимок всех пяти полей документа (идемпотентно).
 * Ошибка записи — оптимистичный откат к последнему успешному снимку + inline-баннер.
 */
data class SharingSettingsUiState(
    /** Первичное чтение документа; при каждом ScreenShown снова true — полноэкранный спиннер. */
    val isLoading: Boolean = true,
    /** Экран-ошибка чтения; «Повторить» = повторный ScreenShown. */
    val isLoadFailed: Boolean = false,
    /** Конкретная причина экрана-ошибки; null — generic-текст. */
    val loadError: SharingUiError? = null,
    /** Главный переключатель: выключен = локацию не видит никто; дефолт отсутствующего документа. */
    val isEnabled: Boolean = false,
    /** Аудитория шаринга; секции выбора «Люди»/«Группы» видны только в [SharingAudienceUi.CUSTOM]. */
    val audience: SharingAudienceUi = SharingAudienceUi.ALL_FRIENDS,
    /** Секция «Люди»: [SharingCheckRowUiModel.isChecked] = выбор ∩ актуальные друзья. */
    val friends: List<SharingCheckRowUiModel> = emptyList(),
    /** Секция «Группы»: [SharingCheckRowUiModel.isChecked] = выбор ∩ актуальные группы. */
    val groups: List<SharingCheckRowUiModel> = emptyList(),
    /** Пассивная недоступность realtime-секции «Люди» (паттерн Friends); восстанавливается ScreenShown. */
    val isFriendsLoadFailed: Boolean = false,
    /** Пассивная недоступность realtime-секции «Группы»; восстанавливается ScreenShown. */
    val isGroupsLoadFailed: Boolean = false,
    /** Только индикатор «Сохранение…»: контролы при автосохранении НЕ дизейблятся. */
    val isSaving: Boolean = false,
    /** Ошибка автосохранения для баннера; сбрасывается по SaveErrorDismissed и при каждом ScreenShown. */
    val saveError: SharingUiError? = null,
    /**
     * Баннер «нет доступа к геолокации» (Фаза 6): попытка включить тумблер без прав, отказ
     * в системном диалоге или документ «включён» при не выданных правах на ScreenShown.
     * Включение без прав не персистится — сервис без ACCESS_FINE_LOCATION не работает.
     */
    val isPermissionMissing: Boolean = false,
)

/** Аудитория шаринга в терминах Ui-слоя. */
enum class SharingAudienceUi {
    /** Все друзья владельца. */
    ALL_FRIENDS,

    /** Выбранные люди и группы; пустой выбор = «не видно никому» (валидно, есть подсказка). */
    CUSTOM,
}

/** Действия пользователя на экране настроек шаринга. */
sealed class SharingSettingsIntent {

    /**
     * Экран показан: чистый лист транзиентов (isSaving, saveError, isLoadFailed и флаги секций),
     * одноразовое чтение документа (отсутствующий — дефолт), перезапуск realtime-потоков
     * друзей и групп. Повтор по «Повторить» — тот же intent.
     */
    data object ScreenShown : SharingSettingsIntent()

    /** Переключение главного тумблера; сохраняется автосохранением. */
    data class EnabledToggled(val value: Boolean) : SharingSettingsIntent()

    /** Выбор аудитории; сохраняется автосохранением. */
    data class AudienceSelected(val audience: SharingAudienceUi) : SharingSettingsIntent()

    /** Переключение чекбокса друга; сохраняется автосохранением. */
    data class FriendToggled(val uid: String) : SharingSettingsIntent()

    /** Переключение чекбокса группы; сохраняется автосохранением. */
    data class GroupToggled(val groupId: String) : SharingSettingsIntent()

    /** Скрытие баннера ошибки автосохранения. */
    data object SaveErrorDismissed : SharingSettingsIntent()

    /**
     * Результат системного диалога прав геолокации (Фаза 6): granted — персист isEnabled = true
     * обычным автосейвом; отказ — тумблер остаётся выключенным, показывается баннер.
     */
    data class LocationPermissionsResult(val granted: Boolean) : SharingSettingsIntent()
}

/** One-off события экрана. */
sealed class SharingSettingsEffect {

    /**
     * Подсказка-снекбар «сейчас локацию не увидит никто»: переход в CUSTOM с пустым выбором
     * или включение тумблера при CUSTOM с пустым выбором.
     */
    data object ShowEmptyAudienceHint : SharingSettingsEffect()

    /**
     * Запрос системного диалога прав (Фаза 6): экран запускает rememberLauncherForActivityResult
     * с ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION и POST_NOTIFICATIONS; результат
     * возвращается intent'ом [SharingSettingsIntent.LocationPermissionsResult].
     */
    data object RequestLocationPermissions : SharingSettingsEffect()
}
