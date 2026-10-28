package com.youjin.app.domain

/** 目标类型：首版支持累计数值与步骤清单 */
enum class GoalType { CUMULATIVE, CHECKLIST }

/** 目标状态：进行中 / 暂停 / 完成 / 归档 */
enum class GoalStatus { ACTIVE, PAUSED, COMPLETED, ARCHIVED }

/** 目标分类（可选） */
val CATEGORIES = listOf("健康", "学习", "工作", "财务", "兴趣", "其他")

/** 目标图标（emoji，索引对应 colorIndex 使用同一套调色板） */
val GOAL_EMOJIS = listOf("🎯", "🏃", "📚", "💪", "💰", "🎸", "✍️", "🌱")

/**
 * 目标领域模型。currentValue 与 ratio 为计算字段，始终由 ProgressCalculator 推导，
 * UI 层不得自行计算。
 */
data class Goal(
    val id: Long,
    val name: String,
    val type: GoalType,
    val category: String?,
    val status: GoalStatus,
    val unit: String,
    val targetValue: Double,
    val initialValue: Double,
    val startDate: Long?,
    val deadline: Long?,
    val motivation: String?,
    val nextAction: String?,
    val pinned: Boolean,
    val pinOrder: Int,
    val colorIndex: Int,
    val iconIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    /** 当前值：累计型 = 初始量 + 记录增量之和；清单型 = 已完成步骤数 */
    val currentValue: Double,
    /** 完成比例 0..1，按类型用不同规则计算 */
    val ratio: Double,
    /** 清单型：步骤总数（累计型为 0） */
    val stepsTotal: Int,
) {
    val isOverdue: Boolean
        get() = status == GoalStatus.ACTIVE && deadline != null &&
            deadline!! < java.time.LocalDate.now().toEpochDay()

    /** 是否已达到终点：累计型看数值，清单型看全部步骤完成（且至少有一个步骤） */
    fun reachedTarget(): Boolean = when (type) {
        GoalType.CUMULATIVE -> targetValue > 0.0 && currentValue >= targetValue
        GoalType.CHECKLIST -> ratio >= 1.0
    }

    fun iconEmoji(): String = GOAL_EMOJIS[iconIndex.coerceIn(0, GOAL_EMOJIS.lastIndex)]
}

/** 一条进度记录（新增量） */
data class ProgressEntry(
    val id: Long,
    val goalId: Long,
    val delta: Double,
    /** 发生日期（epochDay），补记按该日期统计 */
    val occurDate: Long,
    val note: String?,
    /** 录入时间（epochMillis） */
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isBackfill: Boolean
        get() = !com.youjin.app.util.Dates.sameLocalDay(createdAt, occurDate)
}

/** 清单型目标的步骤 */
data class GoalStep(
    val id: Long,
    val goalId: Long,
    val title: String,
    val sortOrder: Int,
    val done: Boolean,
    val doneDate: Long?,
    val updatedAt: Long,
)

/** 提醒配置。weekdaysMask 按 DayOfWeek.value（周一=1 … 周日=7）置位 */
data class ReminderSpec(
    val id: Long,
    val goalId: Long,
    val enabled: Boolean,
    val weekdaysMask: Int,
    val hour: Int,
    val minute: Int,
) {
    val isDaily: Boolean get() = weekdaysMask == ALL_DAYS_MASK
    companion object {
        const val ALL_DAYS_MASK = 0b11111110
    }
}

/** 周回顾笔记，weekStart 为该周周一的 epochDay，唯一 */
data class WeeklyReview(
    val id: Long,
    val weekStart: Long,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 目标字段变更留痕（目标值、日期、状态调整等） */
data class GoalChange(
    val id: Long,
    val goalId: Long,
    val field: String,
    val oldValue: String,
    val newValue: String,
    val changedAt: Long,
)

/** 创建/编辑目标的输入草稿 */
data class GoalDraft(
    val name: String,
    val type: GoalType,
    val category: String?,
    val unit: String,
    val targetValue: Double,
    val initialValue: Double,
    val startDate: Long?,
    val deadline: Long?,
    val motivation: String?,
    val nextAction: String?,
    val pinned: Boolean,
    val colorIndex: Int,
    val iconIndex: Int,
    /** 仅清单型使用：创建时的步骤标题列表（编辑时忽略，步骤在详情页维护） */
    val steps: List<String> = emptyList(),
)
