package ru.tyupik.sharemylocation.business.map

import ru.tyupik.sharemylocation.business.sharing.SharedLocation

/**
 * Источник собственной локации для вкладки «Карта»: гейт прав для точки isMyLocationEnabled
 * и фиксы для центровки камеры. Модель точки переиспользуется из business.sharing
 * ([SharedLocation]): координаты и точность совпадают с системным Location,
 * а updatedAt документа Firestore карте не нужен.
 */
interface MyLocationProvider {

    /**
     * Есть ли фактический рантайм-грант ACCESS_FINE_LOCATION или ACCESS_COARSE_LOCATION.
     * Только реальные гранты: isMyLocationEnabled без них — SecurityException Maps SDK,
     * «оптимистичных» допущений здесь быть не должно.
     */
    fun hasLocationPermission(): Boolean

    /**
     * Последняя известная точка (lastLocation FusedLocationProvider); null — фикса нет,
     * прав нет либо источник отказал (SecurityException/ошибка API). При null камеру
     * не трогаем — контракт [ru.tyupik.sharemylocation.ui.map.MapUiState.myLocation].
     */
    suspend fun getLastKnownLocation(): SharedLocation?

    /** One-shot запрос свежей точки; null при таймауте/отказе/отсутствии прав. */
    suspend fun getCurrentLocation(): SharedLocation?
}
