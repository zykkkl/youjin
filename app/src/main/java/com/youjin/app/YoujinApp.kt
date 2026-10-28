package com.youjin.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.youjin.app.di.AppContainer

class YoujinApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "目标提醒",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "目标的每日/指定星期提醒"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                UPDATE_CHANNEL_ID,
                "版本更新",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "自动检查发现新版本时提醒"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "goal_reminders"
        const val UPDATE_CHANNEL_ID = "update_alerts"
        const val EXTRA_GOAL_ID = "extra_goal_id"
        const val EXTRA_OPEN_SETTINGS = "extra_open_settings"
    }
}
