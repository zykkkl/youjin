package com.youjin.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.youjin.app.data.ThemeMode
import com.youjin.app.data.UpdateScheduler
import com.youjin.app.ui.YoujinRoot
import com.youjin.app.ui.theme.YoujinTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** 通知点击带入的目标 ID（onNewIntent 时更新） */
    private var deepLinkGoalId by mutableStateOf<Long?>(null)

    /** 更新通知点击后要求打开设置页 */
    private var openSettings by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as YoujinApp).container
        deepLinkGoalId = intent?.getLongExtra(YoujinApp.EXTRA_GOAL_ID, -1L)?.takeIf { it > 0 }
        openSettings = intent?.getBooleanExtra(YoujinApp.EXTRA_OPEN_SETTINGS, false) == true

        // 按设置开关同步「自动检查更新」后台任务（默认开启）
        lifecycleScope.launch {
            container.settingsRepository.autoCheckUpdates
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) {
                        UpdateScheduler.schedule(this@MainActivity)
                    } else {
                        UpdateScheduler.cancel(this@MainActivity)
                    }
                }
        }

        setContent {
            val themeMode by container.settingsRepository.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM)
            YoujinTheme(themeMode = themeMode) {
                YoujinRoot(
                    container = container,
                    deepLinkGoalId = deepLinkGoalId,
                    onDeepLinkConsumed = { deepLinkGoalId = null },
                    openSettingsExtra = openSettings,
                    onSettingsConsumed = { openSettings = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = intent.getLongExtra(YoujinApp.EXTRA_GOAL_ID, -1L)
        if (id > 0) deepLinkGoalId = id
        if (intent.getBooleanExtra(YoujinApp.EXTRA_OPEN_SETTINGS, false)) {
            openSettings = true
        }
    }
}
