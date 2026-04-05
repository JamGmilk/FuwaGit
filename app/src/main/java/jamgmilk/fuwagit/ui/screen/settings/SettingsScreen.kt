package jamgmilk.fuwagit.ui.screen.settings

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.ui.components.FilePickerDialog
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.screen.credentials.ChangeMasterPasswordDialog
import jamgmilk.fuwagit.ui.screen.credentials.CredentialStoreViewModel
import jamgmilk.fuwagit.ui.screen.credentials.SetupMasterPasswordDialog
import jamgmilk.fuwagit.ui.screen.credentials.UnlockDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "SettingsScreen"
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToCredentials: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    credentialsViewModel: CredentialStoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val credentialsUiState by credentialsViewModel.uiState.collectAsStateWithLifecycle()
    val applyResult = settingsUiState.applyResult

    var showFilePicker by rememberSaveable { mutableStateOf(false) }
    var pendingBiometricEnable by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(applyResult) {
        applyResult?.let {
        }
    }

    LaunchedEffect(credentialsUiState.error) {
        credentialsUiState.error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            credentialsViewModel.clearError()
        }
    }

    LaunchedEffect(credentialsUiState.isDecryptionUnlocked, pendingBiometricEnable) {
        Log.d(TAG, "LaunchedEffect: isDecryptionUnlocked=${credentialsUiState.isDecryptionUnlocked}, pendingBiometricEnable=$pendingBiometricEnable")
        if (credentialsUiState.isDecryptionUnlocked && pendingBiometricEnable) {
            Log.d(TAG, "LaunchedEffect: calling enableBiometric, activity=$activity")
            if (activity == null) {
                Log.e(TAG, "LaunchedEffect: activity is NULL, cannot enable biometric")
            }
            delay(100)
            pendingBiometricEnable = false
            activity?.let { credentialsViewModel.enableBiometric(it) }
        }
    }

    ScreenTemplate(
        title = "Settings",
        modifier = modifier
    ) {
        BetaWarningCard(modifier = Modifier.fillMaxWidth())

        StorageSettingsCard(
            onPermissionsClick = onNavigateToPermissions,
            modifier = Modifier.fillMaxWidth()
        )

        GlobalConfigCard(
            userName = settingsUiState.userName,
            userEmail = settingsUiState.userEmail,
            defaultBranch = settingsUiState.defaultBranch,
            applyResult = applyResult,
            onUserConfigSave = { name, email -> settingsViewModel.saveUserConfig(name, email) },
            onDefaultBranchSave = { settingsViewModel.saveDefaultBranch(it) },
            onReload = { settingsViewModel.reloadUserConfig() },
            onApplyToAllRepos = { name, email, alsoToGlobal ->
                settingsViewModel.applyConfigToAllRepos(name, email, alsoToGlobal)
            },
            onClearApplyResult = { settingsViewModel.clearApplyResult() },
            modifier = Modifier.fillMaxWidth()
        )

        SecuritySettingsCard(
            onCredentialsClick = onNavigateToCredentials,
            onMasterPasswordClick = {
                credentialsViewModel.showChangePasswordDialog()
            },
            biometricEnabled = credentialsUiState.isBiometricEnabled,
            isDecryptionUnlocked = credentialsUiState.isDecryptionUnlocked,
            isMasterPasswordSet = credentialsUiState.isMasterPasswordSet,
            onBiometricEnabledChange = { enabled ->
                Log.d(TAG, "Switch toggled: enabled=$enabled, isDecryptionUnlocked=${credentialsUiState.isDecryptionUnlocked}, activity=$activity")
                if (enabled) {
                    if (!credentialsUiState.isDecryptionUnlocked) {
                        Log.d(TAG, "Enabling biometric but locked, showing unlock dialog")
                        pendingBiometricEnable = true
                        credentialsViewModel.showUnlockDialog()
                    } else {
                        Log.d(TAG, "Calling enableBiometric directly")
                        activity?.let { credentialsViewModel.enableBiometric(it) }
                    }
                } else {
                    if (!credentialsUiState.isDecryptionUnlocked && credentialsUiState.isBiometricEnabled) {
                        Log.d(TAG, "Already enabled but locked, showing unlock dialog to unlock vault")
                        credentialsViewModel.showUnlockDialog()
                    } else {
                        Log.d(TAG, "Disabling biometric")
                        credentialsViewModel.disableBiometric()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        AppearanceSettingsCard(
            darkMode = settingsUiState.darkMode,
            onDarkModeChange = { mode -> settingsViewModel.saveDarkMode(mode) },
            modifier = Modifier.fillMaxWidth()
        )

        SyncSettingsCard(
            autoSync = settingsUiState.autoSync,
            onAutoSyncChange = { settingsViewModel.saveAutoSync(it) },
            conflictSafeMode = settingsUiState.conflictSafeMode,
            onConflictSafeModeChange = { settingsViewModel.saveConflictSafeMode(it) },
            backupBeforeSync = settingsUiState.backupBeforeSync,
            onBackupBeforeSyncChange = { settingsViewModel.saveBackupBeforeSync(it) },
            modifier = Modifier.fillMaxWidth()
        )

        DeveloperOptionsCard(
            verboseLogging = settingsUiState.verboseLogging,
            onVerboseLoggingChange = { settingsViewModel.saveVerboseLogging(it) },
            onTestFilePicker = { showFilePicker = true },
            modifier = Modifier.fillMaxWidth()
        )

        AboutCard(
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showFilePicker) {
        FilePickerDialog(
            title = "Test File Picker",
            onDismiss = { showFilePicker = false },
            onSelect = { showFilePicker = false }
        )
    }

    if (credentialsUiState.showUnlockDialog) {
        UnlockDialog(
            onDismiss = {
                pendingBiometricEnable = false
                credentialsViewModel.dismissUnlockDialog()
            },
            onUnlock = { password ->
                credentialsViewModel.unlockWithPassword(password)
            },
            biometricEnabled = credentialsUiState.isBiometricEnabled,
            onUnlockWithBiometric = {
                activity?.let { credentialsViewModel.unlockWithBiometric(it) }
            },
            passwordHint = credentialsUiState.passwordHint,
            error = credentialsUiState.error,
            isLoading = credentialsUiState.isLoading
        )
    }

    if (credentialsUiState.showChangePasswordDialog) {
        if (credentialsUiState.isMasterPasswordSet) {
            ChangeMasterPasswordDialog(
                onDismiss = { credentialsViewModel.dismissChangePasswordDialog() },
                onConfirm = { oldPassword, newPassword, confirmPassword, hint ->
                    credentialsViewModel.changeMasterPassword(oldPassword, newPassword, confirmPassword, hint)
                },
                passwordHint = credentialsUiState.passwordHint,
                error = credentialsUiState.changePasswordError,
                isLoading = credentialsUiState.isLoading
            )
        } else {
            SetupMasterPasswordDialog(
                onDismiss = { credentialsViewModel.dismissChangePasswordDialog() },
                onConfirm = { password, confirmPassword, hint ->
                    credentialsViewModel.setupMasterPassword(password, confirmPassword, hint)
                },
                error = credentialsUiState.changePasswordError,
                isLoading = credentialsUiState.isLoading
            )
        }
    }
}

@Composable
private fun BetaWarningCard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val warningOrange = Color(0xFFFF9800)
    val warningBackground = if (isDark) {
        lerp(warningOrange, colors.surface, 0.85f)
    } else {
        lerp(warningOrange, Color.White, 0.9f)
    }
    val contentOrange = if (isDark) Color(0xFFFFB74D) else warningOrange

    ElevatedCard(
        modifier = modifier.border(
            width = 1.dp,
            color = contentOrange.copy(alpha = if (isDark) 0.3f else 0.2f),
            shape = RoundedCornerShape(24.dp)
        ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = warningBackground
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Beta Version Notice",
                icon = Icons.Default.Warning,
                color = contentOrange
            )

            HorizontalDivider(color = contentOrange.copy(alpha = 0.1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = contentOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Features may be unstable or change.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = contentOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Important: Backup your repositories and credentials.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageSettingsCard(
    onPermissionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Storage",
                icon = Icons.Default.Storage,
                color = colors.primary
            )

            SettingsNavigationItem(
                title = "Permissions",
                subtitle = "Manage storage access permissions",
                icon = Icons.Default.Security,
                onClick = onPermissionsClick
            )
        }
    }
}

@Composable
private fun SecuritySettingsCard(
    modifier: Modifier = Modifier,
    onCredentialsClick: () -> Unit,
    onMasterPasswordClick: () -> Unit,
    biometricEnabled: Boolean = false,
    isDecryptionUnlocked: Boolean = false,
    isMasterPasswordSet: Boolean = false,
    onBiometricEnabledChange: ((Boolean) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Security",
                icon = Icons.Default.Shield,
                color = colors.tertiary
            )

            SettingsNavigationItem(
                title = "Credentials",
                subtitle = "HTTPS passwords & SSH keys",
                icon = Icons.Default.Key,
                onClick = onCredentialsClick
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableItem(
                title = if (isMasterPasswordSet) "Change Master Password" else "Set Master Password",
                subtitle = if (isMasterPasswordSet) "Update your master password" else "Protect your credentials",
                icon = Icons.Default.Lock,
                onClick = onMasterPasswordClick
            )

            if (isMasterPasswordSet && onBiometricEnabledChange != null) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    title = "Biometric Unlock",
                    subtitle = when {
                        !isDecryptionUnlocked -> "Tap to unlock credentials first"
                        biometricEnabled -> "Enabled"
                        else -> "Use fingerprint to unlock"
                    },
                    icon = Icons.Default.Fingerprint,
                    checked = biometricEnabled,
                    onCheckedChange = { onBiometricEnabledChange(it) }
                )
            }
        }
    }
}

@Composable
private fun SyncSettingsCard(
    autoSync: Boolean,
    onAutoSyncChange: (Boolean) -> Unit,
    conflictSafeMode: Boolean,
    onConflictSafeModeChange: (Boolean) -> Unit,
    backupBeforeSync: Boolean,
    onBackupBeforeSyncChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Sync & Backup",
                icon = Icons.Default.CloudSync,
                color = colors.secondary
            )

            SettingsSwitchItem(
                title = "Auto Sync",
                subtitle = "Periodic pull/push in background",
                icon = Icons.Default.Schedule,
                checked = autoSync,
                onCheckedChange = onAutoSyncChange
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSwitchItem(
                title = "Safe Conflict Strategy",
                subtitle = "Prefer no-overwrite during merge",
                icon = Icons.Default.Shield,
                checked = conflictSafeMode,
                onCheckedChange = onConflictSafeModeChange
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSwitchItem(
                title = "Backup Before Sync",
                subtitle = "Create snapshot before pull/rebase",
                icon = Icons.Default.Backup,
                checked = backupBeforeSync,
                onCheckedChange = onBackupBeforeSyncChange
            )
        }
    }
}

