package com.youjin.app.ui.today

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youjin.app.domain.Goal
import com.youjin.app.domain.GoalStatus
import com.youjin.app.domain.GoalStep
import com.youjin.app.domain.GoalType
import com.youjin.app.domain.ProgressEntry
import com.youjin.app.ui.components.AppleCard
import com.youjin.app.ui.components.AppleGroup
import com.youjin.app.ui.components.AppleIconButton
import com.youjin.app.ui.components.AppleLargeTitle
import com.youjin.app.ui.components.AppleRow
import com.youjin.app.ui.components.AppleSectionHeader
import com.youjin.app.ui.components.AppleSheetHandle
import com.youjin.app.ui.components.ColorDot
import com.youjin.app.ui.components.ConfirmDialog
import com.youjin.app.ui.components.EmptyState
import com.youjin.app.ui.components.GoalCard
import com.youjin.app.ui.components.Hairline
import com.youjin.app.ui.components.LocalSnackbarHost
import com.youjin.app.ui.components.appleRowClick
import com.youjin.app.ui.components.goalColor
import com.youjin.app.ui.components.showLifoUndoSnackbar
import com.youjin.app.ui.record.ChecklistSheet
import com.youjin.app.ui.record.RecordSheet
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.util.Dates
import com.youjin.app.util.formatNumber
import kotlinx.coroutines.launch

