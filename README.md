<p align="center">
  <img src="./app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="200" alt="FuwaGit Logo" />
</p>

# <p align="center">FuwaGit</p>

</br>

<p align="center">
  <img alt="API" src="https://img.shields.io/badge/API%2026+-50f270?logo=android&logoColor=black&style=for-the-badge"/>
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-a503fc?logo=kotlin&logoColor=white&style=for-the-badge"/>
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white&style=for-the-badge"/>
  <img alt="Material You" src="https://custom-icon-badges.demolab.com/badge/Material%20You-lightblue?logo=material-you&logoColor=333&style=for-the-badge"/>
</p>

</br>

<div align="center">

# 🗺️ Overview

FuwaGit is a lightweight and powerful Git client for Android that brings full Git operations to your mobile device. Whether you're managing repositories, committing changes, or collaborating with others, FuwaGit provides a smooth and intuitive experience with secure credential management.

Built with modern Android technologies including Jetpack Compose and Material Design 3, FuwaGit offers a beautiful interface while maintaining robust functionality for developers on the go.

</div>


# 📲 Download

Go to the [Releases](https://github.com/JamGmilk/FuwaGit/releases/latest) and download the latest APK.

# 💻 Installation Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/JamGmilk/FuwaGit.git
   ```
2. Install dependencies using Gradle:
   ```bash
   ./gradlew build
   ```
3. The debug APK will be generated at:
   ```bash
   app/build/outputs/apk/debug/app-debug.apk
   ```

# ⚔️ Tech Stack

| Component | Technology |
|:----------|:----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Design System | Material Design 3 |
| Git Library | Eclipse JGit 6.8.0 |
| SSH | JSch |
| DI | Hilt |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 |

# ✨ Features

## Git Operations

- **Clone** - Clone remote repositories via HTTPS or SSH
- **Commit** - Stage changes and create commits with custom messages
- **Push** - Push commits to remote repositories
- **Pull** - Pull changes from remote with merge/rebase support
- **Fetch** - Fetch updates without merging
- **Branch Management** - Create, delete, rename, checkout branches
- **Merge** - Merge branches with conflict detection
- **Rebase** - Interactive rebase support
- **Tags** - Create, delete, and push tags (lightweight and annotated)
- **Reset** - Soft, mixed, and hard reset support
- **Diff** - View file differences with syntax highlighting

## Repository Management

- **Multi-Repo Support** - Manage multiple repositories simultaneously
- **Local Repos** - Add existing local Git repositories
- **Remote Cloning** - Clone from GitHub, GitLab, Bitbucket, or any Git server
- **Repository Info** - View detailed repository information
- **Clean** - Remove untracked files with preview

## Security

- **Master Password** - Encrypt all stored credentials with AES-256
- **Biometric Unlock** - Use fingerprint to quickly unlock credentials
- **Secure Storage** - Android Keystore-backed credential encryption
- **SSH Key Management** - Generate and store Ed25519/RSA SSH keys
- **HTTPS Credentials** - Securely store usernames and personal access tokens
- **Auto-Lock** - Configurable automatic vault locking

## User Experience

- **Material Design 3** - Modern and beautiful UI with dynamic theming
- **Dark/Light Mode** - System-following or manual theme selection
- **Multi-language** - English and Simplified Chinese support


# 📚 Dependencies

- [Eclipse JGit](https://www.eclipse.org/jgit/) - Pure Java implementation of Git
- [JSch](https://github.com/mwiede/jsch) - SSH2 protocol implementation
- [Bouncy Castle](https://www.bouncycastle.org/) - Cryptographic algorithms
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern declarative UI
- [Material 3](https://developer.android.com/compose/material3) - Material Design components
- [Hilt](https://dagger.dev/hilt/) - Dependency injection
- [Kotlinx Serialization](https://kotlinlang.org/docs/serialization.html) - JSON parsing


# ⚖️ License

```xml
Copyright 2024 JamGmilk

这里放 MIT 的
```
</div>
