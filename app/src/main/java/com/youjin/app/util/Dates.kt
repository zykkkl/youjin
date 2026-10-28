package com.youjin.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 日期与数字格式化工具。所有日期字段内部使用 epochDay 或 epochMillis 存储。 */
object Dates {

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun nowMillis(): Long = System.currentTimeMillis()

    /** epochMillis 对应的本地日期 */
    fun localDateOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    /** 录入时间与发生日期是否同一天（判断是否补记） */
    fun sameLocalDay(millis: Long, epochDay: Long): Boolean =
        localDateOf(millis).toEpochDay() == epochDay

    private val dayFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    private val dayFullFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
    private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.CHINA)
    private val monthDayWeekdayFormatter = DateTimeFormatter.ofPattern("M月d日，EEEE", Locale.CHINA)

    /** "10月22日" */
    fun formatDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayFormatter)

    /** "2026年10月22日" */
    fun formatDayFull(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayFullFormatter)

    /** "星期三" */
    fun formatWeekday(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(weekdayFormatter)

    /** "10月22日，星期三" */
    fun formatMonthDayWeekday(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(monthDayWeekdayFormatter)

    /** "08:30" */
    fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

    /** "12:30"（由 epochMillis 取本地时间） */
    fun formatClock(millis: Long): String =
        localDateOf(millis).atStartOfDay().let { _ ->
            val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
            "%02d:%02d".format(t.hour, t.minute)
        }

    /** 起止日期展示：有截止 "10月1日 - 12月31日"；仅开始 "从 10月1日 开始"；仅截止 "截止 12月31日"；均无 "未设日期" */
    fun formatDateRange(start: Long?, deadline: Long?): String = when {
        start != null && deadline != null ->
            "${formatDay(start)} - ${formatDay(deadline)}"
        start != null -> "从 ${formatDay(start)} 开始"
        deadline != null -> "截止 ${formatDay(deadline)}"
        else -> "未设日期"
    }

    val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    fun weekdayName(dayOfWeek: Int): String =
        WEEKDAY_NAMES[(dayOfWeek - 1).coerceIn(0, WEEKDAY_NAMES.lastIndex)]
}

/** 数值展示：去掉多余的 ".0"，如 5.0 -> "5"，5.25 -> "5.25" */
fun formatNumber(v: Double): String = when {
    v == 0.0 -> "0"
    v % 1.0 == 0.0 && kotlin.math.abs(v) < 1e15 -> v.toLong().toString()
    else -> v.toString()
}
