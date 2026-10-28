package com.youjin.app.data

import com.youjin.app.domain.Goal
import com.youjin.app.domain.GoalChange
import com.youjin.app.domain.GoalStatus
import com.youjin.app.domain.GoalStep
import com.youjin.app.domain.GoalType
import com.youjin.app.domain.ProgressCalculator
import com.youjin.app.domain.ProgressEntry
import com.youjin.app.domain.ReminderSpec
import com.youjin.app.domain.WeeklyReview

// ---------- 实体 -> 领域 ----------

fun GoalEntity.toDomain(
    entries: List<ProgressEntryEntity>,
    steps: List<GoalStepEntity>,
): Goal {
    val (current, ratio) = computeProgress(this, entries, steps)
    return Goal(
        id = id,
        name = name,
        type = type.toGoalType(),
        category = category,
        status = status.toGoalStatus(),
        unit = unit,
        targetValue = targetValue,
        initialValue = initialValue,
        startDate = startDate,
        deadline = deadline,
        motivation = motivation,
        nextAction = nextAction,
        pinned = pinned,
        pinOrder = pinOrder,
        colorIndex = colorIndex,
        iconIndex = iconIndex,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        currentValue = current,
        ratio = ratio,
        stepsTotal = if (type.toGoalType() == GoalType.CHECKLIST) steps.size else 0,
    )
}

/** 统一进度计算：清单型以步骤完成数为 currentValue，比例 = 已完成 / 总数 */
internal fun computeProgress(
    goal: GoalEntity,
    entries: List<ProgressEntryEntity>,
    steps: List<GoalStepEntity>,
): Pair<Double, Double> = when (goal.type.toGoalType()) {
    GoalType.CUMULATIVE -> {
        val current = ProgressCalculator.cumulativeCurrent(goal.initialValue, entries.map { it.delta })
        current to ProgressCalculator.cumulativeRatio(current, goal.targetValue)
    }
    GoalType.CHECKLIST -> {
        val done = steps.count { it.done }
        done.toDouble() to ProgressCalculator.checklistRatio(done, steps.size)
    }
}

fun ProgressEntryEntity.toDomain(): ProgressEntry = ProgressEntry(
    id = id, goalId = goalId, delta = delta, occurDate = occurDate,
    note = note, createdAt = createdAt, updatedAt = updatedAt,
)

fun GoalStepEntity.toDomain(): GoalStep = GoalStep(
    id = id, goalId = goalId, title = title, sortOrder = sortOrder,
    done = done, doneDate = doneDate, updatedAt = updatedAt,
)

fun ReminderEntity.toDomain(): ReminderSpec = ReminderSpec(
    id = id, goalId = goalId, enabled = enabled,
    weekdaysMask = weekdaysMask, hour = hour, minute = minute,
)

fun WeeklyReviewEntity.toDomain(): WeeklyReview = WeeklyReview(
    id = id, weekStart = weekStart, body = body, createdAt = createdAt, updatedAt = updatedAt,
)

fun GoalChangeEntity.toDomain(): GoalChange = GoalChange(
    id = id, goalId = goalId, field = field, oldValue = oldValue,
    newValue = newValue, changedAt = changedAt,
)

// ---------- 领域 -> 实体 ----------

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id, name = name, type = type.name, category = category,
    status = status.name, unit = unit, targetValue = targetValue,
    initialValue = initialValue, startDate = startDate, deadline = deadline,
    motivation = motivation, nextAction = nextAction, pinned = pinned,
    pinOrder = pinOrder, colorIndex = colorIndex, iconIndex = iconIndex,
    createdAt = createdAt, updatedAt = updatedAt, completedAt = completedAt,
)

fun ProgressEntry.toEntity(): ProgressEntryEntity = ProgressEntryEntity(
    id = id, goalId = goalId, delta = delta, occurDate = occurDate,
    note = note, createdAt = createdAt, updatedAt = updatedAt,
)

fun GoalStep.toEntity(): GoalStepEntity = GoalStepEntity(
    id = id, goalId = goalId, title = title, sortOrder = sortOrder,
    done = done, doneDate = doneDate, updatedAt = updatedAt,
)

fun ReminderSpec.toEntity(): ReminderEntity = ReminderEntity(
    id = id, goalId = goalId, enabled = enabled, weekdaysMask = weekdaysMask,
    hour = hour, minute = minute,
)

fun WeeklyReview.toEntity(): WeeklyReviewEntity = WeeklyReviewEntity(
    id = id, weekStart = weekStart, body = body, createdAt = createdAt, updatedAt = updatedAt,
)

// ---------- 枚举映射（容忍历史脏数据） ----------

fun String.toGoalType(): GoalType = runCatching { GoalType.valueOf(this) }.getOrDefault(GoalType.CUMULATIVE)

fun String.toGoalStatus(): GoalStatus = runCatching { GoalStatus.valueOf(this) }.getOrDefault(GoalStatus.ACTIVE)
