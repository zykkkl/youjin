package com.youjin.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.youjin.app.domain.Goal
import com.youjin.app.domain.ProgressCalculator
import com.youjin.app.ui.components.AppleButton
import com.youjin.app.ui.components.AppleCard
import com.youjin.app.ui.components.AppleSecondaryButton
import com.youjin.app.ui.components.AppleSheetHandle
import com.youjin.app.ui.components.YoujinProgressBar
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.util.Dates

/**
 * 打卡型目标的打卡面板：显示连续天数与近几周的打卡日历，
 * 一天只允许打一次卡；今天打过卡后可以撤销。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinSheet(
    goal: Goal,
    onCheckIn: () -> Unit,
    onUndoCheckIn: () -> Unit,
    onOpenDetail: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = Dates.todayEpochDay()
    val dates = goal.checkinDates
    val checkedToday = goal.checkedToday()
    val weekStart = ProgressCalculator.weekStart(today)
    val weekCount = ProgressCalculator.checkinsInWeek(dates, weekStart)

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
                if (checkedToday) "今日已打卡" else "打卡",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                goal.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryLabel,
            )

            Spacer(Modifier.height(16.dp))
            AppleCard(contentPadding = PaddingValues(18.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    StatColumn("连续", "${goal.checkinStreak} 天", Modifier.weight(1f))
                    StatColumn(
                        "累计",
                        "${goal.currentValue.toInt()} / ${goal.targetValue.toInt()} 天",
                        Modifier.weight(1f),
                    )
                    StatColumn("最长", "${goal.checkinBestStreak} 天", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "本周已打卡 $weekCount 天",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryLabel,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${ProgressCalculator.percent(goal.ratio)}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.label,
                    )
                }
                Spacer(Modifier.height(8.dp))
                YoujinProgressBar(goal.ratio)
            }

            Spacer(Modifier.height(16.dp))
            AppleCard(contentPadding = PaddingValues(18.dp)) {
                Text(
                    "近 10 周",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryLabel,
                )
                Spacer(Modifier.height(12.dp))
                CheckinCalendar(dates = dates.toSet(), today = today)
                Spacer(Modifier.height(10.dp))
                Text(
                    "蓝色方块为已打卡的日子，空心框是今天。",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.tertiaryLabel,
                )
            }

            Spacer(Modifier.height(20.dp))
            if (checkedToday) {
                AppleSecondaryButton(
                    text = "撤销今日打卡",
                    onClick = onUndoCheckIn,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                AppleButton(
                    text = "打卡",
                    onClick = onCheckIn,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppleSecondaryButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    fillWidth = false,
                )
                AppleButton(
                    text = "打开目标详情",
                    onClick = onOpenDetail,
                    modifier = Modifier.weight(1f),
                    fillWidth = false,
                )
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = AppleTheme.colors
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.secondaryLabel,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = colors.label,
        )
    }
}

/**
 * 打卡日历：每一列是一周（周一到周日），右侧为最近一周。
 * 已打卡的日期填充品牌蓝，今天用描边标出，未来日期留空。
 */
@Composable
fun CheckinCalendar(
    dates: Set<Long>,
    today: Long,
    modifier: Modifier = Modifier,
    weeks: Int = 10,
) {
    val colors = AppleTheme.colors
    val start = ProgressCalculator.weekStart(today) - (weeks - 1) * 7L
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (w in 0 until weeks) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (d in 0 until 7) {
                    val day = start + w * 7L + d
                    val checked = dates.contains(day)
                    val isToday = day == today
                    val future = day > today
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    future -> Color.Transparent
                                    checked -> colors.blue
                                    else -> colors.fill
                                }
                            )
                            .then(
                                if (isToday) {
                                    Modifier.border(1.5.dp, colors.blue, RoundedCornerShape(4.dp))
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }
}
