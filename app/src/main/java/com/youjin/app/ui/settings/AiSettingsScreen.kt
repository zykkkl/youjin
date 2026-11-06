package com.youjin.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.youjin.app.data.ai.AiProviders
import com.youjin.app.ui.components.AppleButton
import com.youjin.app.ui.components.AppleCard
import com.youjin.app.ui.components.AppleChip
import com.youjin.app.ui.components.AppleDialog
import com.youjin.app.ui.components.AppleGroup
import com.youjin.app.ui.components.AppleNavBar
import com.youjin.app.ui.components.AppleRow
import com.youjin.app.ui.components.AppleSecondaryButton
import com.youjin.app.ui.components.AppleSectionHeader
import com.youjin.app.ui.components.AppleSwitch
import com.youjin.app.ui.components.AppleTextField
import com.youjin.app.ui.components.AppleTextAction
import com.youjin.app.ui.components.Hairline
import com.youjin.app.ui.components.LocalSnackbarHost
import com.youjin.app.ui.theme.AppleTheme

/**
 * AI 助手设置：BYOK（用户自带 Key）、OpenAI 兼容协议、可换服务商。
 * 页面只负责编辑与测试，AI 结果由业务页面确认后才落到数据上。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSettingsScreen(vm: AiSettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val colors = AppleTheme.colors
    val snackbar = LocalSnackbarHost.current

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    Column(Modifier.fillMaxSize()) {
        AppleNavBar(title = "AI 助手", onBack = onBack)

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppleCard {
                    Text(
                        text = "AI 是可选功能：只有在你自己配置好 Key 并点击相关按钮时，应用才会把目标名称、进度记录等必要内容发送给所选服务商。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryLabel,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Key 加密保存在本机，应用没有任何中转服务器；随时可以关闭。",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.tertiaryLabel,
                    )
                }
            }

            item { AppleSectionHeader(text = "服务商") }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AiProviders.builtin.forEach { template ->
                        AppleChip(
                            text = template.displayName,
                            selected = state.providerId == template.id,
                            onClick = { vm.selectProvider(template.id) },
                        )
                    }
                }
            }

            item {
                state.providerNote?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryLabel,
                    )
                }
            }

            item { AppleSectionHeader(text = "接口与模型") }

            item {
                AppleGroup {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                        FieldLabel("接口地址（Base URL）")
                        AppleTextField(
                            value = state.baseUrl,
                            onValueChange = vm::setBaseUrl,
                            placeholder = "https://api.openai.com/v1",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        )

                        Spacer(Modifier.height(16.dp))
                        FieldLabel("模型")
                        AppleTextField(
                            value = state.model,
                            onValueChange = vm::setModel,
                            placeholder = "例如 gpt-4o-mini",
                        )

                        val models = (state.suggestedModels + state.fetchedModels).distinct()
                        if (models.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                models.take(24).forEach { name ->
                                    AppleChip(
                                        text = name,
                                        selected = state.model == name,
                                        onClick = { vm.setModel(name) },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AppleTextAction(
                                text = "拉取可用模型",
                                onClick = vm::fetchModels,
                                enabled = !state.fetchingModels,
                            )
                            if (state.fetchingModels) {
                                Text(
                                    text = "拉取中…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.secondaryLabel,
                                )
                            }
                        }

                        Hairline()

                        Spacer(Modifier.height(14.dp))
                        FieldLabel(if (state.keyOptional) "API Key（本地服务可留空）" else "API Key")
                        AppleTextField(
                            value = state.apiKey,
                            onValueChange = vm::setApiKey,
                            placeholder = "服务商控制台里的 Key",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Key 只用于请求上面这个地址，不会上传到任何中转服务。",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.tertiaryLabel,
                        )
                    }
                }
            }

            item { AppleSectionHeader(text = "启用") }

            item {
                AppleGroup {
                    AppleRow(
                        title = "启用 AI 助手",
                        subtitle = if (state.enabled) {
                            "已启用 · ${state.providerName}${if (state.model.isBlank()) "" else " · ${state.model}"}"
                        } else {
                            "启用前会先说明数据会发往哪里"
                        },
                        trailing = {
                            AppleSwitch(checked = state.enabled, onCheckedChange = vm::requestEnable)
                        },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppleButton(
                        text = "保存配置",
                        onClick = vm::save,
                        modifier = Modifier.weight(1f),
                        enabled = state.canSave,
                        loading = state.saving,
                    )
                    AppleSecondaryButton(
                        text = "测试连接",
                        onClick = vm::test,
                        modifier = Modifier.weight(1f),
                        enabled = state.canSave && !state.testing,
                    )
                }
            }

            item {
                state.testMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.testPassed) colors.green else colors.red,
                    )
                }
            }

            item {
                Text(
                    text = "本地 Ollama：先在电脑上启动 Ollama 并允许局域网访问，地址填电脑的局域网 IP，例如 http://192.168.1.2:11434/v1 ，模型填 qwen2.5:7b 这类已拉取的模型名。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.tertiaryLabel,
                )
            }
        }
    }

    if (state.showPrivacyDialog) {
        AppleDialog(
            title = "启用 AI 助手？",
            message = "启用后，当你主动点击「AI 帮我拆解」或「AI 生成复盘」时，应用会把目标名称、进度记录等必要内容发送给 ${state.providerName}（${AiProviders.normalizeBaseUrl(state.baseUrl)}）处理。\n\nAPI Key 只加密保存在本机，不经过任何中转服务器。你可以随时关闭 AI 助手。",
            confirmLabel = "同意并启用",
            dismissLabel = "再想想",
            onConfirm = vm::confirmPrivacyAndEnable,
            onDismiss = vm::dismissPrivacyDialog,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = AppleTheme.colors.secondaryLabel,
    )
    Spacer(Modifier.height(6.dp))
}
