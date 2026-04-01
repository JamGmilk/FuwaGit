package jamgmilk.fuwagit.ui.screen.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.net.toUri
import jamgmilk.fuwagit.ui.biometric.BiometricActivity
import jamgmilk.fuwagit.ui.components.FilePickerDialog
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.screen.credentials.CredentialsStoreViewModel
import jamgmilk.fuwagit.ui.screen.credentials.UnlockDialog
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import androidx.compose.material3.Text
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import jamgmilk.fuwagit.ui.screen.credentials.ChangeMasterPasswordDialog
import jamgmilk.fuwagit.ui.screen.credentials.SetupMasterPasswordDialog

private const val TAG = "SettingsScreen"
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToCredentials: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    credentialsViewModel: CredentialsStoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val credentialsUiState by credentialsViewModel.uiState.collectAsState()

    var showFilePicker by rememberSaveable { mutableStateOf(false) }

    var autoSync by rememberSaveable { mutableStateOf(false) }
    var conflictSafeMode by rememberSaveable { mutableStateOf(true) }
    var backupBeforeSync by rememberSaveable { mutableStateOf(true) }
    var showHiddenFiles by rememberSaveable { mutableStateOf(false) }
    var verboseLogging by rememberSaveable { mutableStateOf(false) }
    var pendingBiometricEnable by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        credentialsViewModel.initialize()
    }

    LaunchedEffect(credentialsUiState.isDecryptionUnlocked, pendingBiometricEnable) {
        Log.d(TAG, "LaunchedEffect: isDecryptionUnlocked=${credentialsUiState.isDecryptionUnlocked}, pendingBiometricEnable=$pendingBiometricEnable")
        if (credentialsUiState.isDecryptionUnlocked && pendingBiometricEnable) {
            Log.d(TAG, "LaunchedEffect: calling enableBiometric")
            delay(100)
            pendingBiometricEnable = false
            activity?.let { credentialsViewModel.enableBiometric(it) }
        }
    }

    ScreenTemplate(
        title = "Settings",
        modifier = modifier
    ) {
        StorageSettingsCard(
            onPermissionsClick = onNavigateToPermissions,
            showHiddenFiles = showHiddenFiles,
            onShowHiddenFilesChange = { showHiddenFiles = it },
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
                Log.d(TAG, "Switch toggled: enabled=$enabled, isDecryptionUnlocked=${credentialsUiState.isDecryptionUnlocked}")
                if (!credentialsUiState.isDecryptionUnlocked) {
                    pendingBiometricEnable = true
                    credentialsViewModel.showUnlockDialog()
                } else if (enabled) {
                    Log.d(TAG, "Calling enableBiometric directly")
                    activity?.let { credentialsViewModel.enableBiometric(it) }
                } else {
                    credentialsViewModel.disableBiometric()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        SyncSettingsCard(
            autoSync = autoSync,
            onAutoSyncChange = { autoSync = it },
            conflictSafeMode = conflictSafeMode,
            onConflictSafeModeChange = { conflictSafeMode = it },
            backupBeforeSync = backupBeforeSync,
            onBackupBeforeSyncChange = { backupBeforeSync = it },
            modifier = Modifier.fillMaxWidth()
        )

        DeveloperOptionsCard(
            verboseLogging = verboseLogging,
            onVerboseLoggingChange = { verboseLogging = it },
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
private fun StorageSettingsCard(
    onPermissionsClick: () -> Unit,
    showHiddenFiles: Boolean,
    onShowHiddenFilesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Storage",
                icon = Icons.Default.Storage,
                color = Color(0xFF4CAF50)
            )

            SettingsNavigationItem(
                title = "Permissions",
                subtitle = "Manage storage access permissions",
                icon = Icons.Default.Security,
                onClick = onPermissionsClick
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSwitchItem(
                title = "Show Hidden Files",
                subtitle = "Display files starting with .",
                icon = Icons.Default.Folder,
                checked = showHiddenFiles,
                onCheckedChange = onShowHiddenFilesChange
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
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Security",
                icon = Icons.Default.Shield,
                color = Color(0xFFE91E63)
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
                    checked = biometricEnabled && isDecryptionUnlocked,
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
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Sync & Backup",
                icon = Icons.Default.CloudSync,
                color = Color(0xFF2196F3)
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
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "Developer Options",
                icon = Icons.Default.Build,
                color = Color(0xFFFF9800)
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
    val uiColors = FuwaGitThemeExtras.colors
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
        modifier = modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = "About",
                icon = Icons.Outlined.Info,
                color = Color(0xFF9C27B0)
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

@Composable
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
