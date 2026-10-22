package ru.tyupik.sharemylocation.business.sharing

import kotlinx.coroutines.flow.Flow

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

    /** Запись точки геолокации владельца (вызывается сервисом, Фаза 6). */
    suspend fun updateLocation(location: SharedLocation): Result<Unit>

    /** Частичная запись флага isEnabled (merge {isEnabled, updatedAt}); остальное — автосейв экрана. */
    suspend fun saveEnabled(isEnabled: Boolean): Result<Unit>

    /** Realtime-поток флага isEnabled для корневого оркестратора сервиса; нет документа — false. */
    fun observeIsEnabled(): Flow<Boolean>
}
