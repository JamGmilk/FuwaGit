# FuwaGit 🐱

<p align="center">
  <strong>A Powerful Android Git Client</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?style=flat-square&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=flat-square&logo=android" alt="Compose">
  <img src="https://img.shields.io/badge/JGit-6.8.0-FF6B6B?style=flat-square" alt="JGit">
  <img src="https://img.shields.io/badge/API-26%2B-green?style=flat-square&logo=android" alt="API Level">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License">
</p>

---

## ✨ Introduction

**FuwaGit** is a native Android Git client application built with a modern technology stack, providing comprehensive Git version control functionality. The app supports local repository management, remote repository operations, secure credential storage, and biometric authentication — making it an ideal tool for mobile developers and Git users.

## 🎯 Core Features

### 📦 Repository Management
- **Initialize Repositories** - Create new Git repositories in any directory
- **Clone Repositories** - Clone remote repositories via HTTPS or SSH protocols
- **Multi-repository Management** - Manage multiple local Git repositories with quick switching
- **Remote Configuration** - Add, edit, and remove remote repository URLs
- **Repository Information** - Display detailed repository metadata (path, branches, user config, etc.)

### 🔧 Git Operations
- **Status View** - Real-time display of workspace file change status (modified, staged, untracked, etc.)
- **Staging Management** - Selectively stage or unstage files
- **Commit Changes** - Create commits with commit messages
- **Branch Operations**
  - Create, switch, and delete branches
  - View all local and remote branches
- **Sync Operations**
  - Push to remote repositories
  - Pull remote updates
  - Fetch remote references
- **History** - Browse commit history with detailed commit info and file change diffs
- **Merge & Reset** - Support for branch merging and workspace reset operations
- **Clean Workspace** - Remove untracked files

### 🔐 Secure Credential System
- **Master Password Protection** - AES-256-GCM encryption protects all credential data
- **HTTPS Credentials** - Securely store usernames and passwords for GitHub/GitLab platforms
- **SSH Key Management** - Support SSH private key import and management (including passphrase-protected keys)
- **PBKDF2 Key Derivation** - Strong key derivation algorithm with 100,000 iterations
- **Android Keystore** - Leverage hardware-level security modules to protect encryption keys

### 👆 Biometric Support
- **Fingerprint Unlock** - Quick access to credential vault using fingerprint without entering password each time
- **Secure Integration** - Biometric seamlessly integrated with master password system
- **Flexible Switching** - Switch between biometric and password unlock at any time

### ⚙️ Settings & Customization
- **Dark Mode** - Support for light, dark, and follow system theme modes
- **Git Configuration** - Customize global username, email, and default branch name
- **Password Hint** - Set master password hints to prevent forgetting

## 📱 Screenshots

> Screenshots coming soon

## 📖 User Guide

### First Time Use
1. **Launch the app** - First launch will request storage permission
2. **Set up master password** - Create a strong password to protect your credentials
3. **Add a repository** - Select a local Git repository directory or clone a remote repository
4. **Configure credentials** (optional) - If you need push/pull functionality, add corresponding HTTPS or SSH credentials

### Daily Operations
- **Bottom navigation bar** switches between five main features:
  - 📋 Status - View workspace status and file changes
  - 📜 History - Browse commit history
  - 🌳 Branches - Manage branches
  - 📁 My Repos - Manage multiple repositories
  - ⚙️ Settings - App settings and credential management

### Enable Fingerprint Unlock
1. Go to Settings → Credentials
2. Tap "Enable Fingerprint"
3. Verify identity with fingerprint
4. You can now quickly unlock the credential vault using fingerprint

## 🐛 Issue Reporting

If you find a bug or have a feature suggestion, please submit it on [GitHub Issues](https://github.com/YOUR_USERNAME/FuwaGit/issues).

When submitting an issue, please include:
- Description of the issue and steps to reproduce
- Device model and Android version
- Application version number
- Relevant log information (if available)

## 📄 License

This project is licensed under the [MIT License](LICENSE).

```
MIT License

Copyright (c) 2025 jamgmilk

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 🙏 Acknowledgments

- [Eclipse JGit](https://www.eclipse.org/jgit/) - Powerful pure Java Git implementation
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [Dagger Hilt](https://dagger.dev/hilt/) - Excellent dependency injection framework
- [Material Design 3](https://m3.material.io/) - Google's design system

## 📧 Contact

- **Author**: jamgmilk
- **Email**: (To be filled)

---

<p align="center">
  <strong>⭐ If this project helps you, please give it a Star! ⭐</strong>
</p>

<p align="center">
  Made with ❤️ by <a href="https://github.com/YOUR_USERNAME">jamgmilk</a>
</p>
