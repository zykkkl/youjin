package com.youjin.app.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youjin.app.data.ThemeMode
import com.youjin.app.data.UpdateScheduler
import com.youjin.app.ui.components.AppleDialog
import com.youjin.app.ui.components.AppleGroup
import com.youjin.app.ui.components.AppleNavBar
import com.youjin.app.ui.components.AppleRow
import com.youjin.app.ui.components.AppleSectionHeader
import com.youjin.app.ui.components.AppleSegmented
import com.youjin.app.ui.components.AppleSwitch
import com.youjin.app.ui.components.AppleTextAction
import com.youjin.app.ui.components.ConfirmDialog
import com.youjin.app.ui.components.Hairline
import com.youjin.app.ui.components.LocalSnackbarHost
import com.youjin.app.ui.theme.AppleTheme
import com.youjin.app.util.Dates
import kotlinx.coroutines.launch

/** 设置页：主题、减少动效、通知权限、数据备份恢复与导出、关于 */
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val reduceAnimations by vm.reduceAnimations.collectAsStateWithLifecycle()
    val updateState by vm.update.collectAsStateWithLifecycle()
    val autoCheckUpdates by vm.autoCheckUpdates.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = AppleTheme.colors
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }

    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    var notificationGranted by remember { mutableStateOf(hasNotificationPermission()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationGranted = hasNotificationPermission()
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            snackbar.showSnackbar(message)
        }
    }

    LaunchedEffect(updateState) {
        when (val s = updateState) {
            is UpdateUiState.NotConfigured -> {
                snackbar.showSnackbar("尚未配置更新源，检查更新未启用")
                vm.consumeUpdateState()
            }
            is UpdateUiState.UpToDate -> {
                snackbar.showSnackbar("已是最新版本 v${s.currentVersion}")
                vm.consumeUpdateState()
            }
            is UpdateUiState.Failed -> {
                snackbar.showSnackbar("检查更新失败：${s.message}")
                vm.consumeUpdateState()
            }
            else -> Unit
        }
    }

    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationGranted = hasNotificationPermission() }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportJson(it) } }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { vm.exportCsv(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { pendingImport = it } }

    Column(Modifier.fillMaxSize()) {
        AppleNavBar(title = "设置", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item { AppleSectionHeader("外观") }
            item {
                AppleGroup {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                        Text(
                            "主题",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.secondaryLabel,
                        )
                        Spacer(Modifier.height(10.dp))
                        AppleSegmented(
                            options = listOf("跟随系统", "浅色", "深色"),
                            selectedIndex = when (themeMode) {
                                ThemeMode.SYSTEM -> 0
                                ThemeMode.LIGHT -> 1
                                ThemeMode.DARK -> 2
                            },
                            onSelect = { index ->
                                vm.setThemeMode(
                                    when (index) {
                                        0 -> ThemeMode.SYSTEM
                                        1 -> ThemeMode.LIGHT
                                        else -> ThemeMode.DARK
                                    }
                                )
                            },
                        )
                    }
                    Hairline(startIndent = 18.dp)
                    AppleRow(
                        title = "减少动效",
                        subtitle = "关闭达成反馈等动画",
                        trailing = {
                            AppleSwitch(
                                checked = reduceAnimations,
                                onCheckedChange = vm::setReduceAnimations,
                            )
                        },
                    )
                }
            }

            item { AppleSectionHeader("提醒") }
            item {
                AppleGroup {
                    AppleRow(
                        title = "通知权限",
                        subtitle = if (notificationGranted) {
                            "已开启，提醒可以正常送达"
                        } else {
                            "未开启，提醒不会显示"
                        },
                        trailing = {
                            AppleTextAction(
                                text = if (notificationGranted) "系统设置" else "去开启",
                                onClick = {
                                    if (notificationGranted) {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    } else if (Build.VERSION.SDK_INT >= 33) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                            )
                        },
                    )
                }
            }

            item { AppleSectionHeader("数据") }
            item {
                AppleGroup {
                    AppleRow(
                        title = "导出 JSON 备份",
                        titleColor = colors.blue,
                        showChevron = true,
                        onClick = {
                            exportJsonLauncher.launch("youjin-backup-${Dates.todayEpochDay()}.json")
                        },
                    )
                    Hairline(startIndent = 18.dp)
                    AppleRow(
                        title = "从 JSON 备份恢复",
                        titleColor = colors.blue,
                        showChevron = true,
                        onClick = {
                            importLauncher.launch(
                                arrayOf("application/json", "text/plain", "application/octet-stream")
                            )
                        },
                    )
                    Hairline(startIndent = 18.dp)
                    AppleRow(
                        title = "导出 CSV 记录",
                        titleColor = colors.blue,
                        showChevron = true,
                        onClick = {
                            exportCsvLauncher.launch("youjin-records-${Dates.todayEpochDay()}.csv")
                        },
                    )
                }
            }
            item {
                Text(
                    "JSON 用于完整备份恢复，CSV 用于查看与分析。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryLabel,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                )
            }

            item { AppleSectionHeader("关于") }
            item {
                AppleGroup {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                        Text("有进", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "版本 v$versionName",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryLabel,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "把“我想做到”变成“我正在做到”。\n\n" +
                                "数据仅保存在本机（无需账号）。卸载或清除应用数据会丢失全部记录，请定期导出 JSON 备份。\n\n" +
                                "备份文件可能包含你的目标、备注等信息，请妥善保管，不要分享给不信任的人。",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryLabel,
                        )
                    }
                    Hairline(startIndent = 18.dp)
                    AppleRow(
                        title = "自动检查更新",
                        subtitle = "每天在后台检查一次，发现新版本时通知",
                        trailing = {
                            AppleSwitch(
                                checked = autoCheckUpdates,
                                onCheckedChange = { enabled ->
                                    vm.setAutoCheckUpdates(enabled)
                                    if (enabled) {
                                        UpdateScheduler.schedule(context)
                                    } else {
                                        UpdateScheduler.cancel(context)
                                    }
                                },
                            )
                        },
                    )
                    Hairline(startIndent = 18.dp)
                    AppleRow(
                        title = "检查更新",
                        subtitle = when {
                            updateState is UpdateUiState.Checking -> "正在检查…"
                            updateState is UpdateUiState.NotConfigured ->
                                "未配置更新源：部署 manifest JSON 并填入地址后可启用"
                            else -> "检查是否有新版本可用"
                        },
                        onClick = { vm.checkUpdate() },
                        showChevron = true,
                    )
                }
            }
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = "恢复备份？",
            text = "将用备份文件内容替换当前全部数据。恢复前会自动保留当前数据快照。",
            confirmLabel = "恢复",
            onConfirm = {
                vm.importJson(uri)
                pendingImport = null
                scope.launch { snackbar.showSnackbar("正在恢复…") }
            },
            onDismiss = { pendingImport = null },
        )
    }

    (updateState as? UpdateUiState.Available)?.let { s ->
        AppleDialog(
            title = "发现新版本 v${s.info.versionName}",
            message = s.info.notes.ifBlank { "包含常规改进与问题修复。" },
            confirmLabel = "去下载",
            dismissLabel = "稍后",
            onConfirm = {
                vm.consumeUpdateState()
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.info.apkUrl)))
                }.onFailure {
                    scope.launch {
                        snackbar.showSnackbar("无法打开下载链接，请手动复制地址下载")
                    }
                }
            },
            onDismiss = { vm.consumeUpdateState() },
        )
    }
}
