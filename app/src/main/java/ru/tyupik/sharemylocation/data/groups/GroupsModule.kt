package ru.tyupik.sharemylocation.data.groups

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.groups.GroupsRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface GroupsModule {

    @Binds
    @Singleton
    fun bindGroupsRepository(impl: GroupsRepositoryImpl): GroupsRepository
}
