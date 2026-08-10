# yuri1st

一个专注百合作品的 Android 应用：整理 Bangumi 上的百合 / 轻百合动画、漫画、轻小说和游戏，用 AI 判断作品雷点，并一键同步你的 Bangumi 收藏状态。

📦 下载 Android APK：[yuri1st v0.3.0](https://github.com/toydream525/yuri1st/releases/latest)（当前为调试签名测试版）

## 功能介绍

### AI 雷点分析

- 标准 OpenAI 兼容接口：支持 OpenAI、DeepSeek、Moonshot、自建端点等
- 读取作品资料（标题、简介、标签、基本资料、评分），可选联网检索
- 输出真百 / 轻百 / 非百、置信度、判断理由与雷点列表，标注“仅供参考”
- 提示词可自由编辑；结果按条目缓存，列表和封面直接显示 AI 徽章
- 支持一键分析本页全部作品，实时进度、可随时取消

### Bangumi 点格子

- 把想看 / 看过 / 在看 / 搁置 / 抛弃写入你的 Bangumi 账号收藏
- 详情页单条点格子，列表与封面显示状态徽章
- 批量点格子：可选当前页或全部分类，自动限速避免触发频率限制，可随时取消

### 百合目录

- 动画 / 漫画 / 轻小说 / 游戏分类浏览
- 列表 / 封面双视图，支持搜索与多条件筛选（年份、评分人数、TV / OVA / 剧场版 / 其他等）
- 按 AI 判断的真百 / 轻百 / 非百筛选，也支持自定义“赢面 / 输面”标记与筛选

### 本地与隐私

- 本地收藏、屏蔽、随机推荐；NSFW 默认关闭
- Bangumi Token 与 AI API Key 使用 Android Keystore 加密保存，不读取评论区

## 截图

| 信息流 | 详情页：点格子 + AI 分析 |
| --- | --- |
| ![信息流](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/01-main-list.jpg) | ![详情页](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/02-detail-ai-pointgrid.jpg) |

| 批量点格子 | AI 设置 | 本页 AI 分析 |
| --- | --- | --- |
| ![批量点格子](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/03-batch-pointgrid.jpg) | ![AI 设置](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/04-ai-settings.jpg) | ![本页 AI 分析](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/05-batch-ai.jpg) |
