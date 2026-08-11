# yuri1st · Bangumi 百合作品集

一个浏览 Bangumi 百合 / 轻百合作品集的 Android 应用：把动画、漫画、轻小说和游戏整理到一起，支持搜索、筛选、收藏和屏蔽。

📦 下载 Android APK：[yuri1st v0.4.0](https://github.com/toydream525/yuri1st/releases/latest)

## v0.4.0 更新摘要

- AI 百合倾向分析改用可读的“判断偏好”设置，不再向用户展示技术 JSON。
- 扩大真百 / 轻百的默认判断范围，支持手动调整真百、轻百或非百；首页仅非百显示提示标记。
- 高级筛选支持反选；左上角排版按钮可在列表、两列封面与四列密集布局间切换。

完整变更请见 [CHANGELOG.md](CHANGELOG.md)。

## 基础功能

### 百合作品集

- 动画 / 漫画 / 轻小说 / 游戏分类浏览，数据来自 Bangumi 的百合 / 轻百合标签
- 列表、两列封面、四列密集三种布局，左上角排版按钮一键切换
- 搜索：名称、别名、平台、标签、原作/Staff 等；支持粘贴 Bangumi 链接或 ID 导入
- 筛选：年份、最低评分人数、TV / OVA / 剧场版 / 其他、仅收藏、仅 NSFW，并支持反选结果
- 每页 20 条，支持输入页码跳转

### 本地功能

- 本地收藏、屏蔽与屏蔽词、随机推荐
- 自定义“赢面 / 输面”标记与筛选
- 内置基础条目可离线浏览；普通刷新累加新条目，强制刷新更新全部
- NSFW 默认关闭；Bangumi Token 与 AI Key 使用 Android Keystore 加密保存

## 高级功能

### AI 百合倾向分析

使用 OpenAI 兼容接口分析真百 / 轻百 / 非百倾向，结果仅供参考；支持单条与本页批量分析、手动分类和自然语言判断偏好。

### Bangumi 点格子

把你的 Bangumi 收藏状态同步进应用：

- 状态：想看 / 看过 / 在看 / 搁置 / 抛弃，写入你的 Bangumi 账号
- 详情页单条点格子，打开详情页自动同步当前状态；列表与封面显示状态徽章
- 批量点格子：左上角清单图标，可选“当前页”或“当前分类全部条目”，自动跳过已标记条目
- 自动限速（约 1.2 秒/条）避免触发频率限制，实时进度、可随时取消
- 需要先在“设置”中配置 Bangumi Access Token；官方接口不支持删除收藏，只能切换五种状态

## 截图

| 信息流 | 详情页 |
| --- | --- |
| ![信息流](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/01-main-list.jpg) | ![详情页](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/02-detail-ai-pointgrid.jpg) |

| 批量点格子 | AI 设置 | 本页 AI 分析 |
| --- | --- | --- |
| ![批量点格子](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/03-batch-pointgrid.jpg) | ![AI 设置](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/04-ai-settings.jpg) | ![本页 AI 分析](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/05-batch-ai.jpg) |
