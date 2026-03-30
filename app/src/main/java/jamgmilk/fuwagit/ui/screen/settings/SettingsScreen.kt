package jamgmilk.fuwagit.ui.screen.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.net.toUri
import jamgmilk.fuwagit.ui.AppViewModel
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposViewModel
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.screen.credentials.CredentialsScreen
import jamgmilk.fuwagit.ui.screen.credentials.CredentialsStoreViewModel
import jamgmilk.fuwagit.ui.screen.permissions.PermissionsScreen


import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras

sealed class SettingsNavigationTarget {
    data object Main : SettingsNavigationTarget()
    data object Credentials : SettingsNavigationTarget()
    data object Permissions : SettingsNavigationTarget()
}

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    myReposViewModel: MyReposViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors
    val savedRepos by myReposViewModel.savedRepos.collectAsState()
    val savedReposCount = savedRepos.size

    var showPermissions by rememberSaveable { mutableStateOf(false) }
    var showCredentials by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        myReposViewModel.loadSavedRepos()
    }

    LaunchedEffect(showPermissions, showCredentials) {
        viewModel.swipeEnabled = !(showPermissions || showCredentials)
    }

    var autoSync by rememberSaveable { mutableStateOf(false) }
    var conflictSafeMode by rememberSaveable { mutableStateOf(true) }
    var backupBeforeSync by rememberSaveable { mutableStateOf(true) }
    var showHiddenFiles by rememberSaveable { mutableStateOf(false) }
    var verboseLogging by rememberSaveable { mutableStateOf(false) }
    var biometricEnabled by rememberSaveable { mutableStateOf(false) }

    val currentTarget = when {
        showCredentials -> SettingsNavigationTarget.Credentials
        showPermissions -> SettingsNavigationTarget.Permissions
        else -> SettingsNavigationTarget.Main
    }

    AnimatedContent(
        targetState = currentTarget,
        transitionSpec = {
            fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "settings_transition"
    ) { target ->
        when (target) {
            SettingsNavigationTarget.Credentials -> {
                BackHandler { showCredentials = false }
                CredentialsScreen(
                    viewModel = hiltViewModel(),
                    onBack = { showCredentials = false },
                    modifier = modifier
                )
            }
            SettingsNavigationTarget.Permissions -> {
                BackHandler { showPermissions = false }
                PermissionsScreen(
                    savedReposCount = savedReposCount,
                    onBack = { showPermissions = false },
                    modifier = modifier
                )
            }
            SettingsNavigationTarget.Main -> {
                ScreenTemplate(
                    title = "Settings",
                    modifier = modifier
                ) {
                    StorageSettingsCard(
                        onPermissionsClick = { showPermissions = true },
                        showHiddenFiles = showHiddenFiles,
                        onShowHiddenFilesChange = { showHiddenFiles = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    CredentialsSettingsCard(
                        onCredentialsClick = { showCredentials = true },
                        biometricEnabled = biometricEnabled,
                        onBiometricEnabledChange = { enabled ->
                            biometricEnabled = enabled
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
                        modifier = Modifier.fillMaxWidth()
                    )

                    AboutCard(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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
    val colors = MaterialTheme.colorScheme
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
private fun CredentialsSettingsCard(
    onCredentialsClick: () -> Unit,
    biometricEnabled: Boolean = false,
    onBiometricEnabledChange: ((Boolean) -> Unit)? = null,
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
                title = "Credentials",
                icon = Icons.Default.Key,
                color = Color(0xFFE91E63)
            )

            SettingsNavigationItem(
                title = "Git Credentials",
                subtitle = "HTTPS passwords & SSH keys",
                icon = Icons.Default.CreditCard,
                onClick = onCredentialsClick
            )

            if (onBiometricEnabledChange != null) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    title = "Biometric Unlock",
                    subtitle = if (biometricEnabled) "Enabled" else "Use fingerprint to unlock",
                    icon = Icons.Default.Lock,
                    checked = biometricEnabled,
                    onCheckedChange = onBiometricEnabledChange
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
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
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
        }
    }
}

@Composable
private fun AboutCard(
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
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
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
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

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.primary,
                checkedTrackColor = colors.primary.copy(alpha = 0.5f)
            )
        )
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
