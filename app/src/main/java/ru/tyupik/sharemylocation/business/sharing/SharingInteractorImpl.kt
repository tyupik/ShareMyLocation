package ru.tyupik.sharemylocation.business.sharing

import javax.inject.Inject
import javax.inject.Singleton

/** Реализация интерактора настроек шаринга поверх репозитория (тонкий проброс). */
@Singleton
class SharingInteractorImpl @Inject constructor(
    private val sharingRepository: SharingRepository,
) : SharingInteractor {

    override suspend fun getSettings(): Result<SharingSettings> = sharingRepository.getSettings()

    override suspend fun saveSettings(settings: SharingSettings): Result<Unit> =
        sharingRepository.saveSettings(settings)
}
