package ru.tyupik.sharemylocation.data.profile

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.profile.ProfileRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface ProfileModule {

    @Binds
    @Singleton
    fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}
