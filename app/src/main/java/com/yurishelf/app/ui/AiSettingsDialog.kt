package com.yurishelf.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yurishelf.app.data.ai.AiSettings
import com.yurishelf.app.data.ai.DEFAULT_AI_BASE_URL
import com.yurishelf.app.data.ai.DEFAULT_AI_MODEL
import com.yurishelf.app.data.ai.DEFAULT_AI_PROMPT

@Composable
fun AiSettingsDialog(
    settings: AiSettings,
    onDismiss: () -> Unit,
    onSave: (AiSettings, String?, Boolean) -> Unit,
) {
    var baseUrl by rememberSaveable(settings.baseUrl) { mutableStateOf(settings.baseUrl) }
    var model by rememberSaveable(settings.model) { mutableStateOf(settings.model) }
    var prompt by rememberSaveable(settings.prompt) { mutableStateOf(settings.prompt) }
    var webSearch by rememberSaveable(settings.webSearchEnabled) {
        mutableStateOf(settings.webSearchEnabled)
    }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var removeApiKey by rememberSaveable { mutableStateOf(false) }

    val baseUrlValid = baseUrl.trim().startsWith("https://") ||
        baseUrl.trim().startsWith("http://")
    val valid = baseUrlValid && model.isNotBlank() && prompt.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 雷点分析") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "使用标准 OpenAI 兼容接口判断百合程度和雷点。结果由 AI 生成，仅供参考，不构成对作品的评价。",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("接口地址 Base URL") },
                    supportingText = { Text("默认 $DEFAULT_AI_BASE_URL；兼容 DeepSeek、Moonshot、自建服务等") },
                    isError = baseUrl.isNotBlank() && !baseUrlValid,
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("模型名称") },
                    supportingText = { Text("默认 $DEFAULT_AI_MODEL") },
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        removeApiKey = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (settings.hasApiKey) "API Key（留空保持不变）" else "API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        if (settings.hasApiKey) {
                            TextButton(onClick = {
                                removeApiKey = true
                                apiKey = ""
                            }) {
                                Text(if (removeApiKey) "将移除" else "移除")
                            }
                        }
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("联网搜索作品信息", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                        Text(
                            "仅 OpenAI 官方端点使用联网搜索工具；其他兼容端点使用模型自身知识。",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = webSearch,
                        onCheckedChange = { webSearch = it },
                    )
                }
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    minLines = 8,
                    maxLines = 12,
                    label = { Text("提示词") },
                    supportingText = { Text("可以按自己的判断标准编辑；恢复默认会覆盖当前内容。") },
                )
                TextButton(
                    onClick = { prompt = DEFAULT_AI_PROMPT },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("恢复默认提示词")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        AiSettings(
                            baseUrl = baseUrl.trim(),
                            model = model.trim(),
                            prompt = prompt,
                            webSearchEnabled = webSearch,
                            hasApiKey = settings.hasApiKey && !removeApiKey || apiKey.isNotBlank(),
                        ),
                        apiKey.takeIf { it.isNotBlank() },
                        removeApiKey,
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
