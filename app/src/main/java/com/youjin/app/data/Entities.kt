package com.youjin.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** GoalType.name */
    val type: String,
    val category: String?,
    /** GoalStatus.name */
    val status: String,
    val unit: String,
    val targetValue: Double,
    val initialValue: Double,
    /** epochDay，可空表示未设 */
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
)

@Entity(
    tableName = "progress_entries",
    foreignKeys = [ForeignKey(
        entity = GoalEntity::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("goalId")]
)
data class ProgressEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    /** 本次新增量，必须大于 0 */
    val delta: Double,
    /** 发生日期（epochDay），补记按该日期统计 */
    val occurDate: Long,
    val note: String?,
    /** 录入时间（epochMillis） */
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "goal_steps",
    foreignKeys = [ForeignKey(
        entity = GoalEntity::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("goalId")]
)
data class GoalStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val title: String,
    val sortOrder: Int,
    val done: Boolean,
    val doneDate: Long?,
    val updatedAt: Long,
)

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = GoalEntity::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("goalId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val enabled: Boolean,
    /** DayOfWeek.value 位掩码，周一=bit1 … 周日=bit7 */
    val weekdaysMask: Int,
    val hour: Int,
    val minute: Int,
)

@Entity(
    tableName = "weekly_reviews",
    indices = [Index(value = ["weekStart"], unique = true)]
)
data class WeeklyReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStart: Long,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "goal_changes")
data class GoalChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val field: String,
    val oldValue: String,
    val newValue: String,
    val changedAt: Long,
)

/**
 * 周计划事项：属于某一周（weekStart = 周一 epochDay），
 * 用「重要性 / 紧急性」两个 1..10 的分值定位在四象限图上。
 */
@Entity(
    tableName = "plan_items",
    foreignKeys = [ForeignKey(
        entity = GoalEntity::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("weekStart"), Index("goalId")]
)
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStart: Long,
    val title: String,
    /** 可选：关联的目标，目标删除后置空 */
    val goalId: Long?,
    /** 1..10，越高越重要 */
    val importance: Int,
    /** 1..10，越高越紧急 */
    val urgency: Int,
    val done: Boolean,
    val doneAt: Long?,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
