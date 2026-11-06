package com.youjin.app.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * OpenAI 兼容协议的轻量客户端。
 *
 * 只用 HttpURLConnection + org.json，不引入额外网络库：请求体与响应结构都很小，
 * 不做流式，超时、HTTP 状态码与网络异常统一转成可读文案。
 *
 * 兼容性兜底（各家的「兼容」并不完全一致）：
 * - 响应里没有 choices[0].message.content 时，再尝试 choices[0].text 与 MiniMax 旧版
 *   choices[0].messages[0].text 两种结构；
 * - 有些服务不支持 system 角色，空响应时把 system 合并进首条 user 重试一次；
 * - 推理模型可能把输出预算全用在推理上（finish_reason=length），此时放宽预算重试一次。
 */
class AiClient {

    data class Message(val role: String, val content: String)

    suspend fun complete(
        config: AiConfig,
        messages: List<Message>,
        maxTokens: Int = 700,
        temperature: Double = 0.4,
    ): AiResult<String> = withContext(Dispatchers.IO) {
        if (config.baseUrl.isBlank() || config.model.isBlank()) {
            return@withContext AiResult.Failure("请先填写接口地址与模型名")
        }
        try {
            val first = post(config, messages, maxTokens, temperature)
            AiResponse.content(first)?.let { return@withContext AiResult.Ok(it) }
            AiResponse.providerError(first)?.let {
                return@withContext AiResult.Failure("AI 服务返回错误：$it")
            }

            val retryMessages =
                if (messages.any { it.role.equals("system", ignoreCase = true) }) {
                    mergeSystem(messages)
                } else {
                    messages
                }
            val retryTokens =
                if (AiResponse.finishReason(first) == "length") {
                    (maxTokens * 2).coerceAtMost(3000)
                } else {
                    maxTokens
                }
            val second = post(config, retryMessages, retryTokens, temperature)
            AiResponse.content(second)?.let { return@withContext AiResult.Ok(it) }
            AiResponse.providerError(second)?.let {
                return@withContext AiResult.Failure("AI 服务返回错误：$it")
            }
            AiResult.Failure(AiResponse.emptyMessage(second))
        } catch (e: Exception) {
            AiResult.Failure(friendlyError(e))
        }
    }

