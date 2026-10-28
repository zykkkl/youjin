package com.youjin.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.youjin.app.domain.Goal
import com.youjin.app.domain.ProgressEntry
import com.youjin.app.ui.components.AppleButton
import com.youjin.app.ui.components.AppleChip
import com.youjin.app.ui.components.AppleDatePickerDialog
import com.youjin.app.ui.components.AppleGroup
import com.youjin.app.ui.components.AppleRow
import com.youjin.app.ui.components.AppleSecondaryButton
import com.youjin.app.ui.components.AppleSheetHandle
import com.youjin.app.ui.components.AppleTextAction
import com.youjin.app.ui.components.AppleTextField
import com.youjin.app.ui.components.ConfirmDialog
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.util.Dates
import com.youjin.app.util.formatNumber

/**
 * 记录面板：新建记录 / 编辑既有记录（含删除）。
 * - 数值输入默认数字键盘，提供常用增量
 * - 发生日期默认为今天，允许补记过去日期，未来日期校验拦截
 * - 保存后立即更新进度，防止连续点击重复提交
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordSheet(
    goal: Goal,
    existing: ProgressEntry?,
    onSave: (delta: Double, occurDate: Long, note: String?) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val colors = AppleTheme.colors
    var deltaText by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.delta?.let { formatNumber(it) } ?: "")
    }
    var occurDate by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.occurDate ?: Dates.todayEpochDay())
    }
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val delta = deltaText.toDoubleOrNull()
    val valid = delta != null && delta > 0 && occurDate <= Dates.todayEpochDay()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentColor = colors.label,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = { AppleSheetHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                if (existing == null) "记录进展" else "编辑记录",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                goal.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryLabel,
            )
            Spacer(Modifier.height(20.dp))

            Text(
                "本次新增量（${goal.unit}）",
                style = MaterialTheme.typography.labelMedium,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(start = 2.dp, bottom = 7.dp),
            )
            AppleTextField(
                value = deltaText,
                onValueChange = {
                    if (it.length <= 12) {
                        deltaText = it.filter { c -> c.isDigit() || c == '.' }
                    }
                },
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = deltaText.isNotEmpty() && (delta == null || delta <= 0),
            )
            if (deltaText.isNotEmpty() && (delta == null || delta <= 0)) {
                Text(
                    "请输入大于 0 的数值",
                    color = colors.red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "常用增量",
                style = MaterialTheme.typography.labelMedium,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1.0, 5.0, 10.0).forEach { quantity ->
                    val label = formatNumber(quantity)
                    AppleChip(
                        text = "+$label ${goal.unit}",
                        selected = deltaText == label,
                        onClick = { deltaText = label },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            AppleGroup {
                AppleRow(
                    title = "发生日期",
                    onClick = { showDatePicker = true },
                    showChevron = true,
                    trailing = {
                        Text(
                            when {
                                occurDate == Dates.todayEpochDay() -> "今天"
                                occurDate > Dates.todayEpochDay() -> Dates.formatDayFull(occurDate)
                                else -> Dates.formatDayFull(occurDate) + "（补记）"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (occurDate > Dates.todayEpochDay()) colors.red else colors.secondaryLabel,
                        )
                    },
                )
            }
            if (occurDate > Dates.todayEpochDay()) {
                Text(
                    "实际进展不能记在未来",
                    color = colors.red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "备注（可选）",
                style = MaterialTheme.typography.labelMedium,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(start = 2.dp, bottom = 7.dp),
            )
            AppleTextField(
                value = note,
                onValueChange = { if (it.length <= 200) note = it },
                placeholder = "今天做了什么？",
                singleLine = false,
                minLines = 2,
            )

            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                AppleTextAction(
                    text = "删除这条记录",
                    onClick = { showDeleteConfirm = true },
                    color = colors.red,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppleSecondaryButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !saving,
                    fillWidth = false,
                )
                AppleButton(
                    text = "保存",
                    onClick = {
                        val value = delta
                        if (value != null && value > 0 && occurDate <= Dates.todayEpochDay() && !saving) {
                            saving = true
                            onSave(value, occurDate, note.ifBlank { null })
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = valid && !saving,
                    loading = saving,
                    fillWidth = false,
                )
            }
        }
    }

    if (showDatePicker) {
        AppleDatePickerDialog(
            title = "发生日期",
            initialEpochDay = occurDate,
            onConfirm = { day ->
                occurDate = day
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除这条记录？",
            text = "删除后进度与统计将重新计算。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete?.invoke()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}
