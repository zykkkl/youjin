package com.youjin.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.youjin.app.di.AppContainer
import com.youjin.app.ui.components.AppleFloatingButton
import com.youjin.app.ui.components.AppleSnackbarHost
import com.youjin.app.ui.components.AppleTab
import com.youjin.app.ui.components.AppleTabBar
import com.youjin.app.ui.components.LocalSnackbarHost
import com.youjin.app.ui.goals.GoalDetailScreen
import com.youjin.app.ui.goals.GoalDetailViewModel
import com.youjin.app.ui.goals.GoalEditScreen
import com.youjin.app.ui.goals.GoalEditViewModel
import com.youjin.app.ui.goals.GoalsScreen
import com.youjin.app.ui.goals.GoalsViewModel
import com.youjin.app.ui.review.ReviewScreen
import com.youjin.app.ui.review.ReviewViewModel
import com.youjin.app.ui.settings.SettingsScreen
import com.youjin.app.ui.settings.SettingsViewModel
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.ui.today.TodayScreen
import com.youjin.app.ui.today.TodayViewModel

/** 页面路由。GoalEdit 携带唯一 navId，保证每次新建/编辑获得全新的 ViewModel 状态 */
sealed interface Route {
    data object Today : Route
    data object Goals : Route
    data object Review : Route
    data object Settings : Route
    data class GoalDetail(val goalId: Long) : Route
    data class GoalEdit(val goalId: Long?, val navId: Long) : Route
}

@Composable
fun YoujinRoot(
    container: AppContainer,
    deepLinkGoalId: Long?,
    onDeepLinkConsumed: () -> Unit,
    openSettingsExtra: Boolean = false,
    onSettingsConsumed: () -> Unit = {},
) {
    val backStack = remember { mutableStateListOf<Route>(Route.Today) }
    val current = backStack.lastOrNull() ?: Route.Today
    // 必须是 Compose 状态：普通变量会在每次重组时重置，导致复用旧 ViewModel（新建页秒退）
    var editNavCounter by remember { mutableStateOf(0L) }
    val colors = AppleTheme.colors

    fun navigate(route: Route) {
        backStack.add(route)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun switchTab(route: Route) {
        backStack.clear()
        backStack.add(route)
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    LaunchedEffect(deepLinkGoalId) {
        if (deepLinkGoalId != null && deepLinkGoalId > 0) {
            navigate(Route.GoalDetail(deepLinkGoalId))
            onDeepLinkConsumed()
        }
    }

    // 更新通知点击 -> 直接打开设置页（可手动检查更新）
    LaunchedEffect(openSettingsExtra) {
        if (openSettingsExtra) {
            navigate(Route.Settings)
            onSettingsConsumed()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val tabs = remember {
        listOf(
            AppleTab("今天", Icons.Default.DateRange),
            AppleTab("目标", Icons.AutoMirrored.Filled.List),
            AppleTab("回顾", Icons.Default.Star),
        )
    }
    val tabIndex = when (current) {
        is Route.Today -> 0
        is Route.Goals -> 1
        is Route.Review -> 2
        else -> -1
    }
    val showFab = current is Route.Today || current is Route.Goals

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { AppleSnackbarHost(snackbarHostState) },
        bottomBar = {
            if (tabIndex >= 0) {
                AppleTabBar(
                    tabs = tabs,
                    selectedIndex = tabIndex,
                    onSelect = { index ->
                        switchTab(
                            when (index) {
                                0 -> Route.Today
                                1 -> Route.Goals
                                else -> Route.Review
                            }
                        )
                    },
                )
            }
        },
        floatingActionButton = {
            if (showFab) {
                AppleFloatingButton(
                    onClick = { navigate(Route.GoalEdit(null, ++editNavCounter)) },
                    contentDescription = "创建目标",
                    icon = Icons.Default.Add,
                    modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                )
            }
        },
    ) { padding ->
        CompositionLocalProvider(LocalSnackbarHost provides snackbarHostState) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (current) {
                    is Route.Today -> {
                        val vm: TodayViewModel = viewModel(
                            factory = viewModelFactory {
                                initializer { TodayViewModel(container.goalRepository) }
                            }
                        )
                        TodayScreen(
                            vm = vm,
                            onCreateGoal = { navigate(Route.GoalEdit(null, ++editNavCounter)) },
                            onOpenGoal = { navigate(Route.GoalDetail(it)) },
                            onOpenSettings = { navigate(Route.Settings) },
                        )
                    }

                    is Route.Goals -> {
                        val vm: GoalsViewModel = viewModel(
                            factory = viewModelFactory {
                                initializer { GoalsViewModel(container.goalRepository) }
                            }
                        )
                        GoalsScreen(vm = vm, onOpenGoal = { navigate(Route.GoalDetail(it)) })
                    }

                    is Route.Review -> {
                        val vm: ReviewViewModel = viewModel(
                            factory = viewModelFactory {
                                initializer { ReviewViewModel(container.reviewRepository) }
                            }
                        )
                        ReviewScreen(vm)
                    }

                    is Route.Settings -> {
                        val vm: SettingsViewModel = viewModel(
                            factory = viewModelFactory {
                                initializer {
                                    SettingsViewModel(
                                        container.settingsRepository,
                                        container.backupManager,
                                        container.updateChecker,
                                    )
                                }
                            }
                        )
                        SettingsScreen(vm = vm, onBack = ::pop)
                    }

                    is Route.GoalDetail -> {
                        val goalId = current.goalId
                        val vm: GoalDetailViewModel = viewModel(
                            key = "detail-$goalId",
                            factory = viewModelFactory {
                                initializer { GoalDetailViewModel(goalId, container.goalRepository) }
                            },
                        )
                        GoalDetailScreen(
                            goalId = goalId,
                            vm = vm,
                            onBack = ::pop,
                            onEditGoal = { navigate(Route.GoalEdit(it, ++editNavCounter)) },
                        )
                    }

                    is Route.GoalEdit -> {
                        val goalId = current.goalId
                        val navId = current.navId
                        val vm: GoalEditViewModel = viewModel(
                            key = "edit-$navId",
                            factory = viewModelFactory {
                                initializer { GoalEditViewModel(goalId, container.goalRepository) }
                            },
                        )
                        GoalEditScreen(
                            goalId = goalId,
                            vm = vm,
                            onBack = ::pop,
                            onSaved = { pop() },
                        )
                    }
                }
            }
        }
    }
}
