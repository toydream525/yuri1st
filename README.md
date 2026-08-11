# yuri1st · Bangumi 百合作品集

[![Version](https://img.shields.io/github/v/release/toydream525/yuri1st?display_name=tag&sort=semver)](https://github.com/toydream525/yuri1st/releases/latest)
[![Android](https://img.shields.io/badge/platform-Android%208%2B-3DDC84?logo=android)](https://github.com/toydream525/yuri1st/releases/latest)
[![CI](https://github.com/toydream525/yuri1st/actions/workflows/build-apk.yml/badge.svg)](https://github.com/toydream525/yuri1st/actions/workflows/build-apk.yml)
[![License](https://img.shields.io/badge/license-MIT-9a3c72.svg)](LICENSE)
[![Website](https://img.shields.io/badge/website-项目主页-9a3c72.svg)](https://toydream525.github.io/yuri1st/)

面向 Bangumi 百合与轻百合标签条目的 Android 浏览器。yuri1st 将动画、漫画、轻小说和游戏集中整理，提供本地搜索、筛选、收藏、屏蔽与 Bangumi 点格子；可选的 AI 百合倾向分析用于辅助判断，始终仅供参考。

**数据来源**：作品目录与资料来自 [Bangumi](https://bgm.tv/) 公开接口及用户标签。应用仅按精确 `百合` / `轻百合` 标签收录，不代表 Bangumi 官方推荐，也无法保证内容判断完全准确。

<p align="center">
  <a href="https://github.com/toydream525/yuri1st/releases/latest"><strong>下载最新 APK</strong></a>
  · <a href="https://toydream525.github.io/yuri1st/">项目官网</a>
  · <a href="CHANGELOG.md">更新日志</a>
</p>

## 核心亮点

| 能力 | 说明 |
| --- | --- |
| 百合目录浏览 | 动画、漫画、轻小说、游戏独立浏览；支持本地搜索、Bangumi 链接或 ID 导入、分页与离线基础条目。 |
| 三态布局与筛选 | 左上角排版按钮可在列表、两列封面、四列密集间切换；高级筛选支持分类、年份、评分人数、AI/自定义标记及反选。 |
| AI 百合倾向分析 | 使用用户配置的 OpenAI 兼容接口分析真百、轻百、非百倾向；判断偏好以自然语言呈现，可手动分类或恢复跟随 AI。首页仅非百显示提示标记。 |
| Bangumi 点格子 | 将想看、看过、在看、搁置、抛弃同步至自己的 Bangumi 账号，支持单条与批量操作，并自动限速。 |
| 本地优先与隐私 | 收藏、屏蔽、屏蔽词、赢面/输面和分析结果保存在设备本地；NSFW 默认关闭。 |

## v0.4.0

- AI 百合倾向分析改用可读、可编辑的“判断偏好”，不再向用户展示技术 JSON。
- 扩大真百 / 轻百的默认判断范围，支持手动设为真百、轻百或非百；首页仅非百显示提示标记。
- 高级筛选支持反选；排版按钮新增四列密集布局，现可在三种布局间循环切换。
- Room 数据库升级至版本 7，兼容手动分类数据。

完整变更请见 [CHANGELOG.md](CHANGELOG.md)。

## 安装与更新

- **系统要求**：Android 8.0（API 26）及以上。
- 从 [GitHub Releases](https://github.com/toydream525/yuri1st/releases/latest) 下载最新正式 APK，允许系统安装来自此来源的应用后打开即可。
- **从 v0.3 调试签名版升级**：若系统提示签名不一致，需先卸载旧调试版，再安装 v0.4.0 正式版。卸载会清除旧版本地数据（如收藏、设置与缓存），请先确认需要保留的内容。

## 隐私与安全

- Bangumi Access Token 与 AI API Key 使用 Android Keystore 加密保存，不写入仓库、日志或备份文件。
- 本地收藏、屏蔽、筛选和 AI 缓存数据保留在设备数据库中；卸载应用会按 Android 系统规则移除应用数据。
- AI 分析只在你主动触发时调用你配置的兼容接口。模型可能出错或带有偏差，结果仅用于辅助浏览，不应替代个人判断。
- Bangumi 官方接口不支持删除收藏；点格子只能切换五种收藏状态。

## 截图

| 目录信息流 | 详情与百合倾向分析 |
| --- | --- |
| ![yuri1st 目录信息流，展示 AI 百合倾向与自定义标记](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/01-main-list.jpg) | ![yuri1st 条目详情页，展示 Bangumi 点格子与 AI 百合倾向分析](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/02-detail-ai-pointgrid.jpg) |

| 批量点格子 | AI 百合倾向分析设置 | 本页批量分析 |
| --- | --- | --- |
| ![yuri1st 批量点格子操作界面](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/03-batch-pointgrid.jpg) | ![yuri1st AI 百合倾向分析设置界面](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/04-ai-settings.jpg) | ![yuri1st 本页批量 AI 百合倾向分析界面](https://cdn.jsdelivr.net/gh/toydream525/yuri1st@main/docs/screenshots/05-batch-ai.jpg) |

## 文档

- [更新日志](CHANGELOG.md)：版本变更与兼容性说明
- [完整功能说明](docs/FEATURES.md)：目录、筛选、AI 与点格子使用方式
- [开发与构建说明](docs/DEVELOPMENT.md)：技术栈、数据流程、配置与已知边界
- [项目官网](https://toydream525.github.io/yuri1st/)：产品介绍与当前版本更新摘要

## 开发构建

环境：JDK 17、Android SDK 35、项目内置 Gradle Wrapper。

```bash
./gradlew testDebugUnitTest assembleDebug
```

正式构建的 User-Agent 配置、数据流程和更多开发约定请见 [开发与构建说明](docs/DEVELOPMENT.md)。

## 许可证与声明

本项目以 [MIT License](LICENSE) 开源。运行时依赖与其许可证见 [第三方组件清单](THIRD_PARTY_NOTICES.md)。作品数据版权归 Bangumi 及各条目权利方所有。
