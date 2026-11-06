package com.youjin.app.data.ai

import com.youjin.app.data.ReviewRepository
import com.youjin.app.util.Dates
import kotlinx.coroutines.flow.first

/**
 * AI 能力门面：统一做「配置是否就绪」的校验，界面只关心结果文案。
 * 拆解与复盘都是一次请求，不申请任何自动写入权限——建议由用户确认后才落到目标/步骤上。
 */
class AiFeatures(
    private val settings: AiSettingsRepository,
    private val client: AiClient,
) {

    suspend fun config(): AiConfig = settings.config.first()

    suspend fun testConnection(config: AiConfig): AiResult<String> = client.complete(
        config = config,
        messages = listOf(
            AiClient.Message("system", "你是连接测试助手，只做简短回复。"),
            AiClient.Message("user", "只回复两个字：成功"),
        ),
        maxTokens = 16,
        temperature = 0.0,
    )

    suspend fun listModels(config: AiConfig): AiResult<List<String>> = client.listModels(config)

    suspend fun decompose(request: DecomposeRequest): AiResult<PlanSuggestion> {
        val config = settings.config.first()
        if (!config.ready) return AiResult.Failure(notReadyMessage())
        val result = client.complete(
            config = config,
            messages = listOf(
                AiClient.Message("system", AiPrompts.decomposeSystem()),
                AiClient.Message("user", AiPrompts.decomposeUser(request, Dates.todayEpochDay())),
            ),
            maxTokens = 900,
            temperature = 0.5,
        )
        return when (result) {
            is AiResult.Ok -> {
                val plan = AiPrompts.parsePlan(result.data)
                if (plan.steps.isEmpty() && plan.nextAction == null) {
                    AiResult.Failure("AI 返回的内容无法解析，请重试")
                } else {
                    AiResult.Ok(plan)
                }
            }

            is AiResult.Failure -> result
        }
    }

    suspend fun draftReview(
        weekStart: Long,
        stats: ReviewRepository.WeekStats,
        existingNote: String?,
    ): AiResult<String> {
        val config = settings.config.first()
        if (!config.ready) return AiResult.Failure(notReadyMessage())
        return client.complete(
            config = config,
            messages = listOf(
                AiClient.Message("system", AiPrompts.reviewSystem()),
                AiClient.Message("user", AiPrompts.reviewUser(weekStart, stats, existingNote)),
            ),
            maxTokens = 600,
            temperature = 0.5,
        )
    }

    private fun notReadyMessage(): String = "AI 助手未启用或配置不完整，请到 设置 → AI 助手 完成配置"
}
