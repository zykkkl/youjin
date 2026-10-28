package com.youjin.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youjin.app.data.ReviewRepository
import com.youjin.app.domain.ProgressCalculator
import com.youjin.app.domain.WeeklyReview
import com.youjin.app.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModel(private val repo: ReviewRepository) : ViewModel() {

    private val currentWeekStart = ProgressCalculator.weekStart(Dates.todayEpochDay())
    private val weekStartFlow = MutableStateFlow(currentWeekStart)

    val weekStart: StateFlow<Long> = weekStartFlow.asStateFlow()

    val stats: StateFlow<ReviewRepository.WeekStats?> = weekStartFlow
        .flatMapLatest { repo.weekStatsFlow(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val review: StateFlow<WeeklyReview?> = weekStartFlow
        .flatMapLatest { repo.reviewFlow(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun prevWeek() {
        weekStartFlow.value = weekStartFlow.value - 7
    }

    /** 回顾只看已过去的时间，不能跳转到未来周 */
    fun nextWeek() {
        val next = weekStartFlow.value + 7
        if (next <= currentWeekStart) weekStartFlow.value = next
    }

    val isCurrentWeek: Boolean get() = weekStartFlow.value == currentWeekStart

    fun saveReview(body: String) {
        viewModelScope.launch { repo.saveReview(weekStartFlow.value, body) }
    }
}
