package ru.tyupik.sharemylocation.business.friends

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FriendsInteractorModule {

    @Binds
    @Singleton
    fun bindFriendsInteractor(impl: FriendsInteractorImpl): FriendsInteractor
}
