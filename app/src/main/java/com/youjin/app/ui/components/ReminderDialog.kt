package com.youjin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.youjin.app.domain.ReminderSpec
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.util.Dates

/** 提醒设置对话框：启用开关、选择星期（默认每天）、提醒时间 */
@Composable
fun ReminderDialog(
    goalId: Long,
    goalName: String,
    initial: ReminderSpec?,
    onSave: (ReminderSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppleTheme.colors
    var enabled by remember { mutableStateOf(initial?.enabled ?: false) }
    var mask by remember { mutableStateOf(initial?.weekdaysMask ?: ReminderSpec.ALL_DAYS_MASK) }
    var hour by remember { mutableStateOf(initial?.hour ?: 9) }
    var minute by remember { mutableStateOf(initial?.minute ?: 0) }
    var showTime by remember { mutableStateOf(false) }

    AppleDialog(
        title = "提醒设置",
        message = goalName,
        confirmLabel = "保存",
        onConfirm = {
            onSave(
                ReminderSpec(
                    id = initial?.id ?: 0,
                    goalId = goalId,
                    enabled = enabled,
                    weekdaysMask = mask,
                    hour = hour,
                    minute = minute,
                )
            )
        },
        onDismiss = onDismiss,
        content = {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "启用提醒",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    AppleSwitch(checked = enabled, onCheckedChange = { enabled = it })
                }
                if (enabled) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "重复",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.secondaryLabel,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        (1..7).forEach { day ->
                            val selected = (mask and (1 shl day)) != 0
                            WeekdayCircle(
                                label = Dates.weekdayName(day).removePrefix("周"),
                                contentDescription = Dates.weekdayName(day),
                                selected = selected,
                                onClick = {
                                    val newMask = if (selected) {
                                        mask and (1 shl day).inv()
                                    } else {
                                        mask or (1 shl day)
                                    }
                                    if (newMask != 0) mask = newMask // 至少保留一天
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    AppleRow(
                        title = "提醒时间",
                        onClick = { showTime = true },
                        showChevron = true,
                        trailing = {
                            Text(
                                Dates.formatTime(hour, minute),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.blue,
                            )
                        },
                        modifier = Modifier.padding(horizontal = 0.dp),
                    )
                }
            }
        },
    )

    if (showTime) {
        AppleTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { newHour, newMinute ->
                hour = newHour
                minute = newMinute
                showTime = false
            },
            onDismiss = { showTime = false },
        )
    }
}

/** 星期选择：圆形按钮，选中蓝底白字，比一排黑色胶囊更轻盈 */
@Composable
private fun WeekdayCircle(
    label: String,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .aspectRatio(1f)
            .pressScale(source, 0.9f)
            .clip(CircleShape)
            .background(if (selected) colors.blue else colors.fill)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Color.White else colors.label,
        )
    }
}
