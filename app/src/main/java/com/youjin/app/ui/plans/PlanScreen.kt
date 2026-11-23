package com.youjin.app.ui.plans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.youjin.app.domain.Goal
import com.youjin.app.domain.PlanItem
import com.youjin.app.domain.PlanQuadrant
import com.youjin.app.domain.planFraction
import com.youjin.app.domain.planValue
import com.youjin.app.ui.components.AppleButton
import com.youjin.app.ui.components.AppleCard
import com.youjin.app.ui.components.AppleCheckCircle
import com.youjin.app.ui.components.AppleChip
import com.youjin.app.ui.components.AppleGroup
import com.youjin.app.ui.components.AppleIconButton
import com.youjin.app.ui.components.AppleLargeTitle
import com.youjin.app.ui.components.AppleSectionHeader
import com.youjin.app.ui.components.AppleSheetHandle
import com.youjin.app.ui.components.AppleTextAction
import com.youjin.app.ui.components.AppleTextField
import com.youjin.app.ui.components.EmptyState
import com.youjin.app.ui.components.LocalSnackbarHost
import com.youjin.app.ui.components.appleRowClick
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.ui.theme.GoalPalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 计划页：一周一张四象限图 + 一张清单。
 * 未完成的事项以圆点落在「重要性 × 紧急性」坐标系里，圆点可以拖动；
 * 勾选完成后圆点消失，只留在清单里。
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(vm: PlanViewModel) {
    val state by vm.uiState.collectAsState()
    val editor by vm.editor.collectAsState()
    val dragging by vm.dragging.collectAsState()
    val host = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val p = AppleTheme.colors

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            scope.launch { host.showSnackbar(message) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        AppleLargeTitle(
            title = "计划",
            subtitle = if (state.isCurrentWeek) "${state.weekLabel} · 本周" else state.weekLabel,
            trailing = {
                Row {
                    AppleIconButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上一周",
                        onClick = vm::prevWeek,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                    AppleIconButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下一周",
                        onClick = vm::nextWeek,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                }
            },
        )

        if (!state.isCurrentWeek) {
            Row(Modifier.padding(horizontal = 20.dp)) {
                AppleTextAction("回到本周", vm::goThisWeek)
            }
        }

        AppleSectionHeader(
            text = "四象限",
            trailing = {
                Text(
                    text = "拖圆点调位置",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.tertiaryLabel,
                )
            },
        )

        AppleCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        ) {
            EisenhowerMatrix(
                items = state.pending,
                goalsById = state.goalsById,
                overrides = dragging,
                onOpen = vm::openEdit,
                onDrag = vm::previewDrag,
                onDragEnd = vm::commitDrag,
            )
        }

        AppleSectionHeader(
            text = if (state.doneCount > 0) "本周计划 · 已完成 ${state.doneCount}/${state.items.size}" else "本周计划",
            trailing = { AppleTextAction("＋ 添加", vm::openAdd) },
        )

        if (state.pending.isEmpty() && state.completed.isEmpty()) {
            EmptyState(
                emoji = "🗓️",
                title = "这一周还没有计划",
                subtitle = "把要做的事写下来，再标上重要和紧急的程度。",
                actionLabel = "添加计划",
                onAction = vm::openAdd,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            if (state.pending.isNotEmpty()) {
                AppleGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
                    state.pending.forEach { item ->
                        PlanRow(
                            item = item,
                            goal = item.goalId?.let { state.goalsById[it] },
                            onOpen = { vm.openEdit(item) },
                            onToggle = { vm.toggleDone(item) },
                        )
                    }
                }
            }

            if (state.completed.isNotEmpty()) {
                AppleSectionHeader(text = "已完成", modifier = Modifier.padding(horizontal = 4.dp))
                AppleGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
                    state.completed.forEach { item ->
                        PlanRow(
                            item = item,
                            goal = item.goalId?.let { state.goalsById[it] },
                            onOpen = { vm.openEdit(item) },
                            onToggle = { vm.toggleDone(item) },
                        )
                    }
                }
            }
        }
    }

    editor?.let { draft ->
        val goals = vm.linkableGoals(state.goals)
        ModalBottomSheet(
            onDismissRequest = vm::closeEditor,
            containerColor = p.background,
            dragHandle = { AppleSheetHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Text(
                    text = if (draft.isEdit) "编辑计划" else "添加计划",
                    style = MaterialTheme.typography.titleMedium,
                    color = p.label,
                )
                Spacer(Modifier.height(12.dp))
                AppleTextField(
                    value = draft.title,
                    onValueChange = vm::setEditorTitle,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "这周要做什么",
                    singleLine = false,
                    minLines = 2,
                )

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "关联目标（可选）",
                    style = MaterialTheme.typography.labelMedium,
                    color = p.secondaryLabel,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppleChip(
                        text = "不关联",
                        selected = draft.goalId == null,
                        onClick = { vm.setEditorGoal(null) },
                    )
                    goals.forEach { goal ->
                        AppleChip(
                            text = "${goal.iconEmoji()} ${goal.name}",
                            selected = draft.goalId == goal.id,
                            onClick = { vm.setEditorGoal(goal.id) },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                PlanSlider(
                    label = "重要性",
                    hint = "越高越重要",
                    value = draft.importance,
                    onChange = vm::setEditorImportance,
                )
                Spacer(Modifier.height(14.dp))
                PlanSlider(
                    label = "紧急性",
                    hint = "越高越紧急",
                    value = draft.urgency,
                    onChange = vm::setEditorUrgency,
                )

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = draft.quadrant.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.label,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = draft.quadrant.advice,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (draft.quadrant == PlanQuadrant.DO_NOW) p.blue else p.secondaryLabel,
                    )
                }

                Spacer(Modifier.height(22.dp))
                AppleButton(
                    text = "保存",
                    onClick = vm::saveEditor,
                    enabled = draft.canSave,
                    fillWidth = true,
                )
                if (draft.isEdit) {
                    Spacer(Modifier.height(10.dp))
                    AppleButton(
                        text = "删除计划",
                        onClick = vm::deleteEditorItem,
                        destructive = true,
                        fillWidth = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanRow(
    item: PlanItem,
    goal: Goal?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val p = AppleTheme.colors
    val done = item.done
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleRowClick(onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleCheckCircle(
            checked = done,
            onCheckedChange = { onToggle() },
            contentDescription = if (done) "取消完成" else "标记完成",
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) p.secondaryLabel else p.label,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (goal != null) {
                    Text(
                        text = "${goal.iconEmoji()} ${goal.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = p.tertiaryLabel,
                    )
                }
                if (!done) {
                    Text(
                        text = item.quadrant.advice,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.quadrant == PlanQuadrant.DO_NOW) p.blue else p.tertiaryLabel,
                    )
                }
            }
        }
    }
}

/** 重要性 × 紧急性四象限。x 轴紧急性、y 轴重要性，圆点可拖动 */
@Composable
private fun EisenhowerMatrix(
    items: List<PlanItem>,
    goalsById: Map<Long, Goal>,
    overrides: Map<Long, Pair<Int, Int>>,
    onOpen: (PlanItem) -> Unit,
    onDrag: (Long, Int, Int) -> Unit,
    onDragEnd: (PlanItem) -> Unit,
) {
    val p = AppleTheme.colors
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(14.dp)),
    ) {
        val plotW = with(density) { maxWidth.toPx() }
        val plotH = with(density) { maxHeight.toPx() }
        val edge = with(density) { 14.dp.toPx() }

        Canvas(Modifier.fillMaxSize()) {
            val halfW = size.width / 2f
            val halfH = size.height / 2f
            // 右上：重要且紧急（唯一的强调色，克制使用）
            drawRect(
                color = p.blue.copy(alpha = 0.07f),
                topLeft = Offset(halfW, 0f),
                size = Size(halfW, halfH),
            )
            drawRect(
                color = p.fill.copy(alpha = 0.5f),
                topLeft = Offset(0f, 0f),
                size = Size(halfW, halfH),
            )
            drawRect(
                color = p.fill.copy(alpha = 0.3f),
                topLeft = Offset(halfW, halfH),
                size = Size(halfW, halfH),
            )
            drawLine(p.separator, Offset(halfW, 0f), Offset(halfW, size.height), 1.dp.toPx())
            drawLine(p.separator, Offset(0f, halfH), Offset(size.width, halfH), 1.dp.toPx())
        }

        Text(
            text = "重要且紧急 · 立即做",
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            style = MaterialTheme.typography.labelSmall,
            color = p.blue,
        )
        Text(
            text = "重要不紧急 · 排期做",
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            style = MaterialTheme.typography.labelSmall,
            color = p.secondaryLabel,
        )
        Text(
            text = "紧急不重要 · 尽快处理",
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
            style = MaterialTheme.typography.labelSmall,
            color = p.secondaryLabel,
        )
        Text(
            text = "都不重要 · 先不做",
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
            style = MaterialTheme.typography.labelSmall,
            color = p.tertiaryLabel,
        )

        Text(
            text = "紧急性",
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 30.dp),
            style = MaterialTheme.typography.labelSmall,
            color = p.tertiaryLabel,
        )
        Text(
            text = "重要性",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 30.dp)
                .rotate(-90f),
            style = MaterialTheme.typography.labelSmall,
            color = p.tertiaryLabel,
        )

        if (items.isEmpty()) {
            Text(
                text = "本周没有待办，点「＋ 添加」写一条",
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = p.tertiaryLabel,
            )
        }

        items.forEach { item ->
            val position = overrides[item.id] ?: (item.importance to item.urgency)
            val importance = position.first
            val urgency = position.second
            val x = edge + planFraction(urgency) * (plotW - edge * 2)
            val y = edge + (1f - planFraction(importance)) * (plotH - edge * 2)
            val goal = item.goalId?.let { goalsById[it] }
            val dotColor = goal?.let { GoalPalette[it.colorIndex.coerceIn(0, GoalPalette.lastIndex)] } ?: p.blue

            Box(
                modifier = Modifier
                    .offset { IntOffset((x - 11.dp.toPx()).roundToInt(), (y - 11.dp.toPx()).roundToInt()) }
                    .size(22.dp)
                    .pointerInput(item.id) {
                        detectTapGestures { onOpen(item) }
                    }
                    .pointerInput(item.id, item.importance, item.urgency, plotW, plotH) {
                        var accumulated = Offset.Zero
                        val start = Offset(
                            planFraction(item.urgency) * (plotW - edge * 2) + edge,
                            (1f - planFraction(item.importance)) * (plotH - edge * 2) + edge,
                        )
                        detectDragGestures(
                            onDragStart = { accumulated = Offset.Zero },
                            onDragEnd = { onDragEnd(item) },
                            onDragCancel = { onDragEnd(item) },
                        ) { change, dragAmount ->
                            change.consume()
                            accumulated += dragAmount
                            val nextX = (start.x + accumulated.x).coerceIn(edge, plotW - edge)
                            val nextY = (start.y + accumulated.y).coerceIn(edge, plotH - edge)
                            onDrag(
                                item.id,
                                planValue(1f - (nextY - edge) / (plotH - edge * 2)),
                                planValue((nextX - edge) / (plotW - edge * 2)),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(color = p.card, radius = size.minDimension / 2f)
                    drawCircle(color = dotColor, radius = size.minDimension / 2f - 1.5.dp.toPx())
                }
            }
        }
    }
}

