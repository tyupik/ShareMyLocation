package ru.tyupik.sharemylocation.data.notifications

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.tyupik.sharemylocation.business.notifications.NotificationPermissionController
import ru.tyupik.sharemylocation.business.notifications.NotificationPublisher

@Module
@InstallIn(SingletonComponent::class)
internal interface NotificationsModule {

    @Binds
    @Singleton
    fun bindNotificationPublisher(impl: NotificationPublisherImpl): NotificationPublisher

    @Binds
    @Singleton
    fun bindNotificationPermissionController(
        impl: NotificationPermissionControllerImpl,
    ): NotificationPermissionController
}
