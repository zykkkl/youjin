package com.youjin.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youjin.app.data.GoalRepository
import com.youjin.app.domain.Goal
import com.youjin.app.domain.GoalStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class StatusFilter(val label: String) {
    ALL("全部"),
    ACTIVE("进行中"),
    PAUSED("已暂停"),
    OVERDUE("已逾期"),
    COMPLETED("已完成"),
    ARCHIVED("已归档"),
}

enum class GoalSort(val label: String) {
    PINNED("置顶优先"),
    UPDATED("最近更新"),
    CREATED("创建时间"),
}

data class GoalsUiState(
    val query: String = "",
    val filter: StatusFilter = StatusFilter.ALL,
    val sort: GoalSort = GoalSort.PINNED,
    val goals: List<Goal> = emptyList(),
)

class GoalsViewModel(private val repo: GoalRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(StatusFilter.ALL)
    private val sort = MutableStateFlow(GoalSort.PINNED)

    val uiState: StateFlow<GoalsUiState> =
        combine(repo.goals, query, filter, sort) { goals, q, f, s ->
            val matched = goals.filter { goal ->
                val statusOk = when (f) {
                    StatusFilter.ALL -> true
                    StatusFilter.ACTIVE -> goal.status == GoalStatus.ACTIVE
                    StatusFilter.PAUSED -> goal.status == GoalStatus.PAUSED
                    StatusFilter.OVERDUE -> goal.isOverdue
                    StatusFilter.COMPLETED -> goal.status == GoalStatus.COMPLETED
                    StatusFilter.ARCHIVED -> goal.status == GoalStatus.ARCHIVED
                }
                val queryOk = q.isBlank() ||
                    goal.name.contains(q.trim(), ignoreCase = true) ||
                    (goal.category?.contains(q.trim(), ignoreCase = true) ?: false)
                statusOk && queryOk
            }
            val sorted = when (s) {
                GoalSort.PINNED -> matched.sortedWith(
                    compareByDescending<Goal> { it.pinned }
                        .thenBy { it.pinOrder }
                        .thenByDescending { it.updatedAt }
                )
                GoalSort.UPDATED -> matched.sortedByDescending { it.updatedAt }
                GoalSort.CREATED -> matched.sortedByDescending { it.createdAt }
            }
            GoalsUiState(query = q, filter = f, sort = s, goals = sorted)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GoalsUiState(),
        )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFilter(value: StatusFilter) {
        filter.value = value
    }

    fun setSort(value: GoalSort) {
        sort.value = value
    }
}
