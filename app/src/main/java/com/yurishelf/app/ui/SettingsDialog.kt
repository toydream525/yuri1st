package com.yurishelf.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yurishelf.app.data.remote.ProxyMode
import com.yurishelf.app.data.remote.ProxySettings
import com.yurishelf.app.data.ai.AiSettings
import com.yurishelf.app.domain.ThemeMode

@Composable
fun SettingsDialog(
    settings: ProxySettings,
    themeMode: ThemeMode,
    nsfwEnabled: Boolean,
    hasAccessToken: Boolean,
    onDismiss: () -> Unit,
    onSave: (ProxySettings) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenAccessToken: () -> Unit,
    onNsfwEnabledChange: (Boolean) -> Unit,
    onForceRefresh: () -> Unit,
    onOpenBlocked: () -> Unit,
    onOpenBlockWords: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAiSettings: () -> Unit,
    aiSettings: AiSettings,
) {
    var modeName by rememberSaveable { mutableStateOf(settings.mode.name) }
    var host by rememberSaveable { mutableStateOf(settings.host) }
    var portText by rememberSaveable { mutableStateOf(settings.port.toString()) }
    val mode = runCatching { ProxyMode.valueOf(modeName) }.getOrDefault(ProxyMode.SYSTEM)
    val port = portText.toIntOrNull()
    val valid = mode == ProxyMode.SYSTEM || (host.isNotBlank() && port != null && port in 1..65535)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionLabel("外观")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { option ->
                        FilterChip(
                            selected = themeMode == option,
                            onClick = { onThemeModeChange(option) },
                            label = { Text(option.label) },
                        )
                    }
                }

                HorizontalDivider()

                SectionLabel("网络代理")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProxyMode.entries.forEach { option ->
                        FilterChip(
                            selected = mode == option,
                            onClick = { modeName = option.name },
                            label = { Text(option.label) },
                        )
                    }
                }
                if (mode != ProxyMode.SYSTEM) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("主机") },
                        supportingText = { Text("本机代理通常填写 127.0.0.1") },
                    )
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("端口") },
                    )
                }
                Text(
                    text = if (mode == ProxyMode.SYSTEM) {
                        "跟随 Android 系统代理或 VPN，一般应优先选择此项。"
                    } else {
                        "适用于 Clash、sing-box 等提供的 HTTP/SOCKS 监听端口；HTTPS 证书校验不会关闭。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )

                HorizontalDivider()

                SectionLabel("数据与隐私")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("显示 NSFW 内容", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (nsfwEnabled) {
                                "已开启：所有 API 请求使用你的 Access Token"
                            } else {
                                "已关闭：所有 API 请求使用官方公开访问"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = nsfwEnabled,
                        onCheckedChange = onNsfwEnabledChange,
                        enabled = hasAccessToken || nsfwEnabled,
                    )
                }
                if (!hasAccessToken) {
                    Text(
                        text = "请先配置 Access Token，之后才能开启 NSFW。NSFW 默认隐藏。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                OutlinedButton(
                    onClick = onForceRefresh,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("强制重新拉取数据库")
                }

                HorizontalDivider()

                SectionLabel("内容管理")
                SettingsActionRow(
                    title = "屏蔽管理",
                    subtitle = "查看并解除已屏蔽条目",
                    onClick = onOpenBlocked,
                )
                SettingsActionRow(
                    title = "屏蔽词",
                    subtitle = "按名称、标签和资料文字过滤",
                    onClick = onOpenBlockWords,
                )

                HorizontalDivider()

                SectionLabel("高级功能")
                SettingsActionRow(
                    title = "AI 百合倾向分析",
                    subtitle = if (aiSettings.hasApiKey) {
                        "已配置 ${aiSettings.model}（提示词可编辑）"
                    } else {
                        "配置 OpenAI 兼容接口、API Key 与自定义提示词"
                    },
                    onClick = onOpenAiSettings,
                )
                Text(
                    text = "Bangumi 点格子：在条目详情页把状态写入你的 Bangumi 账号，需要已配置的 Access Token。",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider()

                SectionLabel("账户与关于")
                SettingsActionRow(
                    title = "Bangumi Access Token",
                    subtitle = if (hasAccessToken) "已配置（可替换或移除）" else "可选，用于 NSFW 查询",
                    onClick = onOpenAccessToken,
                )
                SettingsActionRow(
                    title = "关于",
                    subtitle = "版本、许可证与友链",
                    onClick = onOpenAbout,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        ProxySettings(
                            mode = mode,
                            host = host.trim(),
                            port = port ?: settings.port,
                        ),
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
