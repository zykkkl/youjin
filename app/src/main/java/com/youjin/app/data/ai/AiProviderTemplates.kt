package com.youjin.app.data.ai

/**
 * 大模型服务商模板。
 *
 * 这些服务商都提供与 OpenAI `/chat/completions` 兼容的接口，所以应用只需要一个协议实现，
 * 模板只负责预填 Base URL、默认模型与常用模型，所有字段用户都可以修改。
 * 模型名变化很快，因此模型字段是自由文本，并支持「拉取可用模型」（GET /models）。
 */
data class AiProviderTemplate(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String,
    val suggestedModels: List<String> = emptyList(),
    /** 是否必须填写 API Key（本地 Ollama 或自建网关可以留空） */
    val keyRequired: Boolean = true,
    val note: String? = null,
)

object AiProviders {

    const val CUSTOM_ID = "custom"
    const val OLLAMA_ID = "ollama"

    val builtin: List<AiProviderTemplate> = listOf(
        AiProviderTemplate(
            id = "openai",
            displayName = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-4o-mini",
            suggestedModels = listOf("gpt-4o-mini", "gpt-4o"),
        ),
        AiProviderTemplate(
            id = "deepseek",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
            defaultModel = "deepseek-chat",
            suggestedModels = listOf("deepseek-chat", "deepseek-reasoner"),
        ),
        AiProviderTemplate(
            id = "minimax",
            displayName = "MiniMax",
            baseUrl = "https://api.minimax.chat/v1",
            defaultModel = "MiniMax-Text-01",
            suggestedModels = listOf("MiniMax-Text-01", "abab6.5s-chat"),
        ),
        AiProviderTemplate(
            id = "moonshot",
            displayName = "Kimi（月之暗面）",
            baseUrl = "https://api.moonshot.cn/v1",
            defaultModel = "moonshot-v1-8k",
            suggestedModels = listOf("moonshot-v1-8k", "moonshot-v1-32k"),
        ),
        AiProviderTemplate(
            id = "zhipu",
            displayName = "智谱 GLM",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            defaultModel = "glm-4-flash",
            suggestedModels = listOf("glm-4-flash", "glm-4-plus"),
        ),
        AiProviderTemplate(
            id = "qwen",
            displayName = "通义千问",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            defaultModel = "qwen-plus",
            suggestedModels = listOf("qwen-plus", "qwen-max", "qwen-turbo"),
        ),
        AiProviderTemplate(
            id = "siliconflow",
            displayName = "SiliconFlow",
            baseUrl = "https://api.siliconflow.cn/v1",
            defaultModel = "Qwen/Qwen2.5-7B-Instruct",
            suggestedModels = listOf("Qwen/Qwen2.5-7B-Instruct"),
        ),
        AiProviderTemplate(
            id = "openrouter",
            displayName = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "openrouter/auto",
            suggestedModels = listOf("openrouter/auto"),
        ),
        AiProviderTemplate(
            id = OLLAMA_ID,
            displayName = "本地 Ollama",
            baseUrl = "http://192.168.1.2:11434/v1",
            defaultModel = "",
            keyRequired = false,
            note = "需在电脑上运行 Ollama 并开启局域网访问；把地址改成电脑的局域网 IP，模型名如 qwen2.5:7b",
        ),
        AiProviderTemplate(
            id = CUSTOM_ID,
            displayName = "自定义",
            baseUrl = "",
            defaultModel = "",
            keyRequired = false,
            note = "任何 OpenAI 兼容服务（含 LM Studio、vLLM、自建网关），API Key 可留空",
        ),
    )

    val default: AiProviderTemplate get() = builtin.first()

    fun byId(id: String): AiProviderTemplate? = builtin.firstOrNull { it.id == id }

    fun displayNameOf(id: String): String = byId(id)?.displayName ?: "自定义"

    /** 规范化 Base URL：去空白、去尾部斜杠、容忍用户误填完整 /chat/completions 路径 */
    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim()
        while (url.endsWith("/")) url = url.dropLast(1)
        if (url.endsWith("/chat/completions")) url = url.removeSuffix("/chat/completions")
        while (url.endsWith("/")) url = url.dropLast(1)
        return url
    }

    fun chatEndpoint(baseUrl: String): String = normalizeBaseUrl(baseUrl) + "/chat/completions"

    fun modelsEndpoint(baseUrl: String): String = normalizeBaseUrl(baseUrl) + "/models"
}
