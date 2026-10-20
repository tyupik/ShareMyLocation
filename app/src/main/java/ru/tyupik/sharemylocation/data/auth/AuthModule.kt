package ru.tyupik.sharemylocation.data.auth

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.auth.AuthRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthModule {

    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseBackend(): FirebaseBackend = FirebaseBackend()
    }
}
