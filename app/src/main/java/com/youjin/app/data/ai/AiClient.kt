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

            val text = request(
                method = "POST",
                url = AiProviders.chatEndpoint(config.baseUrl),
                body = body,
                apiKey = config.apiKey,
            )
            val json = JSONObject(text)
            val content = json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
            if (content.isNullOrBlank()) {
                val error = json.optJSONObject("error")?.optString("message").orEmpty()
                AiResult.Failure(if (error.isNotBlank()) error else "服务返回了空内容，请检查模型名或稍后重试")
            } else {
                AiResult.Ok(content)
            }
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
                JSONObject(error.body).optJSONObject("error")?.optString("message").orEmpty()
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
