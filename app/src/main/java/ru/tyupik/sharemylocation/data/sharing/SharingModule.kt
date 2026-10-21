package ru.tyupik.sharemylocation.data.sharing

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.sharing.SharingRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface SharingModule {

    @Binds
    @Singleton
    fun bindSharingRepository(impl: SharingRepositoryImpl): SharingRepository
}
