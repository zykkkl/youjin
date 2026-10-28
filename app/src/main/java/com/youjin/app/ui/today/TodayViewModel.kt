package com.youjin.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youjin.app.data.GoalRepository
import com.youjin.app.domain.Goal
import com.youjin.app.domain.GoalStatus
import com.youjin.app.domain.GoalStep
import com.youjin.app.domain.ProgressEntry
import com.youjin.app.util.Dates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val today: Long = Dates.todayEpochDay(),
    /** 首页展示的进行中目标：置顶优先，最多 5 个 */
    val visibleGoals: List<Goal> = emptyList(),
    /** 今日记录：数值记录与步骤完成合并后，按最近操作时间倒序（后操作的排最前） */
    val records: List<TodayRecord> = emptyList(),
    val goalsById: Map<Long, Goal> = emptyMap(),
    /** 各目标的全部步骤（供首页步骤面板勾选） */
    val stepsByGoal: Map<Long, List<GoalStep>> = emptyMap(),
    val hasAnyGoal: Boolean = false,
    val hasActiveGoals: Boolean = false,
)

/**
 * 首页“今日记录”的一条操作。数值记录与步骤勾选合并为同一条时间线，
 * 最近一次操作排最前，撤销动作自然作用于最近操作（栈式 LIFO）。
 */
sealed interface TodayRecord {
    val timestampMillis: Long
    val key: String

    data class EntryRecord(val entry: ProgressEntry) : TodayRecord {
        override val timestampMillis: Long get() = entry.updatedAt
        override val key: String get() = "entry-${entry.id}"
    }

    data class StepRecord(val step: GoalStep) : TodayRecord {
        override val timestampMillis: Long get() = step.updatedAt
        override val key: String get() = "step-${step.id}"
    }
}

class TodayViewModel(private val repo: GoalRepository) : ViewModel() {

    val uiState: StateFlow<TodayUiState> =
        combine(repo.goals, repo.allEntries, repo.allSteps) { goals, entries, steps ->
            val today = Dates.todayEpochDay()
            val active = goals.filter { it.status == GoalStatus.ACTIVE }
            val visible = if (active.any { it.pinned }) {
                active.filter { it.pinned }.take(5)
            } else {
                active.take(5)
            }
            // 合并两类操作并按最近操作时间倒序：后操作的先显示、先撤销
            val records = buildList<TodayRecord> {
                entries.filter { it.occurDate == today }
                    .forEach { add(TodayRecord.EntryRecord(it)) }
                steps.filter { it.done && it.doneDate == today }
                    .forEach { add(TodayRecord.StepRecord(it)) }
            }.sortedByDescending { it.timestampMillis }
            TodayUiState(
                today = today,
                visibleGoals = visible,
                records = records,
                goalsById = goals.associateBy { it.id },
                stepsByGoal = steps.groupBy { it.goalId },
                hasAnyGoal = goals.isNotEmpty(),
                hasActiveGoals = active.isNotEmpty(),
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TodayUiState(),
        )

    fun record(
        goalId: Long,
        delta: Double,
        occurDate: Long,
        note: String?,
        onDone: (Long) -> Unit = {},
    ) {
        viewModelScope.launch {
            val id = repo.addEntry(goalId, delta, occurDate, note)
            onDone(id)
        }
    }

    fun updateEntry(entryId: Long, delta: Double, occurDate: Long, note: String?) {
        viewModelScope.launch { repo.updateEntry(entryId, delta, occurDate, note) }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch { repo.deleteEntry(entryId) }
    }

    /** 撤销今天完成的步骤（回到未完成） */
    fun undoToggleStep(stepId: Long) {
        viewModelScope.launch { repo.toggleStep(stepId, done = false) }
    }

    /** 步骤清单型目标的记录方式：勾选 / 取消勾选步骤 */
    fun toggleStep(stepId: Long, done: Boolean) {
        viewModelScope.launch { repo.toggleStep(stepId, done) }
    }

    /** 首页长按菜单：归档 / 取消归档 */
    fun setStatus(goalId: Long, status: GoalStatus) {
        viewModelScope.launch { repo.setStatus(goalId, status) }
    }

    /** 首页长按菜单：删除目标（返回快照用于撤销） */
    fun deleteGoal(goalId: Long, onResult: (GoalRepository.GoalSnapshot?) -> Unit) {
        viewModelScope.launch { onResult(repo.deleteGoal(goalId)) }
    }

    fun undoDelete(snapshot: GoalRepository.GoalSnapshot) {
        viewModelScope.launch { repo.undoDelete(snapshot) }
    }
}
