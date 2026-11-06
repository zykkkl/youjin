package com.youjin.app

import com.youjin.app.data.ai.AiPrompts
import com.youjin.app.data.ai.AiProviders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptsTest {

    // ---- Base URL 规范化 ----

    @Test
    fun normalizeBaseUrl_removesTrailingSlash() {
        assertEquals(
            "https://api.openai.com/v1",
            AiProviders.normalizeBaseUrl("https://api.openai.com/v1/"),
        )
    }

    @Test
    fun normalizeBaseUrl_trimsSpacesAndFullChatPath() {
        assertEquals(
            "https://api.openai.com/v1",
            AiProviders.normalizeBaseUrl("  https://api.openai.com/v1/chat/completions  "),
        )
    }

    @Test
    fun endpoints_areBuiltFromNormalizedBaseUrl() {
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            AiProviders.chatEndpoint("https://api.deepseek.com/v1/"),
        )
        assertEquals(
            "https://api.deepseek.com/v1/models",
            AiProviders.modelsEndpoint("https://api.deepseek.com/v1"),
        )
    }

    // ---- 拆解结果解析 ----

    @Test
    fun parsePlan_parsesJsonWithCodeFence() {
        val raw = """
            ```json
            {"steps":["列出每周可用的三个时间段","选一个时间段做第一次训练"],"nextAction":"今晚定好第一次训练的时间"}
            ```
        """.trimIndent()

        val plan = AiPrompts.parsePlan(raw)

        assertEquals(listOf("列出每周可用的三个时间段", "选一个时间段做第一次训练"), plan.steps)
        assertEquals("今晚定好第一次训练的时间", plan.nextAction)
    }

    @Test
    fun parsePlan_fallsBackToLinesAndExtractsNextAction() {
        val raw = """
            - 整理现有资料
            * 2. 确定本周要完成的两件事
            下一步：今晚整理目标清单
        """.trimIndent()

        val plan = AiPrompts.parsePlan(raw)

        assertEquals(listOf("整理现有资料", "确定本周要完成的两件事"), plan.steps)
        assertEquals("今晚整理目标清单", plan.nextAction)
    }

    @Test
    fun parsePlan_limitsStepsToEight() {
        val steps = (1..10).joinToString(",") { "\"步骤$it\"" }
        val raw = """{"steps":[$steps],"nextAction":"先做第一步"}"""

        val plan = AiPrompts.parsePlan(raw)

        assertEquals(8, plan.steps.size)
        assertEquals("先做第一步", plan.nextAction)
    }

    @Test
    fun parsePlan_returnsEmptySuggestionForBlankInput() {
        val plan = AiPrompts.parsePlan("   ")

        assertTrue(plan.steps.isEmpty())
        assertNull(plan.nextAction)
    }
}
