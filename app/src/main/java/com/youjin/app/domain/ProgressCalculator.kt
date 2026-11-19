package com.youjin.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 业务计算集中管理：所有进度、完成比例、周统计都由这里计算。
 * 记录保存、修改、删除后，读取方通过同一套规则得到最新结果。
 */
object ProgressCalculator {

    /** 累计数值型：当前值 = 初始已完成量 + 有效记录增量之和 */
    fun cumulativeCurrent(initialValue: Double, deltas: List<Double>): Double =
        initialValue + deltas.sum()

    /** 累计数值型：完成比例 = 当前值 / 目标值，限制在 0..1，实际数值保留超额 */
    fun cumulativeRatio(current: Double, target: Double): Double =
        if (target <= 0.0) 0.0 else (current / target).coerceIn(0.0, 1.0)

    /** 步骤清单型：完成比例 = 已完成步骤数 / 步骤总数 */
    fun checklistRatio(doneCount: Int, totalCount: Int): Double =
        if (totalCount <= 0) 0.0 else (doneCount.toDouble() / totalCount).coerceIn(0.0, 1.0)

    /** 打卡型：当前值 = 已打卡天数（同一天多次打卡只算一天） */
    fun checkinCurrent(dates: Collection<Long>): Double = dates.toSet().size.toDouble()

    /**
     * 打卡型：当前连续天数。
     * 今天已打卡则从今天往前数；今天还没打卡时从昨天往前数——当天尚未结束，不算断。
     */
    fun currentStreak(dates: Set<Long>, today: Long): Int {
        if (dates.isEmpty()) return 0
        var cursor = when {
            dates.contains(today) -> today
            dates.contains(today - 1) -> today - 1
            else -> return 0
        }
        var count = 0
        while (dates.contains(cursor)) {
            count++
            cursor--
        }
        return count
    }

    /** 打卡型：历史最长连续天数 */
    fun bestStreak(dates: Set<Long>): Int {
        if (dates.isEmpty()) return 0
        val sorted = dates.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /** 打卡型：一周内的打卡天数 */
    fun checkinsInWeek(dates: Collection<Long>, weekStart: Long): Int =
        dates.count { inWeek(it, weekStart) }

    /** 周起始（周一）的 epochDay */
    fun weekStart(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).with(DayOfWeek.MONDAY).toEpochDay()

    /** 是否落在以 weekStart 起算的一周内（含周一，不含下周一） */
    fun inWeek(epochDay: Long, weekStart: Long): Boolean =
        epochDay >= weekStart && epochDay < weekStart + 7

    /** 一周内新增量之和 */
    fun weeklyDelta(entries: List<Pair<Long, Double>>, weekStart: Long): Double =
        entries.filter { inWeek(it.first, weekStart) }.sumOf { it.second }

    /** 一周内记录条数 */
    fun weeklyEntryCount(entries: List<Pair<Long, Double>>, weekStart: Long): Int =
        entries.count { inWeek(it.first, weekStart) }

    /** 一周内完成的步骤数（按完成日期计入，不生成重复的数值增量） */
    fun stepsDoneInWeek(steps: List<Pair<Long?, Boolean>>, weekStart: Long): Int =
        steps.count { (doneDate, done) ->
            done && doneDate != null && inWeek(doneDate, weekStart)
        }

    /** 百分比取整（展示用） */
    fun percent(ratio: Double): Int = (ratio * 100).toInt().coerceIn(0, 100)
}
