package ru.tyupik.sharemylocation.business.sharing

/**
 * Управление foreground-сервисом отправки геолокации (Фаза 6). Системный компонент,
 * поэтому не MVI: единственный владелец старт/стопа — корневой оркестратор AppViewModel
 * (реагирует на записанный документ isEnabled; latency compensation делает реакцию мгновенной),
 * плюс явный стоп при выходе из аккаунта (MainViewModel) и кнопка «Остановить» в уведомлении.
 * ViewModel контекст не держит — проверки прав и сессии инкапсулированы в реализации.
 */
interface LocationSharingController {

    /**
     * Идемпотентно приводит сервис к состоянию [enabled]: true — стартует сервис, только если
     * есть разрешение на геолокацию и текущий пользователь Firebase (иначе «включено без прав»
     * — ложное обещание, сервис молча не работает); false — останавливает сервис (no-op, если
     * не запущен).
     */
    fun setSharingEnabled(enabled: Boolean)

    /** Есть ли разрешение на геолокацию: ACCESS_FINE_LOCATION или ACCESS_COARSE_LOCATION. */
    fun hasLocationPermission(): Boolean
}
