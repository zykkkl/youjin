package com.youjin.app.data

import androidx.room.withTransaction
import com.youjin.app.data.reminder.ReminderScheduler
import com.youjin.app.domain.Goal
import com.youjin.app.domain.GoalChange
import com.youjin.app.domain.GoalDraft
import com.youjin.app.domain.GoalStatus
import com.youjin.app.domain.GoalStep
import com.youjin.app.domain.GoalType
import com.youjin.app.domain.ProgressCalculator
import com.youjin.app.domain.ProgressEntry
import com.youjin.app.domain.ReminderSpec
import com.youjin.app.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * 目标仓库：唯一写入入口。所有写操作后通过 reconcileCompleted 统一维护
 * “已完成但被修改后不再满足条件 → 恢复进行中”的规则，并记录字段变更留痕。
 */
class GoalRepository(
    private val db: AppDatabase,
    private val goalDao: GoalDao,
    private val entryDao: EntryDao,
    private val stepDao: StepDao,
    private val reminderDao: ReminderDao,
    private val changeDao: ChangeDao,
    private val reminderScheduler: ReminderScheduler,
) {

    // ---------- 观察流 ----------

    val goals: Flow<List<Goal>> =
        combine(goalDao.observeAll(), entryDao.observeAll(), stepDao.observeAll()) { g, e, s ->
            g.map { it.toDomain(e.filter { en -> en.goalId == it.id }, s.filter { st -> st.goalId == it.id }) }
        }

    /** 全部进度记录（跨目标，供首页“今日记录”使用） */
    val allEntries: Flow<List<ProgressEntry>> =
        entryDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** 全部步骤（跨目标，供首页“今日记录”使用） */
    val allSteps: Flow<List<GoalStep>> =
        stepDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getGoal(id: Long): Goal? =
        goalDao.getById(id)?.toDomain(entryDao.getByGoal(id), stepDao.getByGoal(id))

    fun goalFlow(id: Long): Flow<Goal?> =
        combine(goalDao.observeById(id), entryDao.observeAll(), stepDao.observeAll()) { g, e, s ->
            g?.toDomain(e.filter { it.goalId == id }, s.filter { it.goalId == id })
        }

    fun entriesFlow(goalId: Long): Flow<List<ProgressEntry>> =
        entryDao.observeByGoal(goalId).map { list -> list.map { it.toDomain() } }

    fun stepsFlow(goalId: Long): Flow<List<GoalStep>> =
        stepDao.observeByGoal(goalId).map { list -> list.map { it.toDomain() } }

    fun reminderFlow(goalId: Long): Flow<ReminderSpec?> =
        reminderDao.observeByGoal(goalId).map { it?.toDomain() }

    suspend fun getReminder(goalId: Long): ReminderSpec? =
        reminderDao.getByGoal(goalId)?.toDomain()

    suspend fun getSteps(goalId: Long): List<GoalStep> =
        stepDao.getByGoal(goalId).map { it.toDomain() }

    // ---------- 创建 / 编辑 ----------

    suspend fun createGoal(draft: GoalDraft): Long {
        val now = Dates.nowMillis()
        val entity = GoalEntity(
            name = draft.name,
            type = draft.type.name,
            category = draft.category,
            status = GoalStatus.ACTIVE.name,
            unit = draft.unit,
            targetValue = draft.targetValue,
            initialValue = draft.initialValue,
            startDate = draft.startDate,
            deadline = draft.deadline,
            motivation = draft.motivation,
            nextAction = draft.nextAction,
            pinned = draft.pinned,
            pinOrder = if (draft.pinned) goalDao.maxPinOrder() + 1 else 0,
            colorIndex = draft.colorIndex,
            iconIndex = draft.iconIndex,
            createdAt = now,
            updatedAt = now,
            completedAt = null,
        )
        val id = goalDao.insert(entity)
        if (draft.type == GoalType.CHECKLIST) {
            draft.steps.forEachIndexed { index, title ->
                stepDao.insert(
                    GoalStepEntity(
                        goalId = id, title = title.trim(), sortOrder = index,
                        done = false, doneDate = null, updatedAt = now,
                    )
                )
            }
        }
        return id
    }

    suspend fun updateGoal(goalId: Long, draft: GoalDraft) {
        val old = goalDao.getById(goalId) ?: return
        val now = Dates.nowMillis()
        // 目标值与日期调整保留前后值及时间，帮助解释进度变化
        if (old.targetValue != draft.targetValue) {
            logChange(goalId, "目标值", old.targetValue.toString(), draft.targetValue.toString())
        }
        if (old.startDate != draft.startDate) {
            logChange(goalId, "开始日期", old.startDate?.toString() ?: "无", draft.startDate?.toString() ?: "无")
        }
        if (old.deadline != draft.deadline) {
            logChange(goalId, "截止日期", old.deadline?.toString() ?: "无", draft.deadline?.toString() ?: "无")
        }
        val updated = old.copy(
            name = draft.name,
            category = draft.category,
            targetValue = draft.targetValue,
            initialValue = draft.initialValue,
            startDate = draft.startDate,
            deadline = draft.deadline,
            motivation = draft.motivation,
            nextAction = draft.nextAction,
            pinned = draft.pinned,
            pinOrder = if (draft.pinned) {
                if (old.pinned) old.pinOrder else goalDao.maxPinOrder() + 1
            } else 0,
            colorIndex = draft.colorIndex,
            iconIndex = draft.iconIndex,
            updatedAt = now,
        )
        goalDao.update(updated)
        reconcileCompleted(goalId)
    }

    // ---------- 进度记录 ----------

    suspend fun addEntry(goalId: Long, delta: Double, occurDate: Long, note: String?): Long {
        require(delta > 0) { "新增量必须大于 0" }
        require(occurDate <= Dates.todayEpochDay()) { "实际进展不能记在未来" }
        // 规则隔离：步骤清单型目标的进度来自步骤勾选，不允许写入数值记录
        val goal = goalDao.getById(goalId) ?: throw IllegalArgumentException("目标不存在")
        require(goal.type.toGoalType() == GoalType.CUMULATIVE) {
            "步骤清单型目标请通过勾选步骤记录进展"
        }
        val now = Dates.nowMillis()
        val id = entryDao.insert(
            ProgressEntryEntity(
                goalId = goalId, delta = delta, occurDate = occurDate,
                note = note?.takeIf { it.isNotBlank() }, createdAt = now, updatedAt = now,
            )
        )
        reconcileCompleted(goalId)
        return id
    }

    suspend fun updateEntry(entryId: Long, delta: Double, occurDate: Long, note: String?) {
        require(delta > 0) { "新增量必须大于 0" }
        require(occurDate <= Dates.todayEpochDay()) { "实际进展不能记在未来" }
        val old = entryDao.getById(entryId) ?: return
        entryDao.update(
            old.copy(
                delta = delta, occurDate = occurDate,
                note = note?.takeIf { it.isNotBlank() }, updatedAt = Dates.nowMillis(),
            )
        )
        reconcileCompleted(old.goalId)
    }

    suspend fun deleteEntry(entryId: Long) {
        val entry = entryDao.getById(entryId) ?: return
        entryDao.deleteById(entryId)
        reconcileCompleted(entry.goalId)
    }

    // ---------- 步骤（清单型） ----------

    suspend fun addStep(goalId: Long, title: String): Long {
        val now = Dates.nowMillis()
        val order = stepDao.getByGoal(goalId).size
        val id = stepDao.insert(
            GoalStepEntity(
                goalId = goalId, title = title.trim(), sortOrder = order,
                done = false, doneDate = null, updatedAt = now,
            )
        )
        reconcileCompleted(goalId)
        return id
    }

    suspend fun toggleStep(stepId: Long, done: Boolean) {
        val step = stepDao.getById(stepId) ?: return
        val now = Dates.nowMillis()
        stepDao.update(
            step.copy(
                done = done,
                doneDate = if (done) Dates.todayEpochDay() else null,
                updatedAt = now,
            )
        )
        reconcileCompleted(step.goalId)
    }

    suspend fun deleteStep(stepId: Long) {
        val step = stepDao.getById(stepId) ?: return
        stepDao.delete(step)
        reconcileCompleted(step.goalId)
    }

    // ---------- 状态流转 ----------

    suspend fun setStatus(goalId: Long, status: GoalStatus) {
        val goal = goalDao.getById(goalId) ?: return
        if (goal.status == status.name) return
        val now = Dates.nowMillis()
        goalDao.update(
            goal.copy(
                status = status.name,
                completedAt = if (status == GoalStatus.COMPLETED) now else null,
                updatedAt = now,
            )
        )
        logChange(goalId, "状态", goal.status, status.name)
        when (status) {
            GoalStatus.ACTIVE -> {
                // 恢复时按配置重启提醒
                val reminder = reminderDao.getByGoal(goalId)?.toDomain()
                if (reminder != null && reminder.enabled) reminderScheduler.schedule(goalId, reminder)
            }
            else -> reminderScheduler.cancel(goalId) // 暂停/完成/归档停止提醒
        }
    }

    /** 达到终点后由用户确认完成；不满足条件时返回 false */
    suspend fun confirmComplete(goalId: Long): Boolean {
        val goal = goalDao.getById(goalId) ?: return false
        if (goal.status == GoalStatus.COMPLETED.name) return true
        val domain = goal.toDomain(entryDao.getByGoal(goalId), stepDao.getByGoal(goalId))
        if (!domain.reachedTarget()) return false
        setStatus(goalId, GoalStatus.COMPLETED)
        return true
    }

    suspend fun setNextAction(goalId: Long, action: String?) {
        val goal = goalDao.getById(goalId) ?: return
        goalDao.update(
            goal.copy(
                nextAction = action?.takeIf { it.isNotBlank() },
                updatedAt = Dates.nowMillis(),
            )
        )
    }

    suspend fun setPinned(goalId: Long, pinned: Boolean) {
        val goal = goalDao.getById(goalId) ?: return
        val order = if (pinned) goalDao.maxPinOrder() + 1 else 0
        goalDao.update(goal.copy(pinned = pinned, pinOrder = order, updatedAt = Dates.nowMillis()))
    }

    // ---------- 提醒 ----------

    suspend fun saveReminder(goalId: Long, spec: ReminderSpec?) {
        if (spec == null) {
            reminderDao.deleteByGoal(goalId)
            reminderScheduler.cancel(goalId)
            return
        }
        reminderDao.upsert(spec.toEntity())
        val goal = goalDao.getById(goalId)
        if (spec.enabled && goal?.status == GoalStatus.ACTIVE.name) {
            reminderScheduler.schedule(goalId, spec)
        } else {
            reminderScheduler.cancel(goalId)
        }
    }

    // ---------- 删除与撤销 ----------

    data class GoalSnapshot(
        val goal: GoalEntity,
        val entries: List<ProgressEntryEntity>,
        val steps: List<GoalStepEntity>,
        val reminder: ReminderEntity?,
    )

    /** 删除目标（级联删除记录/步骤/提醒），返回快照供撤销 */
    suspend fun deleteGoal(goalId: Long): GoalSnapshot? {
        val goal = goalDao.getById(goalId) ?: return null
        val snapshot = GoalSnapshot(
            goal = goal,
            entries = entryDao.getByGoal(goalId),
            steps = stepDao.getByGoal(goalId),
            reminder = reminderDao.getByGoal(goalId),
        )
        db.withTransaction {
            goalDao.delete(goal)
        }
        reminderScheduler.cancel(goalId)
        return snapshot
    }

    suspend fun undoDelete(snapshot: GoalSnapshot) {
        db.withTransaction {
            goalDao.insert(snapshot.goal)
            snapshot.entries.forEach { entryDao.insert(it) }
            snapshot.steps.forEach { stepDao.insert(it) }
            snapshot.reminder?.let { reminderDao.upsert(it) }
        }
        snapshot.reminder?.let {
            if (it.enabled && snapshot.goal.status == GoalStatus.ACTIVE.name) {
                reminderScheduler.schedule(snapshot.goal.id, it.toDomain())
            }
        }
    }

    // ---------- 内部 ----------

    /** 已完成目标被修改后不再满足条件时，恢复为进行中 */
    private suspend fun reconcileCompleted(goalId: Long) {
        val goal = goalDao.getById(goalId) ?: return
        if (goal.status != GoalStatus.COMPLETED.name) return
        val domain = goal.toDomain(entryDao.getByGoal(goalId), stepDao.getByGoal(goalId))
        if (domain.reachedTarget()) return
        goalDao.update(
            goal.copy(
                status = GoalStatus.ACTIVE.name,
                completedAt = null,
                updatedAt = Dates.nowMillis(),
            )
        )
        logChange(goalId, "状态", GoalStatus.COMPLETED.name, GoalStatus.ACTIVE.name)
    }

    private suspend fun logChange(goalId: Long, field: String, oldValue: String, newValue: String) {
        changeDao.insert(
            GoalChangeEntity(
                goalId = goalId, field = field, oldValue = oldValue,
                newValue = newValue, changedAt = Dates.nowMillis(),
            )
        )
    }
}
