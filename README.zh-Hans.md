<div align="right">

[English](README.md) | **简体中文**

</div>

<div align="center">

<img src="./app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="192" alt="FuwaGit 图标" />

# FuwaGit

![API](https://img.shields.io/badge/API%2026+-50f270?logo=android&logoColor=black&style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-a503fc?logo=kotlin&logoColor=white&style=for-the-badge)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white&style=for-the-badge)
![Material You](https://custom-icon-badges.demolab.com/badge/Material%20You-lightblue?logo=material-you&logoColor=333&style=for-the-badge)

FuwaGit 是一款轻量且强大的 Android 端 Git 客户端。基于 Jetpack Compose 和 Material Design 3 等现代技术构建，致力于为移动端的开发者提供优雅且流畅的交互体验。

*Fuwa (ふわ): 轻盈、蓬松 ~*

---
</div>

## 截图

|                                                        |                                                        |                                                        |
|:------------------------------------------------------:|:------------------------------------------------------:|:------------------------------------------------------:|
| <img src="screenshots/Screenshot_1.jpg" width="300" /> | <img src="screenshots/Screenshot_2.jpg" width="300" /> | <img src="screenshots/Screenshot_6.jpg" width="300" /> |

## 下载

这里喵~ [Releases](https://github.com/JamGmilk/FuwaGit/releases/latest)

## 🛠技术栈

| 组件 | 技术 |
|:----------|:----------|
| **编程语言** | Kotlin |
| **UI 框架** | Jetpack Compose |
| **设计系统** | Material Design 3 |
| **Git 核心库** | Eclipse JGit |
| **SSH 协议** | JSch |
| **依赖注入** | Hilt |
| **最低支持版本** | API 26 (Android 8.0) |

## 功能特性

### Git 操作
- **完整生命周期**: 支持 Clone (HTTPS/SSH)、Commit、Push、Pull 和 Fetch。
- **分支管理**: 轻松创建、删除、重命名及切换分支。
- **高级功能**: 支持带有冲突检测的 Merge (合并)、交互式 Rebase (变基) 及 Tag (标签) 管理。
- **重置功能**: 支持 Soft、Mixed 和 Hard Reset (回退)。
- **差异查看**: 内置支持语法高亮的代码 Diff 查看器。

### 安全与体验
- **生物识别锁定**: 支持通过指纹或面部识别保护您的 Git 凭据。
- **AES-256 加密**: 存储凭据均通过 Android Keystore 支持的加密算法加密。
- **SSH 密钥生成**: 支持直接在应用内生成 Ed25519 或 RSA 密钥。
- **动态配色**: 完美适配 Material You 动态主题及深色模式。

## 许可协议
    Copyright 2026 JamGmilk
    MIT License
