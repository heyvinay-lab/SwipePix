package `in`.heyvinay.swipepix.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.heyvinay.swipepix.data.local.SwipePixDatabase
import `in`.heyvinay.swipepix.data.local.dao.CleaningSessionDao
import `in`.heyvinay.swipepix.data.local.dao.PhotoDecisionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SwipePixDatabase {
        return Room.databaseBuilder(
            context,
            SwipePixDatabase::class.java,
            "swipepix_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePhotoDecisionDao(database: SwipePixDatabase): PhotoDecisionDao {
        return database.photoDecisionDao()
    }

    @Provides
    fun provideCleaningSessionDao(database: SwipePixDatabase): CleaningSessionDao {
        return database.cleaningSessionDao()
    }
}
