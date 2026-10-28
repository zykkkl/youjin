package com.youjin.app.ui.goals

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youjin.app.domain.Goal
import com.youjin.app.domain.GoalStatus
import com.youjin.app.domain.GoalStep
import com.youjin.app.domain.GoalType
import com.youjin.app.domain.ProgressCalculator
import com.youjin.app.domain.ProgressEntry
import com.youjin.app.domain.ReminderSpec
import com.youjin.app.ui.components.AppleButton
import com.youjin.app.ui.components.AppleCard
import com.youjin.app.ui.components.AppleCheckCircle
import com.youjin.app.ui.components.AppleChevron
import com.youjin.app.ui.components.AppleDialog
import com.youjin.app.ui.components.AppleFloatingButton
import com.youjin.app.ui.components.AppleGroup
import com.youjin.app.ui.components.AppleIconButton
import com.youjin.app.ui.components.AppleIconTile
import com.youjin.app.ui.components.AppleNavBar
import com.youjin.app.ui.components.AppleRow
import com.youjin.app.ui.components.AppleSectionHeader
import com.youjin.app.ui.components.AppleSwitch
import com.youjin.app.ui.components.AppleTextField
import com.youjin.app.ui.components.ConfirmDialog
import com.youjin.app.ui.components.Hairline
import com.youjin.app.ui.components.LocalSnackbarHost
import com.youjin.app.ui.components.ReminderDialog
import com.youjin.app.ui.components.StatusBadge
import com.youjin.app.ui.components.YoujinProgressBar
import com.youjin.app.ui.components.appleRowClick
import com.youjin.app.ui.components.goalColor
import com.youjin.app.ui.components.showLifoUndoSnackbar
import com.youjin.app.ui.record.RecordSheet
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.util.Dates
import com.youjin.app.util.formatNumber
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    vm: GoalDetailViewModel,
    onBack: () -> Unit,
    onEditGoal: (Long) -> Unit,
) {
    val goal by vm.goal.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val steps by vm.steps.collectAsStateWithLifecycle()
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = AppleTheme.colors

    var showRecord by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<ProgressEntry?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showNextActionDialog by remember { mutableStateOf(false) }
    var showDeleteGoal by remember { mutableStateOf(false) }
    var showCalcBasis by remember { mutableStateOf(false) }
    var newStepText by remember { mutableStateOf("") }
    var deleteStepTarget by remember { mutableStateOf<GoalStep?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                snackbar.showSnackbar("通知权限未开启，提醒不会显示；可在系统设置中开启")
            }
        }
    }

    val current = goal
    if (current == null) {
        Column(Modifier.fillMaxSize()) {
            AppleNavBar(title = "目标详情", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "目标不存在或已删除",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.secondaryLabel,
                )
            }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AppleNavBar(
                title = current.name,
                onBack = onBack,
                actions = {
                    AppleIconButton(
                        icon = Icons.Default.Edit,
                        contentDescription = "编辑目标",
                        onClick = { onEditGoal(current.id) },
                        tint = colors.blue,
                    )
                    Box {
                        AppleIconButton(
                            icon = Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            onClick = { menuOpen = true },
                            tint = colors.label,
                        )
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = colors.cardElevated,
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (current.pinned) "取消置顶" else "置顶到首页",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    vm.togglePin()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "删除目标",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.red,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    showDeleteGoal = true
                                },
                            )
                        }
                    }
                },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { DetailHeader(current) }

                if (current.reachedTarget() && current.status == GoalStatus.ACTIVE) {
                    item {
                        AppleCard {
                            Row {
                                Text("🎉", fontSize = 22.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("目标值已达成", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.padding(3.dp))
                                    Text(
                                        "确认后目标将标记为已完成，提醒会停止。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.secondaryLabel,
                                    )
                                    Spacer(Modifier.padding(8.dp))
                                    AppleButton(
                                        text = "确认完成",
                                        onClick = {
                                            vm.confirmComplete()
                                            scope.launch {
                                                snackbar.showSnackbar("恭喜完成「${current.name}」")
                                            }
                                        },
                                        modifier = Modifier.width(132.dp),
                                        fillWidth = false,
                                        height = 42.dp,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    NextActionCard(
                        nextAction = current.nextAction,
                        onEdit = { showNextActionDialog = true },
                    )
                }

                if (current.type == GoalType.CHECKLIST) {
                    item {
                        AppleSectionHeader(
                            "步骤清单",
                            Modifier.padding(top = 12.dp, bottom = 0.dp),
                        )
                    }
                    if (steps.isEmpty()) {
                        item {
                            Text(
                                "还没有步骤，添加一个开始吧",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.secondaryLabel,
                            )
                        }
                    } else {
                        item {
                            AppleGroup {
                                steps.forEachIndexed { index, step ->
                                    StepRow(
                                        step = step,
                                        onToggle = {
                                            val newDone = !step.done
                                            vm.toggleStep(step.id, newDone)
                                            // 栈式撤销：勾选/取消后立即提示，撤销作用于本次操作
                                            scope.showLifoUndoSnackbar(
                                                host = snackbar,
                                                message = if (newDone) {
                                                    "已勾选「${step.title}」"
                                                } else {
                                                    "已取消「${step.title}」"
                                                },
                                            ) {
                                                vm.toggleStep(step.id, !newDone)
                                            }
                                        },
                                        onDelete = { deleteStepTarget = step },
                                    )
                                    if (index < steps.lastIndex) Hairline(startIndent = 56.dp)
                                }
                            }
                        }
                    }
                    item {
                        AddStepRow(
                            text = newStepText,
                            onChange = { newStepText = it },
                            onAdd = {
                                vm.addStep(newStepText)
                                newStepText = ""
                            },
                        )
                    }
                    item {
                        Text(
                            "添加或删除步骤后完成比例会重新计算",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.tertiaryLabel,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                item {
                    ReminderCard(
                        reminder = reminder,
                        onToggle = { want ->
                            if (want) {
                                if (Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                    != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                vm.saveReminder(
                                    reminder?.copy(enabled = true)
                                        ?: ReminderSpec(0, current.id, true, ReminderSpec.ALL_DAYS_MASK, 9, 0)
                                )
                            } else {
                                vm.saveReminder(reminder?.copy(enabled = false))
                            }
                        },
                        onEdit = { showReminderDialog = true },
                    )
                }

                // 记录历史仅适用于累计数值型；步骤清单型以步骤勾选为准
                if (current.type == GoalType.CUMULATIVE) {
                    item {
                        AppleSectionHeader(
                            "记录历史",
                            Modifier.padding(top = 12.dp, bottom = 0.dp),
                        )
                    }
                    if (entries.isEmpty()) {
                        item {
                            Text(
                                "还没有记录，点击右下角按钮开始",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.secondaryLabel,
                            )
                        }
                    } else {
                        item {
                            AppleGroup {
                                entries.forEachIndexed { index, entry ->
                                    EntryRow(
                                        goal = current,
                                        entry = entry,
                                        onClick = { editEntry = entry },
                                    )
                                    if (index < entries.lastIndex) Hairline(startIndent = 18.dp)
                                }
                            }
                        }
                    }
                }

                if (current.type == GoalType.CUMULATIVE) {
                    item {
                        CalculationBasis(
                            goal = current,
                            entries = entries,
                            expanded = showCalcBasis,
                            onToggle = { showCalcBasis = !showCalcBasis },
                        )
                    }
                }

                item {
                    ManageSection(
                        goal = current,
                        onSetStatus = { status ->
                            vm.setStatus(status)
                            scope.launch {
                                snackbar.showSnackbar(
                                    when (status) {
                                        GoalStatus.PAUSED -> "已暂停，提醒已停止"
                                        GoalStatus.ACTIVE -> "已恢复进行中"
                                        GoalStatus.ARCHIVED -> "已归档，可随时恢复"
                                        GoalStatus.COMPLETED -> "已完成 🎉"
                                    }
                                )
                            }
                        },
                        onDelete = { showDeleteGoal = true },
                    )
                }
            }
        }

        if (current.type == GoalType.CUMULATIVE && current.status == GoalStatus.ACTIVE) {
            AppleFloatingButton(
                onClick = { showRecord = true },
                contentDescription = "记录进展",
                icon = Icons.Default.Add,
                text = "记录",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp),
            )
        }
    }

    // ---- 弹层与对话框 ----

    if (showRecord) {
        RecordSheet(
            goal = current,
            existing = null,
            onSave = { delta, occurDate, note ->
                vm.record(delta, occurDate, note) { entryId ->
                    showRecord = false
                    scope.showLifoUndoSnackbar(
                        host = snackbar,
                        message = "已记录 +${formatNumber(delta)} ${current.unit}",
                    ) {
                        vm.deleteEntry(entryId)
                    }
                }
            },
            onDismiss = { showRecord = false },
        )
    }

    editEntry?.let { entry ->
        RecordSheet(
            goal = current,
            existing = entry,
            onSave = { delta, occurDate, note ->
                vm.updateEntry(entry.id, delta, occurDate, note)
                editEntry = null
            },
            onDelete = {
                vm.deleteEntry(entry.id)
                editEntry = null
            },
            onDismiss = { editEntry = null },
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            goalId = current.id,
            goalName = current.name,
            initial = reminder,
            onSave = { spec ->
                vm.saveReminder(spec)
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false },
        )
    }

    if (showNextActionDialog) {
        NextActionDialog(
            initial = current.nextAction ?: "",
            onSave = { text ->
                vm.setNextAction(text.ifBlank { null })
                showNextActionDialog = false
            },
            onDismiss = { showNextActionDialog = false },
        )
    }

    if (showDeleteGoal) {
        ConfirmDialog(
            title = "删除目标「${current.name}」？",
            text = "目标的记录、步骤与提醒将一并删除。删除后可以撤销，建议优先使用归档。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                showDeleteGoal = false
                vm.deleteGoal { snapshot ->
                    if (snapshot != null) {
                        snackbar.currentSnackbarData?.dismiss()
                        scope.launch {
                            val result = snackbar.showSnackbar(
                                message = "目标已删除",
                                actionLabel = "撤销",
                                duration = androidx.compose.material3.SnackbarDuration.Short,
                            )
                            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                vm.undoDelete(snapshot)
                            } else {
                                onBack()
                            }
                        }
                    } else {
                        onBack()
                    }
                }
            },
            onDismiss = { showDeleteGoal = false },
        )
    }

    deleteStepTarget?.let { step ->
        ConfirmDialog(
            title = "删除步骤「${step.title}」？",
            text = "删除后完成比例会重新计算。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                vm.deleteStep(step.id)
                deleteStepTarget = null
            },
            onDismiss = { deleteStepTarget = null },
        )
    }
}

@Composable
private fun DetailHeader(goal: Goal) {
    val colors = AppleTheme.colors
    val color = goalColor(goal.colorIndex)
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppleIconTile(emoji = goal.iconEmoji(), tint = color, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        goal.category ?: "未分类",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.secondaryLabel,
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(goal)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    Dates.formatDateRange(goal.startDate, goal.deadline),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryLabel,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "${ProgressCalculator.percent(goal.ratio)}%",
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 54.sp, lineHeight = 58.sp),
            color = colors.label,
        )
        Spacer(Modifier.height(14.dp))
        YoujinProgressBar(goal.ratio, color = color)
        Spacer(Modifier.height(16.dp))
        when (goal.type) {
            GoalType.CUMULATIVE -> {
                Text(
                    "${formatNumber(goal.currentValue)} / ${formatNumber(goal.targetValue)} ${goal.unit}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(3.dp))
                val remain = goal.targetValue - goal.currentValue
                Text(
                    if (remain > 0) {
                        "还差 ${formatNumber(remain)} ${goal.unit}"
                    } else {
                        "已超额 ${formatNumber(-remain)} ${goal.unit}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondaryLabel,
                )
            }
            GoalType.CHECKLIST -> {
                Text(
                    "已完成 ${goal.currentValue.toInt()} / ${goal.stepsTotal} 个步骤",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (goal.motivation != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                "“${goal.motivation}”",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryLabel,
            )
        }
    }
}

@Composable
private fun NextActionCard(nextAction: String?, onEdit: () -> Unit) {
    val colors = AppleTheme.colors
    AppleCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "下一步行动",
                style = MaterialTheme.typography.labelMedium,
                color = colors.secondaryLabel,
                modifier = Modifier.weight(1f),
            )
            AppleChevron()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            nextAction ?: "还没有安排，点击设置",
            style = MaterialTheme.typography.bodyLarge,
            color = if (nextAction == null) colors.tertiaryLabel else colors.label,
        )
    }
}

