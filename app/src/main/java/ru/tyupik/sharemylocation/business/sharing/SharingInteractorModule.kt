package ru.tyupik.sharemylocation.business.sharing

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SharingInteractorModule {

    @Binds
    @Singleton
    fun bindSharingInteractor(impl: SharingInteractorImpl): SharingInteractor
}
