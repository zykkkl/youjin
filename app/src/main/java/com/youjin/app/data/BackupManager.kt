package com.youjin.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.youjin.app.data.reminder.ReminderScheduler
import com.youjin.app.domain.GoalStatus
import com.youjin.app.util.Dates
import com.youjin.app.util.formatNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 备份管理：
 * - JSON 用于完整备份恢复（含格式版本、全量数据与必要配置）
 * - CSV 用于查看与分析（进度记录流水）
 * - 恢复采用全量替换：先校验、保留当前快照、事务性替换、再重新注册有效提醒
 * 通过 Storage Access Framework 读写，无需存储权限。
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val reminderScheduler: ReminderScheduler,
) {

    data class ImportResult(
        val goals: Int,
        val entries: Int,
        val steps: Int,
        val reminders: Int,
        val reviews: Int,
        val changes: Int,
        val droppedOrphans: Int,
        val snapshotPath: String?,
    )

    private val goalDao get() = db.goalDao()
    private val entryDao get() = db.entryDao()
    private val stepDao get() = db.stepDao()
    private val reminderDao get() = db.reminderDao()
    private val reviewDao get() = db.reviewDao()
    private val changeDao get() = db.changeDao()

    // ---------- 导出 ----------

    suspend fun exportJson(uri: Uri) {
        val json = buildJson()
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toString().toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("无法写入所选位置")
        }
    }

    suspend fun exportCsv(uri: Uri) {
        val goals = goalDao.getAll().associateBy { it.id }
        val entries = entryDao.getAll().sortedBy { it.occurDate }
        val sb = StringBuilder()
        sb.append("目标,单位,发生日期,新增量,备注,记录时间\r\n")
        entries.forEach { e ->
            val g = goals[e.goalId]
            sb.append(csv(g?.name ?: "(目标已删除)"))
                .append(',').append(csv(g?.unit ?: ""))
                .append(',').append(csv(Dates.formatDayFull(e.occurDate)))
                .append(',').append(csv(formatNumber(e.delta)))
                .append(',').append(csv(e.note ?: ""))
                .append(',').append(csv(Dates.formatClock(e.createdAt)))
                .append("\r\n")
        }
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("无法写入所选位置")
        }
    }

    // ---------- 导入 ----------

    suspend fun importJson(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IllegalStateException("无法读取所选文件")

        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalStateException("文件不是有效的 JSON：${e.message}")
        }
        if (root.optString("format") != FORMAT) {
            throw IllegalStateException("不是「有进」的备份文件（格式标识不匹配）")
        }
        val version = root.optInt("version", -1)
        if (version != VERSION) throw IllegalStateException("备份版本不受支持：$version")
        if (!root.has("goals")) throw IllegalStateException("备份文件缺少目标数据")

        val goals = parseGoals(root.optJSONArray("goals"))
        val goalIds = goals.map { it.id }.toSet()
        var dropped = 0

        val entries = parseEntries(root.optJSONArray("entries"))
            .filter { e -> if (e.goalId in goalIds && e.delta > 0) true else { dropped++; false } }
        val steps = parseSteps(root.optJSONArray("steps"))
            .filter { s -> if (s.goalId in goalIds) true else { dropped++; false } }
        val reminders = parseReminders(root.optJSONArray("reminders"))
            .filter { r -> if (r.goalId in goalIds) true else { dropped++; false } }
        val reviews = parseReviews(root.optJSONArray("reviews"))
        val changes = parseChanges(root.optJSONArray("changes"))
            .filter { c -> if (c.goalId in goalIds) true else { dropped++; false } }

        // 保留当前快照，替换失败可恢复
        val snapshotPath = snapshotCurrent()

        db.withTransaction {
            entryDao.deleteAll()
            stepDao.deleteAll()
            reminderDao.deleteAll()
            changeDao.deleteAll()
            reviewDao.deleteAll()
            goalDao.deleteAll()
            goals.forEach { goalDao.insert(it) }
            entries.forEach { entryDao.insert(it) }
            steps.forEach { stepDao.insert(it) }
            reminders.forEach { reminderDao.upsert(it) }
            reviews.forEach { reviewDao.upsert(it) }
            changes.forEach { changeDao.insert(it) }
        }

        // 重新注册有效提醒（目标需存在且为进行中）
        val activeIds = goals.filter { it.status == GoalStatus.ACTIVE.name }.map { it.id }.toSet()
        reminderDao.getAll().forEach { r ->
            if (r.enabled && r.goalId in activeIds) {
                reminderScheduler.schedule(r.goalId, r.toDomain())
            } else {
                reminderScheduler.cancel(r.goalId)
            }
        }

        ImportResult(
            goals = goals.size, entries = entries.size, steps = steps.size,
            reminders = reminders.size, reviews = reviews.size, changes = changes.size,
            droppedOrphans = dropped, snapshotPath = snapshotPath,
        )
    }

    // ---------- 内部 ----------

    private suspend fun snapshotCurrent(): String? {
        return try {
            val file = File(context.cacheDir, "snapshot-before-restore-${System.currentTimeMillis()}.json")
            file.writeText(buildJson().toString(), Charsets.UTF_8)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun buildJson(): JSONObject {
        val goals = goalDao.getAll()
        val entries = entryDao.getAll()
        val steps = stepDao.getAll()
        val reminders = reminderDao.getAll()
        val reviews = reviewDao.getAll()
        val changes = changeDao.getAll()

        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("exportedAt", Dates.nowMillis())
            put("goals", JSONArray().apply { goals.forEach { put(goalToJson(it)) } })
            put("entries", JSONArray().apply { entries.forEach { put(entryToJson(it)) } })
            put("steps", JSONArray().apply { steps.forEach { put(stepToJson(it)) } })
            put("reminders", JSONArray().apply { reminders.forEach { put(reminderToJson(it)) } })
            put("reviews", JSONArray().apply { reviews.forEach { put(reviewToJson(it)) } })
            put("changes", JSONArray().apply { changes.forEach { put(changeToJson(it)) } })
        }
    }

    private fun goalToJson(g: GoalEntity) = JSONObject().apply {
        put("id", g.id); put("name", g.name); put("type", g.type)
        putOpt("category", g.category); put("status", g.status); put("unit", g.unit)
        put("targetValue", g.targetValue); put("initialValue", g.initialValue)
        putOpt("startDate", g.startDate); putOpt("deadline", g.deadline)
        putOpt("motivation", g.motivation); putOpt("nextAction", g.nextAction)
        put("pinned", g.pinned); put("pinOrder", g.pinOrder)
        put("colorIndex", g.colorIndex); put("iconIndex", g.iconIndex)
        put("createdAt", g.createdAt); put("updatedAt", g.updatedAt)
        putOpt("completedAt", g.completedAt)
    }

    private fun entryToJson(e: ProgressEntryEntity) = JSONObject().apply {
        put("id", e.id); put("goalId", e.goalId); put("delta", e.delta)
        put("occurDate", e.occurDate); putOpt("note", e.note)
        put("createdAt", e.createdAt); put("updatedAt", e.updatedAt)
    }

    private fun stepToJson(s: GoalStepEntity) = JSONObject().apply {
        put("id", s.id); put("goalId", s.goalId); put("title", s.title)
        put("sortOrder", s.sortOrder); put("done", s.done)
        putOpt("doneDate", s.doneDate); put("updatedAt", s.updatedAt)
    }

    private fun reminderToJson(r: ReminderEntity) = JSONObject().apply {
        put("id", r.id); put("goalId", r.goalId); put("enabled", r.enabled)
        put("weekdaysMask", r.weekdaysMask); put("hour", r.hour); put("minute", r.minute)
    }

    private fun reviewToJson(r: WeeklyReviewEntity) = JSONObject().apply {
        put("id", r.id); put("weekStart", r.weekStart); put("body", r.body)
        put("createdAt", r.createdAt); put("updatedAt", r.updatedAt)
    }

    private fun changeToJson(c: GoalChangeEntity) = JSONObject().apply {
        put("id", c.id); put("goalId", c.goalId); put("field", c.field)
        put("oldValue", c.oldValue); put("newValue", c.newValue); put("changedAt", c.changedAt)
    }

    private fun parseGoals(array: JSONArray?): List<GoalEntity> {
        if (array == null) throw IllegalStateException("备份文件缺少目标数据")
        val out = mutableListOf<GoalEntity>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val id = o.optLong("id", 0)
            val name = o.optString("name", "").trim()
            if (id <= 0 || name.isEmpty()) throw IllegalStateException("备份中存在无效的目标数据")
            out += GoalEntity(
                id = id,
                name = name,
                type = o.optString("type", "CUMULATIVE"),
                category = o.optNullableString("category"),
                status = o.optString("status", "ACTIVE"),
                unit = o.optString("unit", ""),
                targetValue = o.optDouble("targetValue", 0.0),
                initialValue = o.optDouble("initialValue", 0.0),
                startDate = o.optNullableLong("startDate"),
                deadline = o.optNullableLong("deadline"),
                motivation = o.optNullableString("motivation"),
                nextAction = o.optNullableString("nextAction"),
                pinned = o.optBoolean("pinned", false),
                pinOrder = o.optInt("pinOrder", 0),
                colorIndex = o.optInt("colorIndex", 0),
                iconIndex = o.optInt("iconIndex", 0),
                createdAt = o.optLong("createdAt", Dates.nowMillis()),
                updatedAt = o.optLong("updatedAt", Dates.nowMillis()),
                completedAt = o.optNullableLong("completedAt"),
            )
        }
        return out
    }

    private fun parseEntries(array: JSONArray?): List<ProgressEntryEntity> {
        val out = mutableListOf<ProgressEntryEntity>()
        if (array == null) return out
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            out += ProgressEntryEntity(
                id = o.optLong("id", 0),
                goalId = o.optLong("goalId", 0),
                delta = o.optDouble("delta", 0.0),
                occurDate = o.optLong("occurDate", 0),
                note = o.optNullableString("note"),
                createdAt = o.optLong("createdAt", Dates.nowMillis()),
                updatedAt = o.optLong("updatedAt", Dates.nowMillis()),
            )
        }
        return out
    }

    private fun parseSteps(array: JSONArray?): List<GoalStepEntity> {
        val out = mutableListOf<GoalStepEntity>()
        if (array == null) return out
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            out += GoalStepEntity(
                id = o.optLong("id", 0),
                goalId = o.optLong("goalId", 0),
                title = o.optString("title", "").trim(),
                sortOrder = o.optInt("sortOrder", 0),
                done = o.optBoolean("done", false),
                doneDate = o.optNullableLong("doneDate"),
                updatedAt = o.optLong("updatedAt", Dates.nowMillis()),
            ).takeIf { it.title.isNotEmpty() } ?: continue
        }
        return out
    }

    private fun parseReminders(array: JSONArray?): List<ReminderEntity> {
        val out = mutableListOf<ReminderEntity>()
        if (array == null) return out
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            out += ReminderEntity(
                id = o.optLong("id", 0),
                goalId = o.optLong("goalId", 0),
                enabled = o.optBoolean("enabled", false),
                weekdaysMask = o.optInt("weekdaysMask", ReminderSpec_ALL_DAYS),
                hour = o.optInt("hour", 9),
                minute = o.optInt("minute", 0),
            )
        }
        return out
    }

    private fun parseReviews(array: JSONArray?): List<WeeklyReviewEntity> {
        val out = mutableListOf<WeeklyReviewEntity>()
        if (array == null) return out
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            out += WeeklyReviewEntity(
                id = o.optLong("id", 0),
                weekStart = o.optLong("weekStart", 0),
                body = o.optString("body", ""),
                createdAt = o.optLong("createdAt", Dates.nowMillis()),
                updatedAt = o.optLong("updatedAt", Dates.nowMillis()),
            )
        }
        return out
    }

    private fun parseChanges(array: JSONArray?): List<GoalChangeEntity> {
        val out = mutableListOf<GoalChangeEntity>()
        if (array == null) return out
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            out += GoalChangeEntity(
                id = o.optLong("id", 0),
                goalId = o.optLong("goalId", 0),
                field = o.optString("field", ""),
                oldValue = o.optString("oldValue", ""),
                newValue = o.optString("newValue", ""),
                changedAt = o.optLong("changedAt", Dates.nowMillis()),
            )
        }
        return out
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (isNull(key)) null else optLong(key, 0)

    private fun csv(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""

    companion object {
        const val FORMAT = "youjin-backup"
        const val VERSION = 1
        const val ReminderSpec_ALL_DAYS = 0b11111110
    }
}
