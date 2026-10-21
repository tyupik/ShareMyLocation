package ru.tyupik.sharemylocation.business.profile

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ProfileInteractorModule {

    @Binds
    @Singleton
    fun bindProfileInteractor(impl: ProfileInteractorImpl): ProfileInteractor
}
