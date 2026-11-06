package com.youjin.app

import com.youjin.app.data.ai.AiResponse
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 覆盖各家「OpenAI 兼容」响应的差异：标准结构、数组内容、旧版 MiniMax、推理模型、错误结构。 */
class AiResponseTest {

    @Test
    fun parsesStandardContent() {
        val json = JSONObject(
            """{"choices":[{"message":{"role":"assistant","content":"  hello  "},"finish_reason":"stop"}]}""",
        )
        assertEquals("hello", AiResponse.content(json))
        assertEquals("stop", AiResponse.finishReason(json))
    }

    @Test
    fun parsesContentArray() {
        val json = JSONObject(
            """{"choices":[{"message":{"content":[{"type":"text","text":"第一段"},{"type":"text","text":"第二段"}]}}]}""",
        )
        assertEquals("第一段\n第二段", AiResponse.content(json))
    }

    @Test
    fun parsesLegacyMinimaxMessages() {
        val json = JSONObject(
            """{"choices":[{"messages":[{"sender_type":"BOT","text":"旧版回复"}]}]}""",
        )
        assertEquals("旧版回复", AiResponse.content(json))
    }

    @Test
    fun readsLegacyCompletionText() {
        val json = JSONObject("""{"choices":[{"text":"completion 结构"}]}""")
        assertEquals("completion 结构", AiResponse.content(json))
    }

    @Test
    fun reportsProviderErrorFromBaseResp() {
        val json = JSONObject("""{"base_resp":{"status_code":1004,"status_msg":"invalid api key"},"choices":[]}""")
        assertEquals("invalid api key", AiResponse.providerError(json))
        assertNull(AiResponse.content(json))
    }

    @Test
    fun explainsReasoningOnlyResponse() {
        val json = JSONObject(
            """{"choices":[{"message":{"content":"","reasoning_content":"先想想"},"finish_reason":"length"}]}""",
        )
        assertNull(AiResponse.content(json))
        assertTrue(AiResponse.hasReasoningOnly(json))
        assertTrue(AiResponse.emptyMessage(json).contains("推理"))
    }

    @Test
    fun explainsTruncatedEmptyResponse() {
        val json = JSONObject("""{"choices":[{"message":{"content":""},"finish_reason":"length"}]}""")
        assertFalse(AiResponse.hasReasoningOnly(json))
        assertTrue(AiResponse.emptyMessage(json).contains("length"))
    }

    @Test
    fun emptyMessageIncludesSnippet() {
        val json = JSONObject("""{"id":"x","choices":[]}""")
        val message = AiResponse.emptyMessage(json)
        assertTrue(message.contains("空内容"))
        assertTrue(message.contains("choices"))
    }
}
