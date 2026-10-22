package ru.tyupik.sharemylocation.business.map

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MapInteractorModule {

    @Binds
    @Singleton
    fun bindMapInteractor(impl: MapInteractorImpl): MapInteractor
}
