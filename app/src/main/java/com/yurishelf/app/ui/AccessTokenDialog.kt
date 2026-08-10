package com.yurishelf.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AccessTokenDialog(
    hasToken: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var token by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bangumi 授权") },
        text = {
            Column {
                Text(
                    text = if (hasToken) {
                        "已配置 Access Token。输入新 Token 可替换；应用不会显示现有值。"
                    } else {
                        "请在 Bangumi 官方页面生成 Access Token 后粘贴。不要在此输入账号密码。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = { uriHandler.openUri(ACCESS_TOKEN_URL) },
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Text("打开官方 Access Token 页面")
                }
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrectEnabled = false,
                    ),
                    label = { Text("Access Token") },
                    supportingText = { Text("使用 Android Keystore AES-GCM 加密保存") },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = token.isNotBlank(),
                onClick = { onSave(token) },
            ) { Text(if (hasToken) "替换" else "保存") }
        },
        dismissButton = {
            if (hasToken) {
                TextButton(onClick = { onSave(null) }) { Text("移除 Token") }
            } else {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

private const val ACCESS_TOKEN_URL = "https://next.bgm.tv/demo/access-token"
