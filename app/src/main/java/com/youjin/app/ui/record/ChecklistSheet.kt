package com.youjin.app.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.youjin.app.domain.Goal
import com.youjin.app.domain.GoalStep
import com.youjin.app.domain.ProgressCalculator
import com.youjin.app.ui.components.AppleButton
import com.youjin.app.ui.components.AppleCard
import com.youjin.app.ui.components.AppleCheckCircle
import com.youjin.app.ui.components.AppleGroup
import com.youjin.app.ui.components.AppleSecondaryButton
import com.youjin.app.ui.components.AppleSheetHandle
import com.youjin.app.ui.components.Hairline
import com.youjin.app.ui.components.YoujinProgressBar
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.util.Dates

/**
 * 步骤清单型目标的“记录进展”面板：勾选步骤推进进度，
 * 与累计数值型的数值输入逻辑完全区分——不出现数值、单位与增减量。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistSheet(
    goal: Goal,
    steps: List<GoalStep>,
    onToggle: (GoalStep, Boolean) -> Unit,
    onOpenDetail: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val doneCount = steps.count { it.done }
    val totalCount = steps.size
    val ratio = ProgressCalculator.checklistRatio(doneCount, totalCount)

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
                "勾选步骤",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                goal.name,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryLabel,
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "已完成 $doneCount / $totalCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondaryLabel,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${ProgressCalculator.percent(ratio)}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.label,
                )
            }
            Spacer(Modifier.height(8.dp))
            YoujinProgressBar(ratio)
            Spacer(Modifier.height(16.dp))

            if (totalCount == 0) {
                AppleCard {
                    Text(
                        "还没有步骤，去目标详情添加步骤后再勾选",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryLabel,
                    )
                }
            } else {
                AppleGroup {
                    steps.forEachIndexed { index, step ->
                        StepCheckRow(
                            step = step,
                            onToggle = { onToggle(step, !step.done) },
                        )
                        if (index < steps.lastIndex) {
                            Hairline(startIndent = 56.dp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
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
private fun StepCheckRow(
    step: GoalStep,
    onToggle: () -> Unit,
) {
    val colors = AppleTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleCheckCircle(
            checked = step.done,
            onCheckedChange = { onToggle() },
            contentDescription = step.title,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            step.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (step.done) colors.tertiaryLabel else colors.label,
            textDecoration = if (step.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        if (step.done && step.doneDate != null) {
            Text(
                Dates.formatDay(step.doneDate),
                style = MaterialTheme.typography.labelSmall,
                color = colors.tertiaryLabel,
            )
        }
    }
}
