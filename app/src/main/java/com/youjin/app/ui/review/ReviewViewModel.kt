package com.youjin.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youjin.app.data.ReviewRepository
import com.youjin.app.data.ai.AiFeatures
import com.youjin.app.data.ai.AiResult
import com.youjin.app.domain.ProgressCalculator
import com.youjin.app.domain.WeeklyReview
import com.youjin.app.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModel(
    private val repo: ReviewRepository,
    private val features: AiFeatures,
) : ViewModel() {

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

    // ---- AI 复盘草稿 ----

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    private val _aiDraft = MutableStateFlow<String?>(null)
    val aiDraft: StateFlow<String?> = _aiDraft.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** 根据本周事实数据生成复盘草稿；只填进输入框，不直接保存 */
    fun generateReview(existingNote: String) {
        if (_generating.value) return
        val current = stats.value
        if (current == null) {
            _messages.tryEmit("数据还在加载，请稍后再试")
            return
        }
        _generating.value = true
        viewModelScope.launch {
            when (
                val result = features.draftReview(
                    weekStart = weekStartFlow.value,
                    stats = current,
                    existingNote = existingNote.trim().takeIf { it.isNotEmpty() },
                )
            ) {
                is AiResult.Ok -> {
                    _aiDraft.value = result.data
                    _messages.tryEmit("AI 已生成复盘草稿，可以修改后保存")
                }

                is AiResult.Failure -> _messages.tryEmit(result.message)
            }
            _generating.value = false
        }
    }

    fun consumeAiDraft() {
        _aiDraft.value = null
    }
}
