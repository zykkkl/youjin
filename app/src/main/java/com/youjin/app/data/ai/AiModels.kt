package com.youjin.app.data.ai

import com.youjin.app.domain.GoalType

/** AI 助手配置（API Key 在存储层加密） */
data class AiConfig(
    val enabled: Boolean = false,
    val providerId: String = AiProviders.default.id,
    val baseUrl: String = AiProviders.default.baseUrl,
    val model: String = "",
    val apiKey: String = "",
    val privacyAccepted: Boolean = false,
) {
    /** 模板是否允许不填 Key（本地服务 / 自建网关） */
    val keyOptional: Boolean
        get() = !(AiProviders.byId(providerId)?.keyRequired ?: true)

    val credentialsReady: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && (apiKey.isNotBlank() || keyOptional)

    /** 可以发起 AI 请求：已启用 + 已确认隐私说明 + 凭据完整 */
    val ready: Boolean
        get() = enabled && privacyAccepted && credentialsReady
}

/** AI 调用结果：失败时只带可读文案，不把异常抛给界面 */
sealed interface AiResult<out T> {
    data class Ok<T>(val data: T) : AiResult<T>
    data class Failure(val message: String) : AiResult<Nothing>
}

/** 目标拆解请求：来自新建/编辑目标页的当前草稿 */
data class DecomposeRequest(
    val name: String,
    val type: GoalType,
    val unit: String,
    val targetValue: Double,
    val deadlineEpochDay: Long?,
    val motivation: String,
)

/** AI 给出的拆解建议：步骤 + 一句下一步行动 */
data class PlanSuggestion(
    val steps: List<String>,
    val nextAction: String?,
)
