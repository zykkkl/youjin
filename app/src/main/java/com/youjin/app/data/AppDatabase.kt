package com.youjin.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GoalEntity::class,
        ProgressEntryEntity::class,
        GoalStepEntity::class,
        ReminderEntity::class,
        WeeklyReviewEntity::class,
        GoalChangeEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun entryDao(): EntryDao
    abstract fun stepDao(): StepDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reviewDao(): ReviewDao
    abstract fun changeDao(): ChangeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "youjin.db"
                )
                    // 首版无历史迁移；开发期结构变更允许重建（正式发布前应改为 Migration）
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
