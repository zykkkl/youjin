package com.youjin.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youjin.app.data.GoalRepository
import com.youjin.app.domain.Goal
import com.youjin.app.domain.GoalStatus
import com.youjin.app.domain.GoalStep
import com.youjin.app.domain.ProgressEntry
import com.youjin.app.domain.ReminderSpec
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalDetailViewModel(
    private val goalId: Long,
    private val repo: GoalRepository,
) : ViewModel() {

    val goal: StateFlow<Goal?> = repo.goalFlow(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val entries: StateFlow<List<ProgressEntry>> = repo.entriesFlow(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val steps: StateFlow<List<GoalStep>> = repo.stepsFlow(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminder: StateFlow<ReminderSpec?> = repo.reminderFlow(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun record(delta: Double, occurDate: Long, note: String?, onDone: (Long) -> Unit = {}) {
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

    fun toggleStep(stepId: Long, done: Boolean) {
        viewModelScope.launch { repo.toggleStep(stepId, done) }
    }

    fun addStep(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repo.addStep(goalId, title) }
    }

    fun deleteStep(stepId: Long) {
        viewModelScope.launch { repo.deleteStep(stepId) }
    }

    fun setStatus(status: GoalStatus) {
        viewModelScope.launch { repo.setStatus(goalId, status) }
    }

    fun confirmComplete() {
        viewModelScope.launch { repo.confirmComplete(goalId) }
    }

    fun setNextAction(action: String?) {
        viewModelScope.launch { repo.setNextAction(goalId, action) }
    }

    fun saveReminder(spec: ReminderSpec?) {
        viewModelScope.launch { repo.saveReminder(goalId, spec) }
    }

    fun togglePin() {
        viewModelScope.launch {
            val g = goal.value ?: return@launch
            repo.setPinned(goalId, !g.pinned)
        }
    }

    fun deleteGoal(onResult: (GoalRepository.GoalSnapshot?) -> Unit) {
        viewModelScope.launch { onResult(repo.deleteGoal(goalId)) }
    }

    fun undoDelete(snapshot: GoalRepository.GoalSnapshot) {
        viewModelScope.launch { repo.undoDelete(snapshot) }
    }
}
