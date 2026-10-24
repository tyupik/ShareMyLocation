package ru.tyupik.sharemylocation.data.contacts

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.contacts.ContactsRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface ContactsModule {

    @Binds
    @Singleton
    fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository
}
