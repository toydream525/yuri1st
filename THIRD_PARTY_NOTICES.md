# 第三方组件清单

本项目运行时使用以下开源组件。除特别注明外，许可证均为 Apache License 2.0；完整许可证文本以各组件官方发布为准。

## 运行时组件

| 组件 | 版本 | 用途 | 许可证 |
| --- | --- | --- | --- |
| Kotlin 标准库 | 2.1.20 | 语言与标准库 | Apache-2.0 |
| kotlinx-coroutines-android | 1.10.1 | 协程与异步任务 | Apache-2.0 |
| kotlinx-serialization-json | 1.8.0 | JSON 序列化 | Apache-2.0 |
| AndroidX Core KTX | 1.15.0 | Android 基础扩展 | Apache-2.0 |
| AndroidX Activity Compose | 1.10.1 | Activity 与 Compose 集成 | Apache-2.0 |
| AndroidX Lifecycle | 2.8.7 | 生命周期与 ViewModel | Apache-2.0 |
| Jetpack Compose BOM | 2025.03.01 | Compose UI、Foundation、Material 3 | Apache-2.0 |
| Material Icons Extended | 随 Compose BOM | 界面图标（列表/封面切换等） | Apache-2.0 |
| AndroidX Room | 2.7.0 | 本地数据库 | Apache-2.0 |
| Retrofit | 2.11.0 | HTTP 接口封装 | Apache-2.0 |
| converter-kotlinx-serialization | 2.11.0 | Retrofit 的 JSON 适配器 | Apache-2.0 |
| OkHttp | 4.12.0 | HTTP 客户端 | Apache-2.0 |
| OkHttp logging-interceptor | 4.12.0 | 网络请求日志 | Apache-2.0 |
| Coil | 2.7.0 | 封面图片加载 | Apache-2.0 |

## 测试组件

| 组件 | 版本 | 用途 | 许可证 |
| --- | --- | --- | --- |
| JUnit | 4.13.2 | 单元测试 | EPL-2.0 |
| AndroidX Test Ext JUnit | 1.2.1 | 仪器测试支持 | Apache-2.0 |
| AndroidX Espresso | 3.6.1 | UI 测试 | Apache-2.0 |
| Compose UI Test | 随 Compose BOM | Compose UI 测试 | Apache-2.0 |

## 构建组件

| 组件 | 版本 | 用途 | 许可证 |
| --- | --- | --- | --- |
| Android Gradle Plugin | 8.9.2 | Android 构建 | Apache-2.0 |
| Kotlin Gradle Plugin | 2.1.20 | Kotlin 编译 | Apache-2.0 |
| Kotlin Compose Plugin | 2.1.20 | Compose 编译器 | Apache-2.0 |
| KSP | 2.1.20-1.0.32 | 注解处理（Room） | Apache-2.0 |

本清单基于当前构建配置生成；升级依赖后请同步更新。条目数据来自 Bangumi 公开接口，数据版权归 Bangumi 及各条目权利方所有，不属于上述软件组件。
