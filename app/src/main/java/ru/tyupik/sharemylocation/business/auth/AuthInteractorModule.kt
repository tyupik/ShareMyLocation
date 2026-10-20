package ru.tyupik.sharemylocation.business.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthInteractorModule {

    @Binds
    @Singleton
    fun bindAuthInteractor(impl: AuthInteractorImpl): AuthInteractor
}
