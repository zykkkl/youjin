package com.youjin.app.domain

import kotlin.math.roundToInt

/**
 * 周计划事项。属于某一周（weekStart 为周一 epochDay），
 * 用 importance / urgency（1..10）落在四象限图上。
 */
data class PlanItem(
    val id: Long,
    val weekStart: Long,
    val title: String,
    val goalId: Long?,
    val importance: Int,
    val urgency: Int,
    val done: Boolean,
    val doneAt: Long?,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val quadrant: PlanQuadrant get() = quadrantOf(importance, urgency)
}

/** 四象限：先看重要性，再看紧急性 */
enum class PlanQuadrant(val title: String, val advice: String) {
    DO_NOW("重要且紧急", "立即做"),
    SCHEDULE("重要不紧急", "排期做"),
    DELEGATE("紧急不重要", "尽快处理或委托"),
    DROP("不重要也不紧急", "先不做，或删掉"),
}

/** 分值 1..10，>= 6 视为「高」 */
fun quadrantOf(importance: Int, urgency: Int): PlanQuadrant = when {
    importance >= 6 && urgency >= 6 -> PlanQuadrant.DO_NOW
    importance >= 6 -> PlanQuadrant.SCHEDULE
    urgency >= 6 -> PlanQuadrant.DELEGATE
    else -> PlanQuadrant.DROP
}

/** 1..10 映射到 0f..1f，用于把点画在坐标轴里 */
fun planFraction(value: Int): Float = ((value - 1).coerceIn(0, 9)) / 9f

/** 0f..1f 反解回 1..10（四舍五入） */
fun planValue(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * 9f).roundToInt() + 1

/** 拖动时把像素位移换算成新的分值；axisPx 为该轴总长度 */
fun valueFromDrag(startValue: Int, dragPx: Float, axisPx: Float): Int =
    if (axisPx <= 0f) startValue
    else planValue(planFraction(startValue) + dragPx / axisPx)
