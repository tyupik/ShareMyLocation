package ru.tyupik.sharemylocation.data.map

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.map.MapRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface MapModule {

    @Binds
    @Singleton
    fun bindMapRepository(impl: MapRepositoryImpl): MapRepository
}
