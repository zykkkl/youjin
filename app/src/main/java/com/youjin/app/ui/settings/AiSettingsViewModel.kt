package com.youjin.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youjin.app.data.ai.AiConfig
import com.youjin.app.data.ai.AiFeatures
import com.youjin.app.data.ai.AiProviders
import com.youjin.app.data.ai.AiResult
import com.youjin.app.data.ai.AiSettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiSettingsUiState(
    val loading: Boolean = true,
    val enabled: Boolean = false,
    val providerId: String = AiProviders.default.id,
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val privacyAccepted: Boolean = false,
    val suggestedModels: List<String> = emptyList(),
    val fetchedModels: List<String> = emptyList(),
    val fetchingModels: Boolean = false,
    val saving: Boolean = false,
    val testing: Boolean = false,
    val testPassed: Boolean = false,
    val testMessage: String? = null,
    val showPrivacyDialog: Boolean = false,
) {
    val providerName: String get() = AiProviders.displayNameOf(providerId)

    val providerNote: String? get() = AiProviders.byId(providerId)?.note

    val keyOptional: Boolean get() = !(AiProviders.byId(providerId)?.keyRequired ?: true)

    val canSave: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && (apiKey.isNotBlank() || keyOptional)
}

class AiSettingsViewModel(
    private val settings: AiSettingsRepository,
    private val features: AiFeatures,
) : ViewModel() {

    private val _state = MutableStateFlow(AiSettingsUiState())
    val state: StateFlow<AiSettingsUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            val config = settings.config.first()
            val template = AiProviders.byId(config.providerId)
            _state.update {
                it.copy(
                    loading = false,
                    enabled = config.enabled,
                    providerId = config.providerId,
                    baseUrl = config.baseUrl,
                    model = config.model,
                    apiKey = config.apiKey,
                    privacyAccepted = config.privacyAccepted,
                    suggestedModels = template?.suggestedModels.orEmpty(),
                )
            }
        }
    }

    fun selectProvider(id: String) {
        val template = AiProviders.byId(id) ?: return
        _state.update {
            it.copy(
                providerId = id,
                baseUrl = template.baseUrl,
                model = template.defaultModel,
                suggestedModels = template.suggestedModels,
                fetchedModels = emptyList(),
                testMessage = null,
                testPassed = false,
            )
        }
    }

    fun setBaseUrl(value: String) {
        _state.update { it.copy(baseUrl = value.take(300), testMessage = null, testPassed = false) }
    }

    fun setModel(value: String) {
        _state.update { it.copy(model = value.take(120), testMessage = null, testPassed = false) }
    }

    fun setApiKey(value: String) {
        _state.update { it.copy(apiKey = value, testMessage = null, testPassed = false) }
    }

    fun requestEnable(enabled: Boolean) {
        if (!enabled) {
            setEnabled(false)
            return
        }
        if (!_state.value.canSave) {
            viewModelScope.launch { _messages.emit("请先填写接口地址与模型名") }
            return
        }
        if (_state.value.privacyAccepted) {
            setEnabled(true)
        } else {
            _state.update { it.copy(showPrivacyDialog = true) }
        }
    }

    fun confirmPrivacyAndEnable() {
        viewModelScope.launch {
            settings.setPrivacyAccepted(true)
            settings.setEnabled(true)
            _state.update { it.copy(privacyAccepted = true, enabled = true, showPrivacyDialog = false) }
        }
    }

    fun dismissPrivacyDialog() {
        _state.update { it.copy(showPrivacyDialog = false) }
    }

    fun save() {
        val snapshot = _state.value
        if (!snapshot.canSave) {
            viewModelScope.launch { _messages.emit("请先填写接口地址与模型名") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            settings.saveFields(snapshot.providerId, snapshot.baseUrl, snapshot.model, snapshot.apiKey)
            _state.update { it.copy(saving = false) }
            _messages.emit("AI 配置已保存")
        }
    }

    fun test() {
        val snapshot = _state.value
        if (!snapshot.canSave) {
            viewModelScope.launch { _messages.emit("请先填写接口地址与模型名") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(testing = true, testMessage = null, testPassed = false) }
            settings.saveFields(snapshot.providerId, snapshot.baseUrl, snapshot.model, snapshot.apiKey)
            val result = features.testConnection(draftConfig())
            _state.update {
                when (result) {
                    is AiResult.Ok -> it.copy(
                        testing = false,
                        testPassed = true,
                        testMessage = "连接成功，模型回复：${result.data.take(40)}",
                    )

                    is AiResult.Failure -> it.copy(testing = false, testMessage = result.message)
                }
            }
        }
    }

    fun fetchModels() {
        val snapshot = _state.value
        if (snapshot.baseUrl.isBlank()) {
            viewModelScope.launch { _messages.emit("请先填写接口地址") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(fetchingModels = true) }
            val result = features.listModels(draftConfig())
            _state.update {
                when (result) {
                    is AiResult.Ok -> it.copy(
                        fetchingModels = false,
                        fetchedModels = result.data,
                        suggestedModels = emptyList(),
                    )

                    is AiResult.Failure -> it.copy(fetchingModels = false)
                }
            }
            when (result) {
                is AiResult.Ok -> _messages.emit("已获取 ${result.data.size} 个模型")
                is AiResult.Failure -> _messages.emit(result.message)
            }
        }
    }

    private fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setEnabled(enabled)
            _state.update { it.copy(enabled = enabled) }
        }
    }

    private fun draftConfig(): AiConfig {
        val snapshot = _state.value
        return AiConfig(
            enabled = snapshot.enabled,
            providerId = snapshot.providerId,
            baseUrl = AiProviders.normalizeBaseUrl(snapshot.baseUrl),
            model = snapshot.model.trim(),
            apiKey = snapshot.apiKey.trim(),
            privacyAccepted = snapshot.privacyAccepted,
        )
    }
}
