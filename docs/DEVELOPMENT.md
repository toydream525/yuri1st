# 开发与构建说明

## 技术栈

- Kotlin 2.1.20 / Jetpack Compose（Material 3）/ AndroidX Room 2.7.0
- Retrofit + OkHttp + kotlinx-serialization
- Coil 图片加载

## 构建

- Android Studio
- JDK 17
- Android SDK 35
- Gradle 8.11.1（项目 Wrapper 会自动下载）

```bash
./gradlew test
./gradlew assembleDebug
BANGUMI_USER_AGENT='yuri1st/0.3.0 (开发者标识; https://github.com/toydream525/yuri1st)' \
  ./gradlew assembleRelease
```

Debug 构建内置开发版 User-Agent；Release 构建强制要求提供正式 User-Agent，缺少时会构建失败。

## Bangumi User-Agent（开发者标识）

Bangumi 官方要求 API 请求携带可联系的 User-Agent，格式为：

```text
应用名/版本 (开发者标识; 项目主页)
```

开发者标识不需要提交申请，直接使用你的 bgm.tv 用户名即可。如需注册第三方 OAuth 应用，可在 bgm.tv/dev/app 创建；本应用只使用个人 Access Token，不依赖 OAuth client_secret。

## 数据流程

1. `POST /v0/search/subjects` 分别查询动画、漫画、轻小说和游戏，合并 `tag: ["百合"]` 与 `tag: ["轻百合"]` 结果；漫画额外要求 `meta_tags: ["漫画"]`，轻小说要求 `meta_tags: ["小说"]`。
2. NSFW 关闭时使用不带 Token 的公开 API；开启时统一携带 Access Token，分别请求 `nsfw: false` 与 `true` 并去重合并。
3. 客户端只接收响应标签中确实含有本次请求精确标签且满足目录元标签的条目。
4. 首次同步逐页建立 Room 目录；普通刷新只累加新条目；“强制重新拉取”会重写全部命中条目。
5. 打开详情时调用 `GET /v0/subjects/{id}` 补全资料；UI 观察 Room，建库过程可逐步看到结果。
6. 打开详情时会同步一次你的点格子状态（`GET /v0/me` 缓存用户名，再读取 `GET /v0/users/{username}/collections/{subject_id}`）；写入使用 `POST /v0/users/-/collections/{subject_id}`（官方文档“不存在则创建，存在则修改”）。
7. AI 分析由用户主动触发：先重新读取条目资料，再请求配置的兼容端点。OpenAI 官方端点使用 `/responses` + `web_search` 联网搜索；其他兼容端点使用 `/chat/completions`。结果以 JSON 解析后写入 Room 并按条目缓存。

Bangumi 将条目搜索标为实验性 API。网络层、DTO 和仓库层已分开，接口变化不会直接侵入 UI。

## 代理设置

“设置”中可选：

- 跟随系统：使用 Android 系统代理或 VPN
- HTTP：适合 Clash、sing-box 等 HTTP 监听端口（如 `127.0.0.1:7890`）
- SOCKS：地址以 unresolved socket 交给代理

切换后自动刷新连接；应用不关闭 HTTPS 证书或主机名校验，也不保存代理用户名/密码。

## AI 雷点分析配置

“设置 → 高级功能 → AI 雷点分析”：

- 接口地址：默认 `https://api.openai.com/v1`，兼容 DeepSeek、Moonshot、自建服务等
- API Key：Android Keystore 加密保存，不写日志或备份
- 模型名称：默认 `gpt-4o-mini`
- 联网搜索：仅 OpenAI 官方端点启用 `web_search` 工具
- 提示词：可自由编辑或恢复默认；要求模型只输出 JSON，并包含真百 / 轻百 / 非百判定规则、雷点列举规则和低置信度约束

## 已知边界

- `百合` / `轻百合` 是 Bangumi 用户标签，应用只做精确收录，不保证内容判断完全准确。
- 官方搜索会把请求页大小限制为 20，部分标签查询可分页总数封顶 1000；达到上限或分页元数据异常时会提示部分结果。
- 当前同步为前台触发与启动时过期检查；周期后台同步可在后续加入 WorkManager。
- Bangumi 官方 v0 接口不提供删除收藏端点，因此点格子不提供“取消收藏”，只能切换五种状态。
- 点格子状态在打开详情页时同步一次并缓存在本地；网页端修改后需重新打开详情页才会刷新。
- AI 判断可能出错或带有模型偏见，结果仅供参考。
- 当前未实现带用户名/密码的代理认证。

## 开源与许可证

- 源码仓库：[github.com/toydream525/yuri1st](https://github.com/toydream525/yuri1st)
- 许可证：MIT（见 [LICENSE](../LICENSE)）
- 第三方组件：见 [THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES.md)
- 条目数据来自 Bangumi 公开接口，数据版权归 Bangumi 及各条目权利方所有
