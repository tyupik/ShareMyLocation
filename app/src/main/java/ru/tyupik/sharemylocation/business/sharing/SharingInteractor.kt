package ru.tyupik.sharemylocation.business.sharing

/**
 * Интерактор настроек шаринга: оркестрирует [SharingRepository] и возвращает бизнес-модели.
 * Валидация на этом слое не нужна: аудитория — enum, а состав списков отсекает ViewModel
 * (пересечение с актуальными друзьями/группами перед записью).
 */
interface SharingInteractor {

    /** Одноразовое чтение настроек; отсутствующий документ — [SharingSettings.default], не ошибка. */
    suspend fun getSettings(): Result<SharingSettings>

    /** Полная идемпотентная запись снимка настроек (автосохранение экрана). */
    suspend fun saveSettings(settings: SharingSettings): Result<Unit>
}
