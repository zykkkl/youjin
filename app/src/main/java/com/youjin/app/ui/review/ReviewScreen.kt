package com.youjin.app.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youjin.app.data.ReviewRepository
import com.youjin.app.domain.GoalType
import com.youjin.app.ui.components.AppleButton
import com.youjin.app.ui.components.AppleCard
import com.youjin.app.ui.components.AppleGroup
import com.youjin.app.ui.components.AppleIconButton
import com.youjin.app.ui.components.AppleLargeTitle
import com.youjin.app.ui.components.AppleSectionHeader
import com.youjin.app.ui.components.AppleTextField
import com.youjin.app.ui.components.EmptyState
import com.youjin.app.ui.components.Hairline
import com.youjin.app.ui.components.LocalSnackbarHost
import com.youjin.app.ui.components.goalEmoji
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.util.Dates
import com.youjin.app.util.formatNumber
import kotlinx.coroutines.launch

/** 回顾页：周进展事实性汇总 + 回顾笔记 */
@Composable
fun ReviewScreen(vm: ReviewViewModel) {
    val weekStart by vm.weekStart.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val review by vm.review.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val colors = AppleTheme.colors

    var noteDraft by remember { mutableStateOf("") }
    var loadedWeek by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(review, weekStart) {
        if (loadedWeek != weekStart) {
            noteDraft = review?.body ?: ""
            loadedWeek = weekStart
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AppleLargeTitle(
            title = "回顾",
            subtitle = "${Dates.formatDay(weekStart)} - ${Dates.formatDay(weekStart + 6)}" +
                (if (vm.isCurrentWeek) "（本周）" else ""),
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppleIconButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上一周",
                        onClick = vm::prevWeek,
                        tint = colors.blue,
                        size = 40.dp,
                        iconSize = 24.dp,
                    )
                    AppleIconButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下一周",
                        onClick = vm::nextWeek,
                        tint = colors.blue,
                        size = 40.dp,
                        iconSize = 24.dp,
                        enabled = !vm.isCurrentWeek,
                    )
                }
            },
        )

        val s = stats
        if (s == null) {
            EmptyState(emoji = "📊", title = "加载中…")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SummaryCard(s) }

                item {
                    AppleSectionHeader(
                        "各目标本周进展",
                        Modifier.padding(top = 12.dp, bottom = 0.dp),
                    )
                }
                if (s.goals.isEmpty()) {
                    item {
                        EmptyState(
                            emoji = "🌱",
                            title = "没有进行中的目标",
                            subtitle = "创建目标后，这里会按周汇总每个目标的进展",
                        )
                    }
                } else {
                    item {
                        AppleGroup {
                            s.goals.forEachIndexed { index, stat ->
                                WeekGoalRow(stat)
                                if (index < s.goals.lastIndex) {
                                    Hairline(startIndent = 18.dp)
                                }
                            }
                        }
                    }
                }

                item {
                    AppleSectionHeader(
                        "回顾笔记",
                        Modifier.padding(top = 12.dp, bottom = 0.dp),
                    )
                }
                item {
                    AppleTextField(
                        value = noteDraft,
                        onValueChange = { if (it.length <= 1000) noteDraft = it },
                        placeholder = "什么做法有效？下周准备调整什么？",
                        singleLine = false,
                        minLines = 5,
                    )
                }
                item {
                    AppleButton(
                        text = "保存回顾笔记",
                        onClick = {
                            vm.saveReview(noteDraft)
                            scope.launch { snackbar.showSnackbar("回顾笔记已保存") }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(stats: ReviewRepository.WeekStats) {
    val colors = AppleTheme.colors
    AppleCard(contentPadding = PaddingValues(20.dp)) {
        Text("本周小结", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        if (stats.activeGoalCount == 0) {
            Text(
                "本周暂无新增记录。数据缺失不等于没有行动，下周继续。",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryLabel,
            )
        } else {
            Text(
                "本周有 ${stats.activeGoalCount} 个目标新增记录",
                style = MaterialTheme.typography.bodyMedium,
            )
            stats.goals.filter { it.entryCount > 0 || it.stepsDone > 0 }.forEach { stat ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(goalEmoji(stat.iconIndex), fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (stat.type) {
                            GoalType.CUMULATIVE ->
                                "${stat.name}：+${formatNumber(stat.delta)} ${stat.unit}（${stat.entryCount} 次）"
                            GoalType.CHECKLIST ->
                                "${stat.name}：完成 ${stat.stepsDone} 个步骤"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekGoalRow(stat: ReviewRepository.GoalWeekStat) {
    val colors = AppleTheme.colors
    val active = stat.entryCount > 0 || stat.stepsDone > 0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(goalEmoji(stat.iconIndex), fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            stat.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            when (stat.type) {
                GoalType.CUMULATIVE ->
                    if (stat.entryCount > 0) "本周 +${formatNumber(stat.delta)} ${stat.unit}"
                    else "本周暂无记录"
                GoalType.CHECKLIST ->
                    if (stat.stepsDone > 0) "完成 ${stat.stepsDone} 个步骤"
                    else "本周暂无记录"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (active) colors.blue else colors.secondaryLabel,
        )
    }
}
