package com.youjin.app.data

import com.youjin.app.domain.GoalStatus
import com.youjin.app.domain.GoalType
import com.youjin.app.domain.ProgressCalculator
import com.youjin.app.domain.WeeklyReview
import com.youjin.app.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** 周回顾仓库：周统计只做事实性汇总，异单位不相加、不评价勤奋程度 */
class ReviewRepository(
    private val reviewDao: ReviewDao,
    private val goalDao: GoalDao,
    private val entryDao: EntryDao,
    private val stepDao: StepDao,
) {

    data class GoalWeekStat(
        val goalId: Long,
        val name: String,
        val type: GoalType,
        val unit: String,
        val delta: Double,
        val entryCount: Int,
        val stepsDone: Int,
        val colorIndex: Int,
        val iconIndex: Int,
    )

    data class WeekStats(
        val weekStart: Long,
        val goals: List<GoalWeekStat>,
    ) {
        /** 本周有新增记录的目标数（数值记录或完成步骤） */
        val activeGoalCount: Int get() = goals.count { it.entryCount > 0 || it.stepsDone > 0 }
    }

    fun reviewFlow(weekStart: Long): Flow<WeeklyReview?> =
        reviewDao.observeByWeek(weekStart).map { it?.toDomain() }

    fun weekStatsFlow(weekStart: Long): Flow<WeekStats> =
        combine(goalDao.observeAll(), entryDao.observeAll(), stepDao.observeAll()) { g, e, s ->
            val goals = g
                .filter { it.status == GoalStatus.ACTIVE.name || it.status == GoalStatus.PAUSED.name }
                .map { goal ->
                    val entries = e.filter { it.goalId == goal.id }
                    val steps = s.filter { it.goalId == goal.id }
                    GoalWeekStat(
                        goalId = goal.id,
                        name = goal.name,
                        type = goal.type.toGoalType(),
                        unit = goal.unit,
                        delta = ProgressCalculator.weeklyDelta(entries.map { it.occurDate to it.delta }, weekStart),
                        entryCount = ProgressCalculator.weeklyEntryCount(entries.map { it.occurDate to it.delta }, weekStart),
                        stepsDone = ProgressCalculator.stepsDoneInWeek(steps.map { it.doneDate to it.done }, weekStart),
                        colorIndex = goal.colorIndex,
                        iconIndex = goal.iconIndex,
                    )
                }
            WeekStats(weekStart, goals)
        }

    suspend fun saveReview(weekStart: Long, body: String) {
        val now = Dates.nowMillis()
        val existing = reviewDao.getByWeek(weekStart)
        reviewDao.upsert(
            WeeklyReviewEntity(
                id = existing?.id ?: 0,
                weekStart = weekStart,
                body = body,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        )
    }
}
