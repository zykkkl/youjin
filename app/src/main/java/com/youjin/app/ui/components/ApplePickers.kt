package com.youjin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.youjin.app.ui.theme.AppleTheme
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

/**
 * Apple 风格的日期选择：白色圆角弹窗 + 月份切换 + 月历网格。
 * 完全使用应用自己的配色与圆角，避免 Material DatePicker 的视觉混搭。
 */
@Composable
fun AppleDatePickerDialog(
    title: String,
    initialEpochDay: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false,
    onClear: (() -> Unit)? = null,
) {
    val colors = AppleTheme.colors
    val today = remember { LocalDate.now() }
    var selected by remember(initialEpochDay) {
        mutableStateOf(initialEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today)
    }
    var visibleMonth by remember(initialEpochDay) { mutableStateOf(YearMonth.from(selected)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 380.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.cardElevated,
            tonalElevation = 0.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (allowClear && onClear != null) {
                        AppleTextAction(
                            text = "清除",
                            onClick = onClear,
                            color = colors.red,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppleIconButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "上个月",
                        onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                        tint = colors.blue,
                        size = 40.dp,
                        iconSize = 24.dp,
                    )
                    Text(
                        "${visibleMonth.year}年${visibleMonth.monthValue}月",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    AppleIconButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "下个月",
                        onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                        tint = colors.blue,
                        size = 40.dp,
                        iconSize = 24.dp,
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.tertiaryLabel,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                val firstDay = visibleMonth.atDay(1)
                // 周日起始：DayOfWeek.value 周一=1 … 周日=7
                val startOffset = firstDay.dayOfWeek.value % 7
                val dayCount = visibleMonth.lengthOfMonth()
                Column {
                    for (week in 0 until 6) {
                        Row(Modifier.fillMaxWidth()) {
                            for (weekday in 0 until 7) {
                                val dayOfMonth = week * 7 + weekday - startOffset + 1
                                if (dayOfMonth in 1..dayCount) {
                                    val date = visibleMonth.atDay(dayOfMonth)
                                    DayCell(
                                        day = dayOfMonth,
                                        selected = date == selected,
                                        isToday = date == today,
                                        onClick = { selected = date },
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppleSecondaryButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        fillWidth = false,
                        height = 46.dp,
                    )
                    AppleButton(
                        text = "确定",
                        onClick = { onConfirm(selected.toEpochDay()) },
                        modifier = Modifier.weight(1f),
                        fillWidth = false,
                        height = 46.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppleTheme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier.aspectRatio(1f).padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pressScale(source, 0.88f)
                .clip(CircleShape)
                .background(if (selected) colors.blue else Color.Transparent)
                .clickable(interactionSource = source, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    selected -> Color.White
                    isToday -> colors.blue
                    else -> colors.label
                },
            )
        }
    }
}

/** Apple 风格的时间选择：双列滚轮，带吸附与居中高亮 */
@Composable
fun AppleTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    title: String = "选择时间",
) {
    val colors = AppleTheme.colors
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    AppleDialog(
        title = title,
        confirmLabel = "确定",
        onConfirm = { onConfirm(hour, minute) },
        onDismiss = onDismiss,
        content = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelColumn(
                    items = (0..23).map { "%02d".format(it) },
                    selectedIndex = hour,
                    onSelected = { hour = it },
                    modifier = Modifier.width(92.dp),
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.secondaryLabel,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                WheelColumn(
                    items = (0..59).map { "%02d".format(it) },
                    selectedIndex = minute,
                    onSelected = { minute = it },
                    modifier = Modifier.width(92.dp),
                )
            }
        },
    )
}

@Composable
private fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppleTheme.colors
    val scope = rememberCoroutineScope()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val centerIndex by remember { derivedStateOf { state.firstVisibleItemIndex } }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to state.firstVisibleItemIndex }
            .collect { (scrolling, index) ->
                if (!scrolling) {
                    val target = index.coerceIn(0, items.lastIndex)
                    if (target != selectedIndex) onSelected(target)
                }
            }
    }

    Box(modifier.height(220.dp)) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.fill)
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = 88.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(items) { index, label ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { scope.launch { state.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (index == centerIndex) colors.label else colors.tertiaryLabel,
                    )
                }
            }
        }
    }
}
