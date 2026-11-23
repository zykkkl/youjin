package com.youjin.app.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youjin.app.data.GoalRepository
import com.youjin.app.data.PlanRepository
import com.youjin.app.domain.Goal
import com.youjin.app.domain.GoalStatus
import com.youjin.app.domain.PlanItem
import com.youjin.app.domain.PlanQuadrant
import com.youjin.app.domain.ProgressCalculator
import com.youjin.app.domain.quadrantOf
import com.youjin.app.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 计划编辑面板的草稿。id 为空表示新建 */
data class PlanEditorState(
    val id: Long? = null,
    val title: String = "",
    val goalId: Long? = null,
    val importance: Int = 5,
    val urgency: Int = 5,
) {
    val isEdit: Boolean get() = id != null
    val canSave: Boolean get() = title.isNotBlank()
    val quadrant: PlanQuadrant get() = quadrantOf(importance, urgency)
}

data class PlanUiState(
    val weekStart: Long = ProgressCalculator.weekStart(Dates.todayEpochDay()),
    val items: List<PlanItem> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val loading: Boolean = true,
) {
    /** 未完成：重要的排前面；完成的不再出现在坐标里 */
    val pending: List<PlanItem> get() = items.filter { !it.done }
        .sortedWith(
            compareByDescending<PlanItem> { it.importance }
                .thenByDescending { it.urgency }
                .thenBy { it.id }
        )

    val completed: List<PlanItem> get() = items.filter { it.done }
        .sortedByDescending { it.doneAt ?: 0L }

    val goalsById: Map<Long, Goal> get() = goals.associateBy { it.id }

    val doneCount: Int get() = items.count { it.done }

    val isCurrentWeek: Boolean
        get() = weekStart == ProgressCalculator.weekStart(Dates.todayEpochDay())

    val weekLabel: String
        get() = "${Dates.formatMonthDayWeekday(weekStart)} – ${Dates.formatMonthDayWeekday(weekStart + 6)}"
}

class PlanViewModel(
    private val planRepo: PlanRepository,
    goalRepo: GoalRepository,
) : ViewModel() {

    private val weekStartFlow = MutableStateFlow(ProgressCalculator.weekStart(Dates.todayEpochDay()))

    private val _editor = MutableStateFlow<PlanEditorState?>(null)
    val editor: StateFlow<PlanEditorState?> = _editor.asStateFlow()

    /** 拖动中的实时分值（未落库），松手后写入数据库 */
    private val _dragging = MutableStateFlow<Map<Long, Pair<Int, Int>>>(emptyMap())
    val dragging: StateFlow<Map<Long, Pair<Int, Int>>> = _dragging.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlanUiState> = combine(
        weekStartFlow.flatMapLatest { week ->
            planRepo.observeWeek(week).map { items -> week to items }
        },
        goalRepo.goals,
        weekStartFlow,
    ) { (week, items), goals, currentWeek ->
        PlanUiState(
            weekStart = currentWeek,
            items = items,
            goals = goals,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlanUiState(),
    )

    fun prevWeek() {
        weekStartFlow.value -= 7
    }

    fun nextWeek() {
        weekStartFlow.value += 7
    }

    fun goThisWeek() {
        weekStartFlow.value = ProgressCalculator.weekStart(Dates.todayEpochDay())
    }

    // ---------- 编辑面板 ----------

    fun openAdd() {
        _editor.value = PlanEditorState()
    }

    fun openEdit(item: PlanItem) {
        _editor.value = PlanEditorState(
            id = item.id,
            title = item.title,
            goalId = item.goalId,
            importance = item.importance,
            urgency = item.urgency,
        )
    }

    fun closeEditor() {
        _editor.value = null
    }

    fun setEditorTitle(value: String) {
        _editor.value = _editor.value?.copy(title = value.take(120))
    }

    fun setEditorGoal(goalId: Long?) {
        _editor.value = _editor.value?.copy(goalId = goalId)
    }

    fun setEditorImportance(value: Int) {
        _editor.value = _editor.value?.copy(importance = value.coerceIn(1, 10))
    }

    fun setEditorUrgency(value: Int) {
        _editor.value = _editor.value?.copy(urgency = value.coerceIn(1, 10))
    }

    fun saveEditor() {
        val draft = _editor.value ?: return
        if (!draft.canSave) {
            viewModelScope.launch { _messages.emit("先写点要做什么吧") }
            return
        }
        viewModelScope.launch {
            runCatching {
                if (draft.id == null) {
                    planRepo.add(
                        weekStart = weekStartFlow.value,
                        title = draft.title,
                        goalId = draft.goalId,
                        importance = draft.importance,
                        urgency = draft.urgency,
                    )
                } else {
                    planRepo.update(
                        id = draft.id,
                        title = draft.title,
                        goalId = draft.goalId,
                        importance = draft.importance,
                        urgency = draft.urgency,
                    )
                }
            }.onSuccess {
                _editor.value = null
            }.onFailure {
                _messages.emit(it.message ?: "保存失败")
            }
        }
    }

    fun deleteEditorItem() {
        val draft = _editor.value ?: return
        val id = draft.id ?: return
        viewModelScope.launch {
            planRepo.delete(id)
            _editor.value = null
        }
    }

    // ---------- 列表与坐标 ----------

    fun toggleDone(item: PlanItem) {
        viewModelScope.launch {
            planRepo.setDone(item.id, !item.done)
        }
    }

    fun previewDrag(itemId: Long, importance: Int, urgency: Int) {
        _dragging.value = _dragging.value + (itemId to (importance to urgency))
    }

    fun commitDrag(item: PlanItem) {
        val position = _dragging.value[item.id] ?: return
        _dragging.value = _dragging.value - item.id
        viewModelScope.launch {
            planRepo.move(item.id, position.first, position.second)
        }
    }

    /** 可关联的目标：只列未归档的目标 */
    fun linkableGoals(goals: List<Goal>): List<Goal> =
        goals.filter { it.status != GoalStatus.ARCHIVED }
}