    suspend fun listModels(config: AiConfig): AiResult<List<String>> = withContext(Dispatchers.IO) {
        if (config.baseUrl.isBlank()) return@withContext AiResult.Failure("请先填写接口地址")
        try {
            val text = request(
                method = "GET",
                url = AiProviders.modelsEndpoint(config.baseUrl),
                body = null,
                apiKey = config.apiKey,
            )
            val json = JSONObject(text)
            val array = json.optJSONArray("data") ?: json.optJSONArray("models")
                ?: return@withContext AiResult.Failure("该服务未提供模型列表，请手动填写模型名")
            val models = mutableListOf<String>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").ifBlank { item.optString("name") }.trim()
                if (id.isNotEmpty()) models.add(id)
            }
            val distinct = models.distinct().sorted()
            if (distinct.isEmpty()) {
                AiResult.Failure("服务没有返回可用的模型，请手动填写")
            } else {
                AiResult.Ok(distinct)
            }
        } catch (e: Exception) {
            AiResult.Failure(friendlyError(e))
        }
    }

    private fun post(
        config: AiConfig,
        messages: List<Message>,
        maxTokens: Int,
        temperature: Double,
    ): JSONObject {
        val body = JSONObject().apply {
            put("model", config.model.trim())
            put(
                "messages",
                JSONArray().apply {
                    messages.forEach { message ->
                        put(JSONObject().put("role", message.role).put("content", message.content))
                    }
                },
            )
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("stream", false)
        }.toString()
        return JSONObject(
            request(
                method = "POST",
                url = AiProviders.chatEndpoint(config.baseUrl),
                body = body,
                apiKey = config.apiKey,
            ),
        )
    }

    /** 把 system 消息并入首条 user，供不支持 system 角色的兼容实现重试。 */
    private fun mergeSystem(messages: List<Message>): List<Message> {
        val system = messages
            .filter { it.role.equals("system", ignoreCase = true) }
            .joinToString("\n\n") { it.content }
        if (system.isBlank()) return messages
        val rest = messages
            .filterNot { it.role.equals("system", ignoreCase = true) }
            .toMutableList()
        val firstUser = rest.indexOfFirst { it.role.equals("user", ignoreCase = true) }
        if (firstUser >= 0) {
            rest[firstUser] = Message("user", "$system\n\n${rest[firstUser].content}")
        } else {
            rest.add(0, Message("user", system))
        }
        return rest
    }

    private class HttpStatusException(val code: Int, val body: String) : Exception("HTTP $code")

    private fun request(method: String, url: String, body: String?, apiKey: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 90_000
            connection.setRequestProperty("Accept", "application/json")
            if (apiKey.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw HttpStatusException(code, text)
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun friendlyError(error: Throwable): String = when (error) {
        is HttpStatusException -> {
            val detail = runCatching {
                val json = JSONObject(error.body)
                json.optJSONObject("error")?.optString("message").orEmpty().ifBlank {
                    json.optJSONObject("base_resp")?.optString("status_msg").orEmpty()
                }
            }.getOrDefault("")
            when (error.code) {
                401, 403 -> "API Key 无效或没有权限（HTTP ${error.code}）"
                404 -> "接口地址或模型名不正确（HTTP 404），请检查 Base URL"
                429 -> "请求过于频繁或额度不足（HTTP 429），请稍后再试"
                in 500..599 -> "AI 服务暂不可用（HTTP ${error.code}），请稍后再试"
                else -> if (detail.isNotBlank()) "请求失败：$detail" else "请求失败（HTTP ${error.code}）"
            }
        }

        is SocketTimeoutException -> "请求超时，请检查网络或换一个服务"
        is UnknownHostException -> "无法解析服务地址，请检查 Base URL"
        is ConnectException -> "无法连接到服务，请检查网络与地址"
        is SSLException -> "安全连接失败，请确认地址使用 HTTPS"
        else -> error.message?.takeIf { it.isNotBlank() } ?: "请求失败，请稍后再试"
    }
}

/**
 * 响应解析：把各家略有差异的返回结构收敛成「一段回答文本 + 失败原因」。
 * 单独拆出来是为了能在单元测试里覆盖这些兼容分支。
 */
internal object AiResponse {

    fun content(json: JSONObject): String? {
        val choice = json.optJSONArray("choices")?.optJSONObject(0) ?: return null
        choice.optJSONObject("message")?.let { message ->
            textOf(message.opt("content"))?.takeIf { it.isNotBlank() }?.let { return it }
        }
        textOf(choice.opt("text"))?.takeIf { it.isNotBlank() }?.let { return it }
        choice.optJSONArray("messages")?.optJSONObject(0)?.let { legacy ->
            legacy.optString("text").trim().takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    fun finishReason(json: JSONObject): String? = json.optJSONArray("choices")
        ?.optJSONObject(0)
        ?.optString("finish_reason")
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

    fun providerError(json: JSONObject): String? {
        json.optJSONObject("error")?.let { error ->
            val message = error.optString("message").trim()
            if (message.isNotBlank()) return message
        }
        json.optJSONObject("base_resp")?.let { base ->
            val code = base.optInt("status_code", 0)
            if (code != 0) {
                return base.optString("status_msg").trim().ifBlank { "status_code=$code" }
            }
        }
        return null
    }

    fun hasReasoningOnly(json: JSONObject): Boolean {
        val message = json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?: return false
        return textOf(message.opt("reasoning_content"))?.isNotBlank() == true
    }

    fun emptyMessage(json: JSONObject): String {
        val reason = finishReason(json)
        return when {
            hasReasoningOnly(json) ->
                "模型这次只返回了推理内容，没有给出回答（finish_reason=${reason ?: "无"}）。请换用非推理模型再试。"
            reason == "length" ->
                "模型输出被截断且没有正文（finish_reason=length）。请重试，或换一个模型。"
            else -> {
                val snippet = json.toString().take(160)
                "服务返回了空内容（finish_reason=${reason ?: "无"}）。响应片段：$snippet"
            }
        }
    }

    private fun textOf(value: Any?): String? = when (value) {
        is String -> value.trim()
        is JSONArray -> {
            val parts = buildList {
                for (index in 0 until value.length()) {
                    val part = value.optJSONObject(index) ?: continue
                    val text = part.optString("text").trim()
                    if (text.isNotEmpty()) add(text)
                }
            }
            if (parts.isEmpty()) null else parts.joinToString("\n")
        }

        else -> null
    }
}
