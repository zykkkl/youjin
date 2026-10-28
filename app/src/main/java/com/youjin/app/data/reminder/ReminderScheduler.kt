package com.youjin.app.data.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.youjin.app.domain.ReminderSpec
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * 提醒调度：每个启用了提醒的目标注册一个每日周期任务（24h，允许偏差），
 * 任务运行时检查“今天是否是所选星期”再决定是否发通知。
 * 目标暂停/完成/归档/删除、提醒关闭时取消对应任务。
 */
class ReminderScheduler(private val context: Context) {

    fun schedule(goalId: Long, spec: ReminderSpec) {
        if (!spec.enabled) {
            cancel(goalId)
            return
        }
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(nextDelayMillis(spec), TimeUnit.MILLISECONDS)
            .setInputData(androidx.work.workDataOf("goalId" to goalId))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName(goalId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(goalId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(goalId))
    }

    /** 恢复备份后统一重排：按配置重新注册或取消 */
    suspend fun rescheduleAll(reminders: List<Pair<Long, ReminderSpec>>) {
        val wm = WorkManager.getInstance(context)
        reminders.forEach { (goalId, spec) ->
            if (spec.enabled) schedule(goalId, spec) else wm.cancelUniqueWork(workName(goalId))
        }
    }

    /** 计算到下一次（按星期掩码匹配的）提醒时刻的毫秒数，最多向后找 8 天 */
    private fun nextDelayMillis(spec: ReminderSpec): Long {
        val zone = ZoneId.systemDefault()
        var day = LocalDate.now(zone)
        for (i in 0..8) {
            val candidate = day.atTime(spec.hour, spec.minute)
            val mask = 1 shl candidate.dayOfWeek.value
            if ((spec.weekdaysMask and mask) != 0) {
                val targetMillis = candidate.atZone(zone).toInstant().toEpochMilli()
                val delay = targetMillis - System.currentTimeMillis()
                if (delay > 60_000) return delay
            }
            day = day.plusDays(1)
        }
        return 60_000
    }

    private fun workName(goalId: Long) = "reminder-$goalId"
}
