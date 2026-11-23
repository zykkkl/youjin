package com.youjin.app.data

import com.youjin.app.domain.PlanItem
import com.youjin.app.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 周计划：每周一组事项，勾选完成、按重要性/紧急性定位。
 * 事项可以关联目标（可选），删除目标时 goalId 会被置空。
 */
class PlanRepository(private val dao: PlanItemDao) {

    fun observeWeek(weekStart: Long): Flow<List<PlanItem>> =
        dao.observeByWeek(weekStart).map { list -> list.map { it.toDomain() } }

    suspend fun countDone(weekStart: Long): Int = dao.countDone(weekStart)

    suspend fun add(
        weekStart: Long,
        title: String,
        goalId: Long? = null,
        importance: Int = 5,
        urgency: Int = 5,
    ): Long {
        val name = title.trim()
        require(name.isNotEmpty()) { "计划内容不能为空" }
        val now = Dates.nowMillis()
        return dao.insert(
            PlanItemEntity(
                weekStart = weekStart,
                title = name.take(120),
                goalId = goalId,
                importance = importance.coerceIn(1, 10),
                urgency = urgency.coerceIn(1, 10),
                done = false,
                doneAt = null,
                sortOrder = 0,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun update(
        id: Long,
        title: String,
        goalId: Long?,
        importance: Int,
        urgency: Int,
    ) {
        val current = dao.getById(id) ?: return
        val name = title.trim()
        require(name.isNotEmpty()) { "计划内容不能为空" }
        dao.update(
            current.copy(
                title = name.take(120),
                goalId = goalId,
                importance = importance.coerceIn(1, 10),
                urgency = urgency.coerceIn(1, 10),
                updatedAt = Dates.nowMillis(),
            )
        )
    }

    suspend fun setDone(id: Long, done: Boolean) {
        val current = dao.getById(id) ?: return
        dao.update(
            current.copy(
                done = done,
                doneAt = if (done) Dates.nowMillis() else null,
                updatedAt = Dates.nowMillis(),
            )
        )
    }

    suspend fun move(id: Long, importance: Int, urgency: Int) {
        val current = dao.getById(id) ?: return
        dao.update(
            current.copy(
                importance = importance.coerceIn(1, 10),
                urgency = urgency.coerceIn(1, 10),
                updatedAt = Dates.nowMillis(),
            )
        )
    }

    suspend fun delete(id: Long) = dao.deleteById(id)
}
