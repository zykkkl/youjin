package com.youjin.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youjin.app.domain.GOAL_EMOJIS
import com.youjin.app.domain.Goal
import com.youjin.app.domain.GoalStatus
import com.youjin.app.domain.GoalType
import com.youjin.app.domain.ProgressCalculator
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.ui.theme.GoalPalette
import com.youjin.app.util.formatNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 全局 Snackbar 宿主，由根 Scaffold 提供 */
val LocalSnackbarHost = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState 未提供")
}

/**
 * 栈式撤销提示：先关闭当前提示再显示新提示，
 * 保证“撤销”始终作用于最近一次操作（后操作的先撤销，LIFO）。
 * 注意：带操作按钮的 M3 Snackbar 默认时长为 Indefinite，这里显式指定 Short，
 * 让提示在约 4 秒后自动消失。
 */
fun CoroutineScope.showLifoUndoSnackbar(
    host: SnackbarHostState,
    message: String,
    onUndo: () -> Unit,
) {
    host.currentSnackbarData?.dismiss()
    launch {
        val result = host.showSnackbar(
            message = message,
            actionLabel = "撤销",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) onUndo()
    }
}

fun goalColor(index: Int): Color = GoalPalette[index.coerceIn(0, GoalPalette.lastIndex)]

fun goalEmoji(index: Int): String = GOAL_EMOJIS[index.coerceIn(0, GOAL_EMOJIS.lastIndex)]

/** 进度条：6dp 圆角细线，轨道浅灰，进度用目标强调色，变化带柔和动画 */
@Composable
fun YoujinProgressBar(
    ratio: Double,
    modifier: Modifier = Modifier,
    color: Color = AppleTheme.colors.blue,
) {
    val p = AppleTheme.colors
    val progress by animateFloatAsState(
        targetValue = ratio.toFloat().coerceIn(0f, 1f),
        animationSpec = AppleTheme.SoftSpring,
        label = "goalProgress",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(p.fill),
    ) {
        if (progress > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** 分组小节标题（iOS section header 风格） */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    AppleSectionHeader(text, modifier)
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val p = AppleTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(p.fill),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 30.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = p.secondaryLabel,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            AppleButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.width(180.dp),
                fillWidth = false,
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "确定",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppleDialog(
        title = title,
        message = text,
        confirmLabel = confirmLabel,
        destructive = destructive,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/** 状态必须有文字，不能只靠颜色：已暂停 / 已完成 / 已归档 / 已逾期 */
@Composable
fun StatusBadge(goal: Goal) {
    val p = AppleTheme.colors
    val (text, tone) = when {
        goal.status == GoalStatus.PAUSED -> "已暂停" to p.secondaryLabel
        goal.status == GoalStatus.COMPLETED -> "已完成" to p.green
        goal.status == GoalStatus.ARCHIVED -> "已归档" to p.secondaryLabel
        goal.isOverdue -> "已逾期" to p.red
        else -> return
    }
    Surface(
        color = tone.copy(alpha = if (p.isDark) 0.20f else 0.12f),
        contentColor = tone,
        shape = CircleShape,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

/** 目标卡片：进度、数值、下一步行动、状态标注；长按触发目标管理菜单 */
@Composable
fun GoalCard(
    goal: Goal,
    onOpen: () -> Unit,
    onRecord: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val p = AppleTheme.colors
    val color = goalColor(goal.colorIndex)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // 卡片按压时图标轻微放大，营造克制的纵深反馈
    val tileScale by animateFloatAsState(
        targetValue = if (pressed) 1.06f else 1f,
        animationSpec = AppleTheme.SoftSpring,
        label = "goalTileScale",
    )

    AppleCard(
        modifier = modifier,
        onClick = onOpen,
        onLongClick = onLongPress,
        interactionSource = source,
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.graphicsLayer {
                    scaleX = tileScale
                    scaleY = tileScale
                }
            ) {
                AppleIconTile(emoji = goal.iconEmoji(), tint = color)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(goal)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    goal.category ?: if (goal.type == GoalType.CUMULATIVE) "累计数值" else "步骤清单",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.secondaryLabel,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        YoujinProgressBar(goal.ratio, color = color)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (goal.type) {
                    GoalType.CUMULATIVE ->
                        "${formatNumber(goal.currentValue)} / ${formatNumber(goal.targetValue)} ${goal.unit}"
                    GoalType.CHECKLIST ->
                        "${goal.currentValue.toInt()} / ${goal.stepsTotal} 步骤"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = p.secondaryLabel,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${ProgressCalculator.percent(goal.ratio)}%",
                style = MaterialTheme.typography.titleMedium,
                color = p.label,
            )
        }

        if (goal.nextAction != null) {
            Spacer(Modifier.height(14.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "下一步",
                    style = MaterialTheme.typography.labelMedium,
                    color = p.tertiaryLabel,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    goal.nextAction!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = p.secondaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (goal.isOverdue) {
            Spacer(Modifier.height(10.dp))
            Text(
                "已逾期 · 仍可继续记录、改期或归档",
                style = MaterialTheme.typography.bodySmall,
                color = p.red,
            )
        }

        if (goal.reachedTarget() && goal.status == GoalStatus.ACTIVE) {
            Spacer(Modifier.height(14.dp))
            Surface(
                color = p.green.copy(alpha = if (p.isDark) 0.20f else 0.12f),
                contentColor = p.green,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    "已达成目标值，点击卡片确认完成",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        if (onRecord != null) {
            Spacer(Modifier.height(16.dp))
            AppleSecondaryButton(
                text = if (goal.type == GoalType.CUMULATIVE) "记录进展" else "勾选步骤",
                onClick = onRecord,
                modifier = Modifier.width(132.dp),
                height = 40.dp,
                fillWidth = false,
            )
        }
    }
}

/** 小圆点（列表行首的目标标识） */
@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun RowSpaced(
    spacing: Int = 8,
    content: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = { content() },
    )
}