@Composable
private fun NextActionDialog(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AppleDialog(
        title = "下一步行动",
        confirmLabel = "保存",
        onConfirm = { onSave(text) },
        onDismiss = onDismiss,
        content = {
            AppleTextField(
                value = text,
                onValueChange = { if (it.length <= 200) text = it },
                placeholder = "如：今晚跑 3 公里 / 午休听 20 分钟播客",
                singleLine = false,
                minLines = 2,
            )
        },
    )
}

@Composable
private fun StepRow(
    step: GoalStep,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = AppleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleCheckCircle(
            checked = step.done,
            onCheckedChange = { onToggle() },
            contentDescription = "完成步骤",
        )
        Spacer(Modifier.width(12.dp))
        Text(
            step.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (step.done) colors.secondaryLabel else colors.label,
            textDecoration = if (step.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        if (step.done && step.doneDate != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                Dates.formatDay(step.doneDate),
                style = MaterialTheme.typography.labelSmall,
                color = colors.tertiaryLabel,
            )
        }
        AppleIconButton(
            icon = Icons.Default.Delete,
            contentDescription = "删除步骤",
            onClick = onDelete,
            tint = colors.tertiaryLabel,
            size = 36.dp,
            iconSize = 16.dp,
        )
    }
}

@Composable
private fun AddStepRow(
    text: String,
    onChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppleTextField(
            value = text,
            onValueChange = { if (it.length <= 100) onChange(it) },
            placeholder = "添加一个步骤，如：整理内容大纲",
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        AppleButton(
            text = "添加",
            onClick = onAdd,
            modifier = Modifier.width(82.dp),
            enabled = text.isNotBlank(),
            fillWidth = false,
            height = 56.dp,
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderSpec?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val colors = AppleTheme.colors
    AppleCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("提醒", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(3.dp))
                val r = reminder
                Text(
                    when {
                        r == null || !r.enabled -> "未开启"
                        r.isDaily -> "每天 ${Dates.formatTime(r.hour, r.minute)}"
                        else -> {
                            val days = (1..7).filter { (r.weekdaysMask and (1 shl it)) != 0 }
                                .joinToString("、") { Dates.weekdayName(it).removeSuffix("周") }
                            "$days ${Dates.formatTime(r.hour, r.minute)}"
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryLabel,
                )
            }
            AppleSwitch(
                checked = reminder?.enabled ?: false,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun EntryRow(goal: Goal, entry: ProgressEntry, onClick: () -> Unit) {
    val colors = AppleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .appleRowClick(onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Dates.formatDayFull(entry.occurDate),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (entry.isBackfill) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "补记",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.blue,
                    )
                }
            }
            if (entry.note != null) {
                Text(
                    entry.note!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryLabel,
                )
            }
        }
        Text(
            "+${formatNumber(entry.delta)} ${goal.unit}",
            style = MaterialTheme.typography.titleSmall,
            color = goalColor(goal.colorIndex),
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Default.Edit,
            contentDescription = "编辑记录",
            tint = colors.tertiaryLabel,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun CalculationBasis(
    goal: Goal,
    entries: List<ProgressEntry>,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = AppleTheme.colors
    val sum = entries.sumOf { it.delta }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(240, easing = AppleTheme.Easing),
        label = "disclosureRotation",
    )
    AppleCard(onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "计算依据",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.tertiaryLabel,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotation },
            )
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(
                "初始 ${formatNumber(goal.initialValue)} ${goal.unit} + " +
                    "记录合计 ${formatNumber(sum)} ${goal.unit} = " +
                    "当前 ${formatNumber(goal.currentValue)} ${goal.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryLabel,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "目标值 ${formatNumber(goal.targetValue)} ${goal.unit}，共 ${entries.size} 条记录",
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryLabel,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManageSection(
    goal: Goal,
    onSetStatus: (GoalStatus) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = AppleTheme.colors
    Column(Modifier.fillMaxWidth()) {
        AppleSectionHeader("管理", Modifier.padding(top = 12.dp, bottom = 0.dp))
        AppleGroup {
            when (goal.status) {
                GoalStatus.ACTIVE -> {
                    AppleRow(
                        title = "暂停目标",
                        onClick = { onSetStatus(GoalStatus.PAUSED) },
                        titleColor = colors.blue,
                    )
                    Hairline(startIndent = 18.dp)
                    AppleRow(
                        title = "归档目标",
                        onClick = { onSetStatus(GoalStatus.ARCHIVED) },
                        titleColor = colors.blue,
                    )
                }
                GoalStatus.PAUSED -> {
                    AppleRow(
                        title = "恢复进行中",
                        onClick = { onSetStatus(GoalStatus.ACTIVE) },
                        titleColor = colors.blue,
                    )
                    Hairline(startIndent = 18.dp)
                    AppleRow(
                        title = "归档目标",
                        onClick = { onSetStatus(GoalStatus.ARCHIVED) },
                        titleColor = colors.blue,
                    )
                }
                GoalStatus.COMPLETED -> {
                    AppleRow(
                        title = "恢复进行中",
                        onClick = { onSetStatus(GoalStatus.ACTIVE) },
                        titleColor = colors.blue,
                    )
                    Hairline(startIndent = 18.dp)
                    AppleRow(
                        title = "归档目标",
                        onClick = { onSetStatus(GoalStatus.ARCHIVED) },
                        titleColor = colors.blue,
                    )
                }
                GoalStatus.ARCHIVED -> {
                    AppleRow(
                        title = "恢复目标",
                        onClick = { onSetStatus(GoalStatus.ACTIVE) },
                        titleColor = colors.blue,
                    )
                }
            }
            Hairline(startIndent = 18.dp)
            AppleRow(
                title = "删除目标",
                onClick = onDelete,
                titleColor = colors.red,
            )
        }
    }
}
