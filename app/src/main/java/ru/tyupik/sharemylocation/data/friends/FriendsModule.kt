package ru.tyupik.sharemylocation.data.friends

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.friends.FriendsRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface FriendsModule {

    @Binds
    @Singleton
    fun bindFriendsRepository(impl: FriendsRepositoryImpl): FriendsRepository
}
