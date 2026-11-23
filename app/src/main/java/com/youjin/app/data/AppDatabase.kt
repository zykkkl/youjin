package com.youjin.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        GoalEntity::class,
        ProgressEntryEntity::class,
        GoalStepEntity::class,
        ReminderEntity::class,
        WeeklyReviewEntity::class,
        GoalChangeEntity::class,
        PlanItemEntity::class,
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun entryDao(): EntryDao
    abstract fun stepDao(): StepDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reviewDao(): ReviewDao
    abstract fun changeDao(): ChangeDao
    abstract fun planItemDao(): PlanItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 -> v2：新增周计划表 plan_items。
         * 0.3.x 已经发布并带用户真实数据，不能再依赖破坏性重建，必须给出真实的迁移。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `plan_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `weekStart` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `goalId` INTEGER,
                        `importance` INTEGER NOT NULL,
                        `urgency` INTEGER NOT NULL,
                        `done` INTEGER NOT NULL,
                        `doneAt` INTEGER,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_items_weekStart` ON `plan_items` (`weekStart`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_items_goalId` ON `plan_items` (`goalId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "youjin.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
