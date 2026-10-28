package com.youjin.app.ui.goals

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youjin.app.domain.CATEGORIES
import com.youjin.app.domain.GOAL_EMOJIS
import com.youjin.app.domain.GoalType
import com.youjin.app.domain.ReminderSpec
import com.youjin.app.ui.components.AppleChip
import com.youjin.app.ui.components.AppleDatePickerDialog
import com.youjin.app.ui.components.AppleGroup
import com.youjin.app.ui.components.AppleIconButton
import com.youjin.app.ui.components.AppleNavBar
import com.youjin.app.ui.components.AppleRow
import com.youjin.app.ui.components.AppleSectionHeader
import com.youjin.app.ui.components.AppleSegmented
import com.youjin.app.ui.components.AppleSwitch
import com.youjin.app.ui.components.AppleTextAction
import com.youjin.app.ui.components.AppleTextField
import com.youjin.app.ui.components.Hairline
import com.youjin.app.ui.components.LocalSnackbarHost
import com.youjin.app.ui.components.ReminderDialog
import com.youjin.app.ui.components.pressScale
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.ui.theme.GoalPalette
import com.youjin.app.util.Dates
import kotlinx.coroutines.launch

/** 创建 / 编辑目标。类型与单位创建后不可修改（首版不支持切换类型）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalEditScreen(
    goalId: Long?,
    vm: GoalEditViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = AppleTheme.colors

    var showStartPicker by remember { mutableStateOf(false) }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                snackbar.showSnackbar("通知权限未开启，提醒不会显示；可在系统设置中开启")
            }
        }
    }

    LaunchedEffect(state.savedGoalId) {
        if (state.savedGoalId != null) {
            onSaved()
            vm.consumeSaved()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }

    Column(Modifier.fillMaxSize()) {
        AppleNavBar(
            title = if (state.isEdit) "编辑目标" else "新建目标",
            onBack = onBack,
            actions = {
                AppleTextAction(
                    text = if (state.saving) "保存中…" else "保存",
                    onClick = { vm.save() },
                    enabled = state.canSave && !state.saving,
                )
            },
        )

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.blue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AppleTextField(
                        value = state.name,
                        onValueChange = vm::setName,
                        placeholder = "目标名称，如：90 天学习英语 40 小时",
                    )
                }

                item {
                    AppleSectionHeader(
                        "类型",
                        Modifier.padding(top = 10.dp, bottom = 0.dp),
                    )
                }
                item {
                    AppleSegmented(
                        options = listOf("累计数值", "步骤清单"),
                        selectedIndex = if (state.type == GoalType.CUMULATIVE) 0 else 1,
                        onSelect = { index ->
                            vm.setType(if (index == 0) GoalType.CUMULATIVE else GoalType.CHECKLIST)
                        },
                        enabled = !state.isEdit,
                    )
                }
                if (state.isEdit) {
                    item {
                        Text(
                            "首版不支持切换目标类型，如需变更请新建目标",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.tertiaryLabel,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                item {
                    AppleSectionHeader(
                        "分类",
                        Modifier.padding(top = 10.dp, bottom = 0.dp),
                    )
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AppleChip(
                            text = "无",
                            selected = state.category == null,
                            onClick = { vm.setCategory(null) },
                        )
                        CATEGORIES.forEach { category ->
                            AppleChip(
                                text = category,
                                selected = state.category == category,
                                onClick = { vm.setCategory(category) },
                            )
                        }
                    }
                }

                item {
                    AppleSectionHeader(
                        "图标与颜色",
                        Modifier.padding(top = 10.dp, bottom = 0.dp),
                    )
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GOAL_EMOJIS.forEachIndexed { index, emoji ->
                            EmojiOption(
                                emoji = emoji,
                                selected = state.iconIndex == index,
                                tint = GoalPalette[index],
                                onClick = { vm.setIconIndex(index) },
                            )
                        }
                    }
                }

                if (state.type == GoalType.CUMULATIVE) {
                    item {
                        AppleSectionHeader(
                            "目标值",
                            Modifier.padding(top = 10.dp, bottom = 0.dp),
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("单位")
                                AppleTextField(
                                    value = state.unit,
                                    onValueChange = vm::setUnit,
                                    placeholder = "公里/小时/本",
                                    enabled = !state.isEdit,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                FieldLabel("目标值")
                                AppleTextField(
                                    value = state.targetText,
                                    onValueChange = vm::setTarget,
                                    placeholder = "60",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    isError = state.targetText.isNotEmpty() &&
                                        (state.targetValue ?: 0.0) <= 0,
                                )
                            }
                        }
                    }
                    if (state.isEdit) {
                        item {
                            Text(
                                "单位创建后不可修改，请先确认后再保存",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.tertiaryLabel,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                    item {
                        Column {
                            FieldLabel("初始已完成量（可选，默认 0）")
                            AppleTextField(
                                value = state.initialText,
                                onValueChange = vm::setInitial,
                                placeholder = "0",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = state.initialText.isNotEmpty() && (state.initialValue ?: 0.0) < 0,
                            )
                        }
                    }
                } else {
                    item {
                        AppleSectionHeader(
                            "步骤清单",
                            Modifier.padding(top = 10.dp, bottom = 0.dp),
                        )
                    }
                    items(state.steps.size) { index ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppleTextField(
                                value = state.steps[index],
                                onValueChange = { new ->
                                    vm.setSteps(state.steps.toMutableList().also { it[index] = new })
                                },
                                placeholder = "步骤 ${index + 1}",
                                modifier = Modifier.weight(1f),
                            )
                            AppleIconButton(
                                icon = Icons.Default.Close,
                                contentDescription = "删除步骤",
                                onClick = {
                                    vm.setSteps(state.steps.toMutableList().also { it.removeAt(index) })
                                },
                                tint = colors.tertiaryLabel,
                                size = 40.dp,
                                iconSize = 18.dp,
                            )
                        }
                    }
                    item {
                        AppleTextAction(
                            text = "＋ 添加步骤",
                            onClick = { vm.setSteps(state.steps + "") },
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }
                }

                item {
                    AppleSectionHeader(
                        "计划",
                        Modifier.padding(top = 10.dp, bottom = 0.dp),
                    )
                }
                item {
                    Column {
                        FieldLabel("下一步行动（可选）")
                        AppleTextField(
                            value = state.nextAction,
                            onValueChange = vm::setNextAction,
                            placeholder = "如：今晚跑 3 公里",
                        )
                    }
                }
                item {
                    Column {
                        FieldLabel("动机（可选）")
                        AppleTextField(
                            value = state.motivation,
                            onValueChange = vm::setMotivation,
                            placeholder = "为什么想达成它？",
                            singleLine = false,
                            minLines = 3,
                        )
                    }
                }
                item {
                    AppleGroup {
                        AppleRow(
                            title = "开始日期",
                            onClick = { showStartPicker = true },
                            showChevron = true,
                            trailing = {
                                Text(
                                    state.startDate?.let { Dates.formatDayFull(it) } ?: "未设置",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (state.startDate == null) colors.tertiaryLabel else colors.secondaryLabel,
                                )
                            },
                        )
                        Hairline(startIndent = 18.dp)
                        AppleRow(
                            title = "截止日期",
                            subtitle = "可选，默认无",
                            onClick = { showDeadlinePicker = true },
                            showChevron = true,
                            trailing = {
                                Text(
                                    state.deadline?.let { Dates.formatDayFull(it) } ?: "未设置",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (state.deadline == null) colors.tertiaryLabel else colors.secondaryLabel,
                                )
                            },
                        )
                    }
                }
                if (!state.dateValid) {
                    item {
                        Text(
                            "截止日期不能早于开始日期",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.red,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                item {
                    AppleSectionHeader(
                        "更多",
                        Modifier.padding(top = 10.dp, bottom = 0.dp),
                    )
                }
                item {
                    AppleGroup {
                        AppleRow(
                            title = "置顶到首页",
                            subtitle = "置顶目标优先展示在今天页",
                            trailing = {
                                AppleSwitch(checked = state.pinned, onCheckedChange = vm::setPinned)
                            },
                        )
                        Hairline(startIndent = 18.dp)
                        AppleRow(
                            title = "每日/每周提醒",
                            subtitle = run {
                                val r = state.reminder
                                when {
                                    r == null || !r.enabled -> "未开启"
                                    r.isDaily -> "每天 ${Dates.formatTime(r.hour, r.minute)}"
                                    else -> {
                                        val days = (1..7).filter { (r.weekdaysMask and (1 shl it)) != 0 }
                                            .joinToString("、") { Dates.weekdayName(it).removeSuffix("周") }
                                        "$days ${Dates.formatTime(r.hour, r.minute)}"
                                    }
                                }
                            },
                            trailing = {
                                AppleSwitch(
                                    checked = state.reminder?.enabled ?: false,
                                    onCheckedChange = { want ->
                                        if (want) {
                                            if (Build.VERSION.SDK_INT >= 33 &&
                                                ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.POST_NOTIFICATIONS,
                                                ) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            vm.setReminder(
                                                state.reminder?.copy(enabled = true)
                                                    ?: ReminderSpec(
                                                        0,
                                                        goalId ?: 0,
                                                        true,
                                                        ReminderSpec.ALL_DAYS_MASK,
                                                        9,
                                                        0,
                                                    )
                                            )
                                        } else {
                                            vm.setReminder(state.reminder?.copy(enabled = false))
                                        }
                                    },
                                )
                            },
                        )
                        if (state.reminder?.enabled == true) {
                            Hairline(startIndent = 18.dp)
                            AppleRow(
                                title = "设置星期与时间",
                                onClick = { showReminderDialog = true },
                                showChevron = true,
                                titleColor = colors.blue,
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- 日期选择 ----

    if (showStartPicker) {
        AppleDatePickerDialog(
            title = "选择开始日期",
            initialEpochDay = state.startDate,
            allowClear = false,
            onConfirm = { vm.setStartDate(it); showStartPicker = false },
            onDismiss = { showStartPicker = false },
        )
    }
    if (showDeadlinePicker) {
        AppleDatePickerDialog(
            title = "选择截止日期",
            initialEpochDay = state.deadline,
            allowClear = true,
            onConfirm = { vm.setDeadline(it); showDeadlinePicker = false },
            onClear = { vm.setDeadline(null); showDeadlinePicker = false },
            onDismiss = { showDeadlinePicker = false },
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            goalId = goalId ?: 0,
            goalName = state.name.ifBlank { "新目标" },
            initial = state.reminder,
            onSave = { spec ->
                vm.setReminder(spec)
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = AppleTheme.colors.secondaryLabel,
        modifier = Modifier.padding(start = 2.dp, bottom = 7.dp),
    )
}

@Composable
private fun EmojiOption(
    emoji: String,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    val colors = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(50.dp)
            .pressScale(source, 0.92f)
            .clip(CircleShape)
            .background(if (selected) tint.copy(alpha = if (colors.isDark) 0.24f else 0.14f) else Color.Transparent)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) tint else colors.border,
                shape = CircleShape,
            )
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 22.sp)
    }
}

