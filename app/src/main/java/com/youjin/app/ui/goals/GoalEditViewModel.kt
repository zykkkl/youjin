package com.youjin.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youjin.app.data.GoalRepository
import com.youjin.app.domain.GoalDraft
import com.youjin.app.domain.GoalType
import com.youjin.app.domain.ReminderSpec
import com.youjin.app.util.formatNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalEditState(
    val isEdit: Boolean = false,
    val name: String = "",
    val type: GoalType = GoalType.CUMULATIVE,
    val category: String? = null,
    val unit: String = "",
    val targetText: String = "",
    val initialText: String = "0",
    val startDate: Long? = null,
    val deadline: Long? = null,
    val motivation: String = "",
    val nextAction: String = "",
    val pinned: Boolean = false,
    val colorIndex: Int = 0,
    val iconIndex: Int = 0,
    /** 清单型步骤标题（编辑时仅加载展示，修改通过详情页） */
    val steps: List<String> = listOf(""),
    val reminder: ReminderSpec? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val savedGoalId: Long? = null,
    val error: String? = null,
) {
    val targetValue: Double? get() = targetText.toDoubleOrNull()
    val initialValue: Double? get() = initialText.toDoubleOrNull()

    val dateValid: Boolean
        get() = startDate == null || deadline == null || deadline >= startDate

    val canSave: Boolean
        get() = !loading && !saving && name.isNotBlank() && dateValid && when (type) {
            GoalType.CUMULATIVE ->
                unit.isNotBlank() && (targetValue ?: 0.0) > 0 &&
                    (initialValue ?: 0.0) >= 0
            GoalType.CHECKLIST ->
                steps.map { it.trim() }.any { it.isNotEmpty() }
        }
}

class GoalEditViewModel(
    private val goalId: Long?,
    private val repo: GoalRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        GoalEditState(isEdit = goalId != null, loading = goalId != null)
    )
    val state: StateFlow<GoalEditState> = _state.asStateFlow()

    init {
        if (goalId != null) {
            viewModelScope.launch {
                val goal = repo.getGoal(goalId)
                val reminder = repo.getReminder(goalId)
                val steps = repo.getSteps(goalId)
                _state.update { s ->
                    if (goal == null) {
                        s.copy(loading = false, error = "目标不存在")
                    } else {
                        s.copy(
                            loading = false,
                            name = goal.name,
                            type = goal.type,
                            category = goal.category,
                            unit = goal.unit,
                            targetText = if (goal.type == GoalType.CUMULATIVE) formatNumber(goal.targetValue) else "",
                            initialText = formatNumber(goal.initialValue),
                            startDate = goal.startDate,
                            deadline = goal.deadline,
                            motivation = goal.motivation ?: "",
                            nextAction = goal.nextAction ?: "",
                            pinned = goal.pinned,
                            colorIndex = goal.colorIndex,
                            iconIndex = goal.iconIndex,
                            steps = if (goal.type == GoalType.CHECKLIST) steps.map { it.title } else listOf(""),
                            reminder = reminder,
                        )
                    }
                }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v.take(50)) }
    fun setType(t: GoalType) = _state.update { if (it.isEdit) it else it.copy(type = t) }
    fun setCategory(c: String?) = _state.update { it.copy(category = c) }
    fun setUnit(v: String) = _state.update { it.copy(unit = v.take(10)) }
    fun setTarget(v: String) = _state.update {
        it.copy(targetText = v.filter { c -> c.isDigit() || c == '.' }.take(12))
    }
    fun setInitial(v: String) = _state.update {
        it.copy(initialText = v.filter { c -> c.isDigit() || c == '.' }.take(12))
    }
    fun setStartDate(v: Long?) = _state.update { it.copy(startDate = v) }
    fun setDeadline(v: Long?) = _state.update { it.copy(deadline = v) }
    fun setMotivation(v: String) = _state.update { it.copy(motivation = v.take(500)) }
    fun setNextAction(v: String) = _state.update { it.copy(nextAction = v.take(200)) }
    fun setPinned(v: Boolean) = _state.update { it.copy(pinned = v) }

    /** 图标与分类色联动：同一索引 */
    fun setIconIndex(i: Int) = _state.update { it.copy(iconIndex = i, colorIndex = i) }

    fun setSteps(list: List<String>) = _state.update { it.copy(steps = list) }
    fun setReminder(spec: ReminderSpec?) = _state.update { it.copy(reminder = spec) }

    /** 保存完成标记已被消费：避免复用旧 ViewModel 时立刻触发返回 */
    fun consumeSaved() {
        _state.update { it.copy(savedGoalId = null) }
    }

    fun save(onSaved: (Long) -> Unit = {}) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val draft = GoalDraft(
                    name = s.name.trim(),
                    type = s.type,
                    category = s.category,
                    unit = s.unit.trim(),
                    targetValue = if (s.type == GoalType.CUMULATIVE) (s.targetValue ?: 0.0) else 0.0,
                    initialValue = s.initialValue ?: 0.0,
                    startDate = s.startDate,
                    deadline = s.deadline,
                    motivation = s.motivation.ifBlank { null },
                    nextAction = s.nextAction.ifBlank { null },
                    pinned = s.pinned,
                    colorIndex = s.colorIndex,
                    iconIndex = s.iconIndex,
                    steps = if (s.type == GoalType.CHECKLIST) s.steps.map { it.trim() }.filter { it.isNotEmpty() } else emptyList(),
                )
                val id = if (goalId == null) {
                    repo.createGoal(draft)
                } else {
                    repo.updateGoal(goalId, draft)
                    goalId
                }
                s.reminder?.let { repo.saveReminder(id, it.copy(goalId = id)) }
                _state.update { it.copy(saving = false, savedGoalId = id) }
                onSaved(id)
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = e.message ?: "保存失败") }
            }
        }
    }
}
