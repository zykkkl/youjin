package com.youjin.app

import com.youjin.app.domain.ProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 进度计算规则单元测试。
 * 验证场景：初始量、超额钳制、步骤比例、周一起算的周统计、按发生日期统计。
 */
class ProgressCalculatorTest {

    @Test
    fun cumulativeCurrent_addsInitialValueAndDeltas() {
        assertEquals(15.0, ProgressCalculator.cumulativeCurrent(5.0, listOf(3.0, 7.0)), 1e-9)
        assertEquals(0.0, ProgressCalculator.cumulativeCurrent(0.0, emptyList()), 1e-9)
    }

    @Test
    fun cumulativeRatio_clampsBetweenZeroAndOne() {
        assertEquals(0.5, ProgressCalculator.cumulativeRatio(30.0, 60.0), 1e-9)
        // 超额：比例钳制在 1，实际数值在文字中保留
        assertEquals(1.0, ProgressCalculator.cumulativeRatio(65.0, 60.0), 1e-9)
        assertEquals(0.0, ProgressCalculator.cumulativeRatio(10.0, 0.0), 1e-9)
    }

    @Test
    fun checklistRatio_usesDoneOverTotal() {
        assertEquals(0.5, ProgressCalculator.checklistRatio(2, 4), 1e-9)
        assertEquals(0.0, ProgressCalculator.checklistRatio(0, 0), 1e-9)
        assertEquals(1.0, ProgressCalculator.checklistRatio(3, 2), 1e-9)
    }

    @Test
    fun weekStart_isMonday() {
        // 2026-10-21 是周三，所在周的周一为 2026-10-19
        val wednesday = LocalDate.of(2026, 10, 21).toEpochDay()
        val monday = LocalDate.of(2026, 10, 19).toEpochDay()
        assertEquals(monday, ProgressCalculator.weekStart(wednesday))
    }

    @Test
    fun weeklyDelta_usesOccurrenceDateNotEntryTime() {
        val monday = LocalDate.of(2026, 10, 19).toEpochDay()
        val entries = listOf(
            monday to 5.0,      // 周一，计入
            monday + 3 to 2.0,  // 周四，计入
            monday + 8 to 9.0,  // 下周二，不计入
        )
        assertEquals(7.0, ProgressCalculator.weeklyDelta(entries, monday), 1e-9)
        assertEquals(2, ProgressCalculator.weeklyEntryCount(entries, monday))
    }

    @Test
    fun stepsDoneInWeek_countsStepsCompletedInWeek() {
        val monday = LocalDate.of(2026, 10, 19).toEpochDay()
        val steps = listOf(
            monday to true,      // 计入
            monday + 2 to true,  // 计入
            null to true,        // 已完成但无完成日期（历史数据），不计入
            monday + 9 to true,  // 下周完成，不计入
        )
        assertEquals(2, ProgressCalculator.stepsDoneInWeek(steps, monday))
    }

    @Test
    fun percent_roundsWithinRange() {
        assertEquals(30, ProgressCalculator.percent(0.3))
        assertEquals(100, ProgressCalculator.percent(1.0))
        assertEquals(0, ProgressCalculator.percent(0.0))
    }
}