@Composable
private fun DeveloperOptionsCard(
    verboseLogging: Boolean,
    onVerboseLoggingChange: (Boolean) -> Unit,
    onTestFilePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Developer Options",
                icon = Icons.Default.Build,
                color = colors.primary
            )

            SettingsSwitchItem(
                title = "Verbose Logging",
                subtitle = "Show detailed Git operations in terminal",
                icon = Icons.Default.Terminal,
                checked = verboseLogging,
                onCheckedChange = onVerboseLoggingChange
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableItem(
                title = "Test File Picker",
                subtitle = "Open file picker and show selected path",
                icon = Icons.Default.FolderOpen,
                onClick = onTestFilePicker
            )
        }
    }
}

@Composable
private fun AboutCard(
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    val packageInfo = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }

    val versionName = packageInfo?.versionName ?: "Unknown"
    val versionCode = packageInfo?.let {
        PackageInfoCompat.getLongVersionCode(it).toString()
    } ?: "Unknown"

    ElevatedCard(
        modifier = modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "About",
                icon = Icons.Outlined.Info,
                color = colors.tertiary
            )

            SettingsInfoItem(
                title = "Version",
                value = versionName
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsInfoItem(
                title = "Build",
                value = versionCode
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsLinkItem(
                title = "Source Code",
                subtitle = "JamGmilk/FuwaGit",
                icon = Icons.Default.Code,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/JamGmilk/FuwaGit".toUri())
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsLinkItem(
                title = "Report Issue",
                subtitle = "Submit bug reports or feature requests",
                icon = Icons.Default.BugReport,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/JamGmilk/FuwaGit/issues/new".toUri())
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun GlobalConfigCard(
    userName: String,
    userEmail: String,
    defaultBranch: String,
    applyResult: ApplyConfigResult?,
    onUserConfigSave: (String, String) -> Unit,
    onDefaultBranchSave: (String) -> Unit,
    onReload: suspend () -> Unit,
    onApplyToAllRepos: (String, String, Boolean) -> Unit,
    onClearApplyResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var userConfigExpanded by rememberSaveable { mutableStateOf(false) }
    var branchConfigExpanded by rememberSaveable { mutableStateOf(false) }

    var userConfigKey by rememberSaveable { mutableStateOf(0) }
    var branchConfigKey by rememberSaveable { mutableStateOf(0) }

    var localUserName by remember(userConfigKey) { mutableStateOf(userName) }
    var localUserEmail by remember(userConfigKey) { mutableStateOf(userEmail) }
    var localDefaultBranch by remember(branchConfigKey) { mutableStateOf(defaultBranch) }

    LaunchedEffect(userConfigExpanded) {
        if (userConfigExpanded) {
            onReload()
        } else {
            userConfigKey++
        }
    }

    LaunchedEffect(userConfigExpanded, userName, userEmail) {
        if (userConfigExpanded) {
            localUserName = userName
            localUserEmail = userEmail
        }
    }

    LaunchedEffect(branchConfigExpanded) {
        if (branchConfigExpanded) {
            onReload()
        } else {
            branchConfigKey++
        }
    }

    LaunchedEffect(branchConfigExpanded, defaultBranch) {
        if (branchConfigExpanded) {
            localDefaultBranch = defaultBranch
        }
    }

    ElevatedCard(
        modifier = modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Configuration",
                icon = Icons.Default.Code,
                color = colors.secondary
            )

            ExpandableSettingsItem(
                title = "User & Email",
                subtitle = "Set global author information for commits",
                icon = Icons.Default.CreditCard,
                expanded = userConfigExpanded,
                onExpandedChange = { userConfigExpanded = it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Set the default author information. This saves to the global Git config (~/.gitconfig).\nWhen cloning or initializing a repo, you can choose to copy this to the repository's local config.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = localUserName,
                        onValueChange = { localUserName = it },
                        label = { Text("user.name") },
                        placeholder = { Text("Your Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = localUserEmail,
                        onValueChange = { localUserEmail = it },
                        label = { Text("user.email") },
                        placeholder = { Text("your.email@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { userConfigExpanded = false }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    onUserConfigSave(localUserName, localUserEmail)
                                }.invokeOnCompletion {
                                    userConfigExpanded = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }

            // 鏄剧ず搴旂敤閰嶇疆缁撴灉瀵硅瘽锟?
            if (applyResult != null) {
                ApplyConfigResultDialog(
                    result = applyResult,
                    onDismiss = { onClearApplyResult() }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            ExpandableSettingsItem(
                title = "Default Branch",
                subtitle = "init.defaultBranch = ${defaultBranch.ifBlank { "main" }}",
                icon = Icons.Default.AccountTree,
                expanded = branchConfigExpanded,
                onExpandedChange = { branchConfigExpanded = it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Set the default branch name for new repositories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = localDefaultBranch,
                        onValueChange = { localDefaultBranch = it },
                        label = { Text("init.defaultbranch") },
                        placeholder = { Text("main") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { branchConfigExpanded = false }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    onDefaultBranchSave(localDefaultBranch.ifBlank { "main" })
                                }.invokeOnCompletion {
                                    branchConfigExpanded = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    color: Color
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 搴旂敤鍒版墍鏈変粨搴撳璇濇
 */
@Composable
private fun ApplyToAllReposDialog(
    name: String,
    email: String,
    onApply: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var applyToGlobal by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.secondary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Apply to All Repositories",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "This will apply the following settings to all saved repositories:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "user.name: $name",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "user.email: $email",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Also apply to Global Config",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "This will update ~/.gitconfig",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(
                        checked = applyToGlobal,
                        onCheckedChange = { applyToGlobal = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(applyToGlobal) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * 搴旂敤閰嶇疆缁撴灉瀵硅瘽锟?
 */
@Composable
private fun ApplyConfigResultDialog(
    result: ApplyConfigResult,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        (if (result.allSuccess) colors.primary else colors.tertiary).copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (result.allSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (result.allSuccess) colors.primary else colors.tertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = if (result.allSuccess) "Success" else "Completed with Issues",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${result.successCount}/${result.totalCount} repositories updated successfully",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (result.failures.isNotEmpty()) {
                    Text(
                        text = "Failed repositories:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        result.failures.forEach { (path, error) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$path: $error",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("OK")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (checked) colors.primary.copy(alpha = 0.12f)
                    else colors.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.primary,
                checkedTrackColor = colors.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun ExpandableSettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (expanded) {
                        onExpandedChange(false)
                    } else {
                        onExpandedChange(true)
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (expanded) colors.primary.copy(alpha = 0.12f)
                        else colors.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (expanded) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (expanded) colors.primary else colors.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (expanded) colors.primary.copy(alpha = 0.7f) else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (expanded) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        rotationZ = if (expanded) 90f else 0f
                    }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = colors.onSurface
        )
    }
}

@Composable
private fun SettingsLinkItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AppearanceSettingsCard(
    darkMode: String,
    onDarkModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var showDarkModeMenu by remember { mutableStateOf(false) }

    val darkModeLabel = when (darkMode) {
        "always_on" -> "Always On"
        "always_off" -> "Always Off"
        else -> "Follow System"
    }

    ElevatedCard(
        modifier = modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Appearance",
                icon = Icons.Default.DarkMode,
                color = colors.primary
            )

            Box {
                SettingsClickableItem(
                    title = "Dark Mode",
                    subtitle = darkModeLabel,
                    icon = Icons.Default.DarkMode,
                    onClick = { showDarkModeMenu = true }
                )

                DropdownMenu(
                    expanded = showDarkModeMenu,
                    onDismissRequest = { showDarkModeMenu = false }
                ) {
                    val options = listOf(
                        "system" to "Follow System",
                        "always_on" to "Always On",
                        "always_off" to "Always Off"
                    )

                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = darkMode == value,
                                        onClick = {
                                            onDarkModeChange(value)
                                            showDarkModeMenu = false
                                        }
                                    )
                                    Text(text = label)
                                }
                            },
                            onClick = {
                                onDarkModeChange(value)
                                showDarkModeMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
