package com.youjin.app.di

import android.app.Application
import com.youjin.app.data.AppDatabase
import com.youjin.app.data.BackupManager
import com.youjin.app.data.GoalRepository
import com.youjin.app.data.ReviewRepository
import com.youjin.app.data.SettingsRepository
import com.youjin.app.data.UpdateChecker
import com.youjin.app.data.reminder.ReminderScheduler

/**
 * 手动依赖容器（构造注入）。首版保持简单，暂不引入 Hilt。
 * 所有依赖通过 Application 获取，ViewModel 通过工厂创建。
 */
class AppContainer(private val app: Application) {

    val database: AppDatabase by lazy { AppDatabase.getInstance(app) }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(app) }

    val goalRepository: GoalRepository by lazy {
        GoalRepository(
            db = database,
            goalDao = database.goalDao(),
            entryDao = database.entryDao(),
            stepDao = database.stepDao(),
            reminderDao = database.reminderDao(),
            changeDao = database.changeDao(),
            reminderScheduler = reminderScheduler,
        )
    }

    val reviewRepository: ReviewRepository by lazy {
        ReviewRepository(
            reviewDao = database.reviewDao(),
            goalDao = database.goalDao(),
            entryDao = database.entryDao(),
            stepDao = database.stepDao(),
        )
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(app) }

    val backupManager: BackupManager by lazy { BackupManager(app, database, reminderScheduler) }

    val updateChecker: UpdateChecker by lazy { UpdateChecker(app) }
}
