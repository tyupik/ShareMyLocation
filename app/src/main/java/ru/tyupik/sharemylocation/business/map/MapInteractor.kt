package ru.tyupik.sharemylocation.business.map

import kotlinx.coroutines.flow.Flow

/**
 * Интерактор карты: собирает друзей, чьи локации видны текущему пользователю.
 *
 * Семантика видимости (Фаза 5): настройка друга активна (isEnabled) И его аудитория
 * пускает текущего пользователя — ALL_FRIENDS, либо его uid ∈ allowedUids, либо он состоит
 * в memberUids хотя бы одной группы из allowedGroupIds. Отсутствующие настройки («не шарит»),
 * недоступные группы (GroupNotFound) и протухшие allowedGroupIds трактованы как «не видно»;
 * взаимность дружбы не проверяется — её гейтят правила чтения locations.
 */
interface MapInteractor {

    /**
     * Realtime-поток видимых друзей: точки locations + имена/аватар-версии из списка друзей.
     * Точки обновляются realtime-слушателем; поток также переэмитирует тиками каждые 5 секунд,
     * чтобы «как давно» на экране старело без изменений данных.
     *
     * Видимость тоже realtime: настройки шаринга друзей слушаются по uid
     * (observeSharingSettingsByUids), поэтому смена isEnabled/audience/allowedUids/allowedGroupIds
     * у друга меняет набор видимых без перезахода на вкладку; удаление друга перезапускает
     * конвейер через realtime-поток друзей. Границы realtime: состав memberUids групп владельца
     * и удаление группы — одноразовый get при пересборке проекции видимости (документы
     * groups/{id}, моих settings-слушателей не касаются) — обновится на следующий ScreenShown,
     * видимость через allowedGroupIds остаётся по снимку на момент подписки.
     */
    fun observeVisibleFriends(): Flow<List<VisibleFriend>>
}