/** 今天页：置顶目标卡片 + 今日记录。长按目标卡片（或记录中的步骤行）可归档/删除目标。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    vm: TodayViewModel,
    onCreateGoal: () -> Unit,
    onOpenGoal: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val colors = AppleTheme.colors

    var recordGoal by remember { mutableStateOf<Goal?>(null) }
    var checklistGoal by remember { mutableStateOf<Goal?>(null) }
    var editEntry by remember { mutableStateOf<ProgressEntry?>(null) }
    var undoTargetStep by remember { mutableStateOf<GoalStep?>(null) }
    var actionGoal by remember { mutableStateOf<Goal?>(null) }
    var confirmDeleteGoal by remember { mutableStateOf<Goal?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AppleLargeTitle(
            title = "今天",
            subtitle = Dates.formatMonthDayWeekday(state.today),
            trailing = {
                AppleIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "设置",
                    onClick = onOpenSettings,
                    tint = colors.secondaryLabel,
                    iconSize = 21.dp,
                )
            },
        )

        if (!state.hasAnyGoal) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    emoji = "🎯",
                    title = "还没有目标",
                    subtitle = "创建你的第一个目标，把“我想做到”变成“我正在做到”",
                    actionLabel = "创建目标",
                    onAction = onCreateGoal,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AppleSectionHeader(
                        "进行中的目标",
                        Modifier.padding(top = 4.dp, bottom = 0.dp),
                    )
                }
                items(state.visibleGoals, key = { "goal-${it.id}" }) { goal ->
                    GoalCard(
                        goal = goal,
                        onOpen = { onOpenGoal(goal.id) },
                        onRecord = {
                            // 记录逻辑按目标类型分流：数值型 → 数值面板；步骤型 → 步骤勾选面板
                            if (goal.type == GoalType.CUMULATIVE) {
                                recordGoal = goal
                            } else {
                                checklistGoal = goal
                            }
                        },
                        onLongPress = { actionGoal = goal },
                    )
                }
                if (state.visibleGoals.isEmpty() && state.hasActiveGoals) {
                    item {
                        EmptyState(
                            emoji = "🧭",
                            title = "没有置顶的目标",
                            subtitle = "在目标详情中开启置顶，就会出现在首页",
                        )
                    }
                }

                item {
                    AppleSectionHeader(
                        "今日记录",
                        Modifier.padding(top = 12.dp, bottom = 0.dp),
                    )
                }
                if (state.records.isEmpty()) {
                    item {
                        AppleCard(contentPadding = PaddingValues(18.dp)) {
                            Text(
                                "今天还没有记录，行动完成后记得回来打卡",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.secondaryLabel,
                            )
                        }
                    }
                } else {
                    // 栈式时间线：最近一次操作排最前，撤销即针对最近操作
                    item {
                        AppleGroup {
                            state.records.forEachIndexed { index, record ->
                                key(record.key) {
                                    when (record) {
                                        is TodayRecord.EntryRecord -> TodayEntryRow(
                                            goal = state.goalsById[record.entry.goalId],
                                            entry = record.entry,
                                            onClick = { editEntry = record.entry },
                                        )

                                        is TodayRecord.StepRecord -> TodayStepRow(
                                            goal = state.goalsById[record.step.goalId],
                                            step = record.step,
                                            onUndo = { undoTargetStep = record.step },
                                            onLongPress = {
                                                state.goalsById[record.step.goalId]?.let { actionGoal = it }
                                            },
                                        )
                                    }
                                }
                                if (index < state.records.lastIndex) {
                                    Hairline(startIndent = 18.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- 记录面板（新建） ----
    recordGoal?.let { goal ->
        RecordSheet(
            goal = goal,
            existing = null,
            onSave = { delta, occurDate, note ->
                vm.record(goal.id, delta, occurDate, note) { entryId ->
                    recordGoal = null
                    scope.showLifoUndoSnackbar(
                        host = snackbar,
                        message = "已记录 +${formatNumber(delta)} ${goal.unit}",
                    ) {
                        vm.deleteEntry(entryId)
                    }
                }
            },
            onDismiss = { recordGoal = null },
        )
    }

    // ---- 步骤面板（步骤清单型：勾选步骤，不输入数值） ----
    checklistGoal?.let { goal ->
        ChecklistSheet(
            goal = goal,
            steps = state.stepsByGoal[goal.id].orEmpty(),
            onToggle = { step, done -> vm.toggleStep(step.id, done) },
            onOpenDetail = {
                checklistGoal = null
                onOpenGoal(goal.id)
            },
            onDismiss = { checklistGoal = null },
        )
    }

    // ---- 记录面板（编辑） ----
    editEntry?.let { entry ->
        val goal = state.goalsById[entry.goalId]
        if (goal != null) {
            RecordSheet(
                goal = goal,
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
    }

    undoTargetStep?.let { step ->
        ConfirmDialog(
            title = "撤销完成？",
            text = "将步骤「${step.title}」恢复为未完成。",
            confirmLabel = "撤销",
            onConfirm = {
                vm.undoToggleStep(step.id)
                undoTargetStep = null
            },
            onDismiss = { undoTargetStep = null },
        )
    }

    // ---- 长按目标：归档 / 删除 ----
    actionGoal?.let { goal ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { actionGoal = null },
            sheetState = sheetState,
            containerColor = colors.background,
            contentColor = colors.label,
            scrimColor = Color.Black.copy(alpha = 0.32f),
            dragHandle = { AppleSheetHandle() },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    goal.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    when (goal.status) {
                        GoalStatus.ARCHIVED -> "目标已归档，可随时恢复"
                        GoalStatus.PAUSED -> "目标已暂停"
                        else -> "长按操作"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryLabel,
                )
                Spacer(Modifier.height(16.dp))
                AppleGroup {
                    AppleRow(
                        title = if (goal.status == GoalStatus.ARCHIVED) "恢复进行中" else "归档目标",
                        subtitle = if (goal.status == GoalStatus.ARCHIVED) {
                            null
                        } else {
                            "停止提醒并收起目标，记录保留"
                        },
                        onClick = {
                            val target = if (goal.status == GoalStatus.ARCHIVED) {
                                GoalStatus.ACTIVE
                            } else {
                                GoalStatus.ARCHIVED
                            }
                            actionGoal = null
                            vm.setStatus(goal.id, target)
                            scope.launch {
                                snackbar.showSnackbar(
                                    if (target == GoalStatus.ARCHIVED) {
                                        "已归档「${goal.name}」"
                                    } else {
                                        "已恢复「${goal.name}」"
                                    }
                                )
                            }
                        },
                    )
                    Hairline(startIndent = 18.dp)
                    AppleRow(
                        title = "删除目标",
                        subtitle = "记录、步骤与提醒将一并删除，可短时撤销",
                        titleColor = colors.red,
                        onClick = {
                            actionGoal = null
                            confirmDeleteGoal = goal
                        },
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "归档会保留全部记录；删除后可在提示中撤销。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.tertiaryLabel,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }

    confirmDeleteGoal?.let { goal ->
        ConfirmDialog(
            title = "删除目标「${goal.name}」？",
            text = "目标的记录、步骤与提醒将一并删除。删除后可以撤销，建议优先使用归档。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                confirmDeleteGoal = null
                vm.deleteGoal(goal.id) { snapshot ->
                    if (snapshot != null) {
                        scope.showLifoUndoSnackbar(host = snackbar, message = "目标已删除") {
                            vm.undoDelete(snapshot)
                        }
                    }
                }
            },
            onDismiss = { confirmDeleteGoal = null },
        )
    }
}

@Composable
private fun TodayEntryRow(
    goal: Goal?,
    entry: ProgressEntry,
    onClick: () -> Unit,
) {
    val colors = AppleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .appleRowClick(onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(goalColor(goal?.colorIndex ?: 0))
        Spacer(Modifier.width(12.dp))
        Text(
            Dates.formatClock(entry.createdAt),
            style = MaterialTheme.typography.labelMedium,
            color = colors.tertiaryLabel,
            modifier = Modifier.width(44.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                "${goal?.name ?: "（目标已删除）"}  +${formatNumber(entry.delta)} ${goal?.unit ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.note != null) {
                Text(
                    entry.note!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (entry.isBackfill) {
            Spacer(Modifier.width(8.dp))
            Text(
                "补记",
                style = MaterialTheme.typography.labelSmall,
                color = colors.blue,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Default.Edit,
            contentDescription = "编辑记录",
            tint = colors.tertiaryLabel,
            modifier = Modifier.size(15.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodayStepRow(
    goal: Goal?,
    step: GoalStep,
    onUndo: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = AppleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onLongPress,
            )
            .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = colors.blue,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "完成步骤",
            style = MaterialTheme.typography.labelMedium,
            color = colors.blue,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "${goal?.name ?: "（目标已删除）"} · ${step.title}",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AppleIconButton(
            icon = Icons.Default.Refresh,
            contentDescription = "撤销完成",
            onClick = onUndo,
            tint = colors.tertiaryLabel,
            size = 36.dp,
            iconSize = 16.dp,
        )
    }
}
