package ru.tyupik.sharemylocation.business.contacts

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ContactsInteractorModule {

    @Binds
    @Singleton
    fun bindContactsInteractor(impl: ContactsInteractorImpl): ContactsInteractor
}
