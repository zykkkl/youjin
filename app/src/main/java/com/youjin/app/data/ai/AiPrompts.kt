package com.youjin.app.data.ai

import com.youjin.app.data.ReviewRepository
import com.youjin.app.domain.GoalType
import com.youjin.app.util.Dates
import com.youjin.app.util.formatNumber
import org.json.JSONObject

/**
 * 提示词与 AI 输出的解析。
 *
 * 全部是纯函数，便于单元测试；原则：
 * - 只让 AI 处理「建议」与「文字草稿」，不碰记录数据；
 * - 拆解结果必须落回现有的 GoalStep / nextAction，不引入新实体；
 * - 复盘只按事实描述，不做评价与命令。
 */
object AiPrompts {

    private const val MAX_STEPS = 8

    fun decomposeSystem(): String = """
        你是一个目标拆解助手，服务于一款个人目标记录 App。
        你的任务：把用户的目标拆成具体、可执行的步骤，并给出一句「下一步行动」。

        要求：
        1. 步骤必须具体：能在一到两次行动内完成，或在几天内验证；禁止「坚持努力」「保持自律」这类空话。
        2. 步骤之间不重复，数量 3 到 8 个。
        3. 不承诺结果，不做医疗、健康或投资建议。
        4. 只输出 JSON，不要任何解释、标题或 Markdown 代码块。
        5. JSON 格式固定：{"steps":["步骤1","步骤2"],"nextAction":"一句具体的下一步"}
    """.trimIndent()

    fun decomposeUser(request: DecomposeRequest, todayEpochDay: Long): String = buildString {
        appendLine("今天：${Dates.formatDayFull(todayEpochDay)}")
        appendLine("目标名称：${request.name}")
        when (request.type) {
            GoalType.CUMULATIVE -> {
                appendLine("目标类型：累计数值")
                appendLine("单位：${request.unit.ifBlank { "（未填写）" }}")
                appendLine("目标值：${formatNumber(request.targetValue)} ${request.unit}")
            }

            GoalType.CHECKLIST -> appendLine("目标类型：步骤清单（需要可勾选的步骤）")
        }
        request.deadlineEpochDay?.let { appendLine("截止日期：${Dates.formatDayFull(it)}") }
        if (request.motivation.isNotBlank()) appendLine("动机：${request.motivation.trim().take(300)}")
        append("请给出拆解结果。")
    }

    fun reviewSystem(): String = """
        你是目标记录 App 的周复盘助手。用户会给你一周的事实数据，你写一段中文复盘草稿。

        要求：
        1. 只陈述数据里的事实，不评价用户勤奋与否，不使用「你应该」「你必须」「太棒了」这类评价或命令。
        2. 数据里没有的内容不要编造；某目标本周没有记录，就如实说没有记录。
        3. 结构：本周发生了什么 → 数据里能看出的一个规律（没有就跳过）→ 下周可以尝试的一个具体调整。
        4. 3 到 5 句话，不超过 150 字，直接输出正文，不要标题、不要列表符号、不要 Markdown。
    """.trimIndent()

    fun reviewUser(
        weekStart: Long,
        stats: ReviewRepository.WeekStats,
        existingNote: String?,
    ): String = buildString {
        appendLine("本周范围：${Dates.formatDay(weekStart)} 至 ${Dates.formatDay(weekStart + 6)}")
        appendLine("数据：")
        if (stats.goals.isEmpty()) {
            appendLine("- 本周没有进行中的目标")
        } else {
            stats.goals.forEach { goal ->
                appendLine(weekLine(goal))
            }
        }
        if (!existingNote.isNullOrBlank()) {
            appendLine()
            appendLine("用户草稿（可以参考，但不要照抄）：")
            appendLine(existingNote.trim().take(400))
        }
        append("请写一段复盘草稿。")
    }

    private fun weekLine(goal: ReviewRepository.GoalWeekStat): String = when (goal.type) {
        GoalType.CUMULATIVE -> if (goal.entryCount > 0) {
            "- ${goal.name}：本周新增 ${formatNumber(goal.delta)} ${goal.unit}，记录 ${goal.entryCount} 次"
        } else {
            "- ${goal.name}：本周没有新增记录"
        }

        GoalType.CHECKLIST -> if (goal.stepsDone > 0) {
            "- ${goal.name}：本周完成 ${goal.stepsDone} 个步骤"
        } else {
            "- ${goal.name}：本周没有完成步骤"
        }
    }

    /** 解析 AI 输出：优先按 JSON 解析；失败则按行提取，最终兜底为空建议 */
    fun parsePlan(raw: String): PlanSuggestion {
        val cleaned = raw.trim().removeCodeFence()
        parseJsonPlan(cleaned)?.let { plan ->
            if (plan.steps.isNotEmpty() || plan.nextAction != null) return plan
        }
        return parseLinePlan(cleaned)
    }

    private fun parseJsonPlan(text: String): PlanSuggestion? = runCatching {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "no json object" }
        val json = JSONObject(text.substring(start, end + 1))
        val steps = mutableListOf<String>()
        json.optJSONArray("steps")?.let { array ->
            for (index in 0 until array.length()) {
                val step = array.optString(index).trim()
                if (step.isNotEmpty()) steps.add(step)
            }
        }
        val next = json.optString("nextAction").trim().takeIf { it.isNotEmpty() }
        PlanSuggestion(steps = steps.take(MAX_STEPS), nextAction = next)
    }.getOrNull()

    /** 非 JSON 兜底：按行提取，去掉列表符号与序号；「下一步」行作为行动 */
    private fun parseLinePlan(text: String): PlanSuggestion {
        val steps = mutableListOf<String>()
        var next: String? = null
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val stripped = line.trimStart('-', '*', '•', '·', ' ', '\t')
                val body = stripped.replaceFirst(Regex("^\\d+[.、)）]\\s*"), "").trim()
                if (body.isEmpty()) return@forEach
                val action = when {
                    body.startsWith("下一步") -> body.substringAfter("：", "").ifBlank {
                        body.substringAfter(":", "")
                    }.trim()

                    body.startsWith("nextAction", ignoreCase = true) ->
                        body.substringAfter(":", "").ifBlank { body.substringAfter("：", "") }.trim()

                    else -> null
                }
                if (action != null) {
                    if (action.isNotEmpty()) next = action
                } else if (!body.startsWith("{") && !body.startsWith("}") && steps.size < MAX_STEPS) {
                    steps.add(body.trim('"', '\'', ' '))
                }
            }
        return PlanSuggestion(steps = steps.filter { it.isNotBlank() }, nextAction = next)
    }

    private fun String.removeCodeFence(): String = removePrefix("```json")
        .removePrefix("```JSON")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
}
