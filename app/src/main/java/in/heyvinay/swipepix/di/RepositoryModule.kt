package `in`.heyvinay.swipepix.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.heyvinay.swipepix.data.repository.CleanupSessionRepository
import `in`.heyvinay.swipepix.data.repository.CleanupSessionRepositoryImpl
import `in`.heyvinay.swipepix.data.repository.MediaRepository
import `in`.heyvinay.swipepix.data.repository.MediaRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: MediaRepositoryImpl,
    ): MediaRepository

    @Binds
    @Singleton
    abstract fun bindCleanupSessionRepository(
        impl: CleanupSessionRepositoryImpl,
    ): CleanupSessionRepository
}
