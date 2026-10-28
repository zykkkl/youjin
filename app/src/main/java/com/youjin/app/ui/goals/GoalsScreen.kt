package com.youjin.app.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youjin.app.ui.components.AppleChip
import com.youjin.app.ui.components.AppleLargeTitle
import com.youjin.app.ui.components.AppleSearchField
import com.youjin.app.ui.components.AppleTextAction
import com.youjin.app.ui.components.EmptyState
import com.youjin.app.ui.components.GoalCard
import com.youjin.app.ui.theme.AppleTheme

/** 目标列表页：搜索、状态筛选、排序 */
@Composable
fun GoalsScreen(
    vm: GoalsViewModel,
    onOpenGoal: (Long) -> Unit,
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var sortMenuOpen by remember { mutableStateOf(false) }
    val colors = AppleTheme.colors

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AppleLargeTitle(
            title = "目标",
            subtitle = "把想做的事拆成可以记录的进展",
        )

        AppleSearchField(
            value = state.query,
            onValueChange = vm::setQuery,
            placeholder = "搜索目标名称或分类",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )

        LazyRow(
            modifier = Modifier.padding(top = 14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(StatusFilter.entries) { filter ->
                AppleChip(
                    text = filter.label,
                    selected = state.filter == filter,
                    onClick = { vm.setFilter(filter) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "共 ${state.goals.size} 个目标",
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryLabel,
                modifier = Modifier.weight(1f),
            )
            Box {
                AppleTextAction(
                    text = "排序 · ${state.sort.label}",
                    onClick = { sortMenuOpen = true },
                )
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = colors.cardElevated,
                ) {
                    GoalSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    sort.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (sort == state.sort) colors.blue else colors.label,
                                )
                            },
                            onClick = {
                                vm.setSort(sort)
                                sortMenuOpen = false
                            },
                        )
                    }
                }
            }
        }

        if (state.goals.isEmpty()) {
            EmptyState(
                emoji = "🔍",
                title = "没有符合条件的目标",
                subtitle = "试试调整筛选或搜索关键词",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.goals, key = { it.id }) { goal ->
                    GoalCard(goal = goal, onOpen = { onOpenGoal(goal.id) })
                }
            }
        }
    }
}
