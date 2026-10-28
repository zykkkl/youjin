package com.youjin.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youjin.app.data.BackupManager
import com.youjin.app.data.SettingsRepository
import com.youjin.app.data.ThemeMode
import com.youjin.app.data.UpdateChecker
import com.youjin.app.data.UpdateInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 检查更新的界面状态 */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    /** 未配置更新源（UpdateConfig.MANIFEST_URL 为空） */
    data object NotConfigured : UpdateUiState
    data class UpToDate(val currentVersion: String) : UpdateUiState
    data class Available(val currentVersion: String, val info: UpdateInfo) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val backup: BackupManager,
    private val updateChecker: UpdateChecker,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val reduceAnimations: StateFlow<Boolean> = settings.reduceAnimations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 是否每天后台自动检查更新（默认开启） */
    val autoCheckUpdates: StateFlow<Boolean> = settings.autoCheckUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _update = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val update: StateFlow<UpdateUiState> = _update.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setReduceAnimations(value: Boolean) {
        viewModelScope.launch { settings.setReduceAnimations(value) }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoCheckUpdates(enabled) }
    }

    fun checkUpdate() {
        if (_update.value is UpdateUiState.Checking) return
        _update.value = UpdateUiState.Checking
        viewModelScope.launch {
            _update.value = runCatching { updateChecker.check() }.fold(
                onSuccess = { result ->
                    when {
                        !result.configured -> UpdateUiState.NotConfigured
                        result.latest != null -> UpdateUiState.Available(result.currentVersionName, result.latest)
                        else -> UpdateUiState.UpToDate(result.currentVersionName)
                    }
                },
                onFailure = { UpdateUiState.Failed(it.message ?: "网络异常") },
            )
        }
    }

    /** 消费一次检查结果，避免对话框/提示重复出现 */
    fun consumeUpdateState() {
        _update.value = UpdateUiState.Idle
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            runCatching { backup.exportJson(uri) }
                .onSuccess { _messages.emit("JSON 备份已导出") }
                .onFailure { _messages.emit("导出失败：${it.message}") }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            runCatching { backup.exportCsv(uri) }
                .onSuccess { _messages.emit("CSV 记录已导出") }
                .onFailure { _messages.emit("导出失败：${it.message}") }
        }
    }

    fun importJson(uri: Uri) {
        viewModelScope.launch {
            runCatching { backup.importJson(uri) }
                .onSuccess { r ->
                    val orphanNote = if (r.droppedOrphans > 0) "（已跳过 ${r.droppedOrphans} 条失效数据）" else ""
                    _messages.emit("恢复完成：目标 ${r.goals} 个、记录 ${r.entries} 条、步骤 ${r.steps} 个$orphanNote")
                }
                .onFailure { _messages.emit("恢复失败，数据未变更：${it.message}") }
        }
    }
}
