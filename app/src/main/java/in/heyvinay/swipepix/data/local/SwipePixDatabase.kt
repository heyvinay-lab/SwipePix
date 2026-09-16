package `in`.heyvinay.swipepix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import `in`.heyvinay.swipepix.data.local.dao.CleaningSessionDao
import `in`.heyvinay.swipepix.data.local.dao.PhotoDecisionDao
import `in`.heyvinay.swipepix.data.local.entity.CleaningSessionEntity
import `in`.heyvinay.swipepix.data.local.entity.PhotoDecisionEntity

@Database(
    entities = [
        PhotoDecisionEntity::class,
        CleaningSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SwipePixDatabase : RoomDatabase() {
    abstract fun photoDecisionDao(): PhotoDecisionDao
    abstract fun cleaningSessionDao(): CleaningSessionDao
}