/** 1..10 的滑杆，Apple 风格：细轨道 + 蓝色填充 + 圆形滑块 */
@Composable
private fun PlanSlider(
    label: String,
    hint: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    val p = AppleTheme.colors
    val density = LocalDensity.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.label,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = p.tertiaryLabel,
                )
            }
            Text(
                text = "$value / 10",
                style = MaterialTheme.typography.bodyMedium,
                color = p.blue,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val pad = with(density) { 12.dp.toPx() }
                        val usable = (size.width - pad * 2).coerceAtLeast(1f)
                        onChange(planValue((offset.x - pad) / usable))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val pad = with(density) { 12.dp.toPx() }
                        val usable = (size.width - pad * 2).coerceAtLeast(1f)
                        onChange(planValue((change.position.x - pad) / usable))
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val pad = 12.dp.toPx()
                val centerY = size.height / 2f
                val usable = (size.width - pad * 2).coerceAtLeast(1f)
                val thumbX = pad + planFraction(value) * usable

                drawLine(
                    color = p.fill,
                    start = Offset(pad, centerY),
                    end = Offset(size.width - pad, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = p.blue,
                    start = Offset(pad, centerY),
                    end = Offset(thumbX, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(color = p.card, radius = 11.dp.toPx(), center = Offset(thumbX, centerY))
                drawCircle(
                    color = p.blue,
                    radius = 11.dp.toPx(),
                    center = Offset(thumbX, centerY),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}
