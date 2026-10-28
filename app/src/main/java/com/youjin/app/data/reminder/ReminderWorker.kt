package com.youjin.app.data.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.youjin.app.MainActivity
import com.youjin.app.R
import com.youjin.app.YoujinApp
import com.youjin.app.data.AppDatabase
import com.youjin.app.domain.GoalStatus
import java.time.LocalDate

/**
 * 提醒任务：检查当天是否为所选星期，是则发出通知。
 * 通知使用通用文案（避免锁屏暴露目标名称），点击进入对应目标详情。
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val goalId = inputData.getLong("goalId", -1L)
        if (goalId <= 0) return Result.success()

        val db = AppDatabase.getInstance(applicationContext)
        val goal = db.goalDao().getById(goalId) ?: return Result.success()
        if (goal.status != GoalStatus.ACTIVE.name) return Result.success()
        val reminder = db.reminderDao().getByGoal(goalId) ?: return Result.success()
        if (!reminder.enabled) return Result.success()

        // 在设备当前时区判断今天是否属于所选星期
        val mask = 1 shl LocalDate.now().dayOfWeek.value
        if ((reminder.weekdaysMask and mask) == 0) return Result.success()

        postNotification(goalId)
        return Result.success()
    }

    private fun postNotification(goalId: Long) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 通知权限被拒绝：其他功能不受影响
            return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(YoujinApp.EXTRA_GOAL_ID, goalId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            goalId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, YoujinApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("有进 · 今天该行动啦")
            .setContentText("打开看看你的目标进展")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(goalId.toInt(), notification)
    }
}
