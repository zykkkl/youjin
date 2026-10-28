package com.youjin.app.data

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

/**
 * 后台检查更新任务：读取更新源 manifest，发现新版本时发通知（点击进入设置页）。
 * manifest 地址在 [UpdateConfig.MANIFEST_URL] 配置；未配置时任务空转。
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = runCatching { UpdateChecker(applicationContext).check() }.getOrNull()
            ?: return if (runAttemptCount >= 2) Result.success() else Result.retry()

        if (!result.configured) return Result.success()
        val latest = result.latest ?: return Result.success()

        postNotification(latest)
        return Result.success()
    }

    private fun postNotification(info: UpdateInfo) {
        // Android 13+ 需要通知权限；未授权时静默跳过（仍可在设置页手动检查）
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(YoujinApp.EXTRA_OPEN_SETTINGS, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, YoujinApp.UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("有进 · 有新版本可用")
            .setContentText("v${info.versionName} 已发布，点击查看更新说明")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 7001
    }
}
