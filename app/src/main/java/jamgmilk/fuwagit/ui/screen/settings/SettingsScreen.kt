package jamgmilk.fuwagit.ui.screen.settings

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import jamgmilk.fuwagit.BuildConfig
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
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
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.ui.components.FilePickerDialog
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.screen.credentials.CredentialStoreViewModel
import jamgmilk.fuwagit.ui.screen.credentials.UnlockDialog
import jamgmilk.fuwagit.util.CrashLogManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow

private const val TAG = "SettingsScreen"
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToCredentials: () -> Unit = {},
    onNavigateToMasterPassword: () -> Unit = {},
    onMasterPasswordSuccess: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    credentialsViewModel: CredentialStoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = context as? FragmentActivity
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val credentialsUiState by credentialsViewModel.uiState.collectAsStateWithLifecycle()
    val applyResult = settingsUiState.applyResult

    var showFilePicker by rememberSaveable { mutableStateOf(false) }
    var pendingBiometricEnable by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Re-initialize credentials state when screen comes into focus
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                credentialsViewModel.initialize()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(applyResult) {
        applyResult?.let {
        }
    }

    LaunchedEffect(credentialsUiState.error) {
        credentialsUiState.error?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage, duration = SnackbarDuration.Long)
            credentialsViewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.LanguageChanged -> {
                    val localeList = when (event.language) {
                        "zh" -> LocaleListCompat.forLanguageTags("zh")
                        "en" -> LocaleListCompat.forLanguageTags("en")
                        else -> LocaleListCompat.getEmptyLocaleList()
                    }
                    AppCompatDelegate.setApplicationLocales(localeList)
                }
            }
        }
    }

    // Re-initialize credentials state when returning to settings
    LaunchedEffect(Unit) {
        credentialsViewModel.initialize()
    }

    var previousMasterPasswordSet by remember { mutableStateOf(credentialsUiState.isMasterPasswordSet) }

    LaunchedEffect(credentialsUiState.isMasterPasswordSet) {
        if (!previousMasterPasswordSet && credentialsUiState.isMasterPasswordSet) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.credentials_master_password_set_successfully)
                )
            }
        }
        previousMasterPasswordSet = credentialsUiState.isMasterPasswordSet
    }

    LaunchedEffect(Unit) {
        snapshotFlow { 
            Pair(credentialsUiState.isDecryptionUnlocked, pendingBiometricEnable) 
        }.collectLatest { (isUnlocked, pendingEnable) ->
            if (BuildConfig.DEBUG) Log.d(TAG, "snapshotFlow: isDecryptionUnlocked=$isUnlocked, pendingBiometricEnable=$pendingEnable")
            if (isUnlocked && pendingEnable) {
                if (BuildConfig.DEBUG) Log.d(TAG, "snapshotFlow: calling enableBiometric, activity=$activity")
                if (activity == null) {
                    Log.e(TAG, "snapshotFlow: activity is NULL, cannot enable biometric")
                }
                delay(100)
                pendingBiometricEnable = false
                activity?.let { credentialsViewModel.enableBiometric(it) }
            }
        }
    }

    ScreenTemplate(
        title = stringResource(R.string.screen_settings),
        modifier = modifier,
        snackbarHostState = snackbarHostState
    ) {
        BetaWarningCard(
            darkMode = settingsUiState.darkMode,
            modifier = Modifier.fillMaxWidth()
        )

        StorageSettingsCard(
            onPermissionsClick = onNavigateToPermissions,
            modifier = Modifier.fillMaxWidth()
        )

        GlobalConfigCard(
            userName = settingsUiState.userName,
            userEmail = settingsUiState.userEmail,
            defaultBranch = settingsUiState.defaultBranch,
            setUpstreamOnPush = settingsUiState.setUpstreamOnPush,
            applyResult = applyResult,
            onUserConfigSave = { name, email -> settingsViewModel.saveUserConfig(name, email) },
            onDefaultBranchSave = { settingsViewModel.saveDefaultBranch(it) },
            onSetUpstreamOnPushChange = { settingsViewModel.saveSetUpstreamOnPush(it) },
            onReload = { settingsViewModel.reloadUserConfig() },
            onApplyToAllRepos = { name, email, alsoToGlobal ->
                settingsViewModel.applyConfigToAllRepos(name, email, alsoToGlobal)
            },
            onClearApplyResult = { settingsViewModel.clearApplyResult() },
            modifier = Modifier.fillMaxWidth()
        )

        SecuritySettingsCard(
            onCredentialsClick = {
                if (!credentialsUiState.isMasterPasswordSet) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.settings_please_set_master_password_first)
                        )
                    }
                } else {
                    onNavigateToCredentials()
                }
            },
            onMasterPasswordClick = {
                onNavigateToMasterPassword()
            },
            biometricEnabled = credentialsUiState.isBiometricEnabled,
            isDecryptionUnlocked = credentialsUiState.isDecryptionUnlocked,
            isMasterPasswordSet = credentialsUiState.isMasterPasswordSet,
            onBiometricEnabledChange = { enabled ->
                if (BuildConfig.DEBUG) Log.d(TAG, "Switch toggled: enabled=$enabled, isDecryptionUnlocked=${credentialsUiState.isDecryptionUnlocked}, activity=$activity")
                if (enabled) {
                    if (!credentialsUiState.isDecryptionUnlocked) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Enabling biometric but locked, showing unlock dialog")
                        pendingBiometricEnable = true
                        credentialsViewModel.showUnlockDialog()
                    } else {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Calling enableBiometric directly")
                        activity?.let { credentialsViewModel.enableBiometric(it) }
                    }
                } else {
                    if (!credentialsUiState.isDecryptionUnlocked && credentialsUiState.isBiometricEnabled) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Already enabled but locked, showing unlock dialog to unlock vault")
                        credentialsViewModel.showUnlockDialog()
                    } else {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Disabling biometric")
                        credentialsViewModel.disableBiometric()
                    }
                }
            },
            autoLockTimeout = settingsUiState.autoLockTimeout,
            onAutoLockTimeoutChange = { timeout -> settingsViewModel.saveAutoLockTimeout(timeout) },
            modifier = Modifier.fillMaxWidth()
        )

        AppearanceSettingsCard(
            darkMode = settingsUiState.darkMode,
            language = settingsUiState.language,
            dynamicColor = settingsUiState.dynamicColor,
            onDarkModeChange = { mode -> settingsViewModel.saveDarkMode(mode) },
            onLanguageChange = { lang -> settingsViewModel.saveLanguage(lang) },
            onDynamicColorChange = { settingsViewModel.saveDynamicColor(it) },
            modifier = Modifier.fillMaxWidth()
        )

        // SyncSettingsCard(
        //     autoSync = settingsUiState.autoSync,
        //     onAutoSyncChange = { settingsViewModel.saveAutoSync(it) },
        //     conflictSafeMode = settingsUiState.conflictSafeMode,
        //     onConflictSafeModeChange = { settingsViewModel.saveConflictSafeMode(it) },
        //     backupBeforeSync = settingsUiState.backupBeforeSync,
        //     onBackupBeforeSyncChange = { settingsViewModel.saveBackupBeforeSync(it) },
        //     modifier = Modifier.fillMaxWidth()
        // )

        DeveloperOptionsCard(
            verboseLogging = settingsUiState.verboseLogging,
            onVerboseLoggingChange = { settingsViewModel.saveVerboseLogging(it) },
            onTestFilePicker = { showFilePicker = true },
            onResetOnboarding = { settingsViewModel.resetOnboarding() },
            onClearKnownHostsComplete = {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.settings_clear_known_hosts_deleted))
                }
            },
            onClearCommitEditMsgComplete = {
                scope.launch {
                    val repoPath = settingsViewModel.getCurrentRepoPath()
                    if (repoPath != null) {
                        val commitEditMsg = java.io.File(repoPath, ".git/COMMIT_EDITMSG")
                        if (commitEditMsg.exists()) {
                            commitEditMsg.delete()
                            snackbarHostState.showSnackbar(context.getString(R.string.settings_commit_editmsg_deleted))
                        } else {
                            snackbarHostState.showSnackbar(context.getString(R.string.settings_no_commit_editmsg))
                        }
                    } else {
                        snackbarHostState.showSnackbar(context.getString(R.string.settings_no_repository_selected))
                    }
                }
            },
            onExportLogsComplete = { hasLogs, message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        AboutCard(
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showFilePicker) {
        FilePickerDialog(
            title = stringResource(R.string.test_file_picker_title),
            onDismiss = { showFilePicker = false },
            onSelect = { path ->
                showFilePicker = false
                scope.launch {
                    snackbarHostState.showSnackbar(path)
                }
            }
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
}

@Composable
private fun BetaWarningCard(
    modifier: Modifier = Modifier,
    darkMode: String = "system"
) {
    val colors = MaterialTheme.colorScheme
    val isDark = when (darkMode) {
        "always_on" -> true
        "always_off" -> false
        else -> isSystemInDarkTheme()
    }

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
                title = stringResource(R.string.settings_beta_version_notice),
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
                        text = stringResource(R.string.settings_beta_features_unstable),
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
                        text = stringResource(R.string.settings_beta_backup_warning),
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
                title = stringResource(R.string.settings_storage),
                icon = Icons.Default.Storage,
                color = colors.primary
            )

            SettingsNavigationItem(
                title = stringResource(R.string.settings_permissions),
                subtitle = stringResource(R.string.settings_permissions_subtitle),
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
    onBiometricEnabledChange: ((Boolean) -> Unit)? = null,
    autoLockTimeout: String = "300",
    onAutoLockTimeoutChange: (String) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var autoLockExpanded by remember { mutableStateOf(false) }
    var localTimeout by remember(autoLockTimeout) { mutableStateOf(autoLockTimeout) }

    ElevatedCard(
        modifier = modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_security),
                icon = Icons.Default.Shield,
                color = colors.tertiary
            )

            SettingsNavigationItem(
                title = stringResource(R.string.settings_credentials_title),
                subtitle = stringResource(R.string.settings_credentials_subtitle),
                icon = Icons.Default.Key,
                onClick = onCredentialsClick
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableItem(
                title = if (isMasterPasswordSet) stringResource(R.string.settings_change_master_password) else stringResource(R.string.settings_set_master_password),
                subtitle = if (isMasterPasswordSet) stringResource(R.string.settings_change_master_password_subtitle) else stringResource(R.string.settings_set_master_password_subtitle),
                icon = Icons.Default.Lock,
                onClick = onMasterPasswordClick
            )

            if (isMasterPasswordSet && onBiometricEnabledChange != null) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    title = stringResource(R.string.settings_biometric_unlock),
                    subtitle = when {
                        !isDecryptionUnlocked -> stringResource(R.string.settings_biometric_tap_to_unlock)
                        biometricEnabled -> stringResource(R.string.settings_biometric_enabled)
                        else -> stringResource(R.string.settings_biometric_use_fingerprint)
                    },
                    icon = Icons.Default.Fingerprint,
                    checked = biometricEnabled,
                    onCheckedChange = { onBiometricEnabledChange(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                val displayTimeout = when (autoLockTimeout.toLongOrNull()) {
                    0L -> stringResource(R.string.settings_auto_lock_never)
                    60L -> stringResource(R.string.settings_auto_lock_1_minute)
                    300L -> stringResource(R.string.settings_auto_lock_5_minutes)
                    600L -> stringResource(R.string.settings_auto_lock_10_minutes)
                    1800L -> stringResource(R.string.settings_auto_lock_30_minutes)
                    3600L -> stringResource(R.string.settings_auto_lock_1_hour)
                    else -> stringResource(R.string.settings_auto_lock_seconds_format, (autoLockTimeout.toLongOrNull() ?: 0L).toInt())
                }

                ExpandableSettingsItem(
                    title = stringResource(R.string.settings_auto_lock_timeout),
                    subtitle = stringResource(R.string.settings_auto_lock_subtitle_format, displayTimeout),
                    icon = Icons.Default.Schedule,
                    expanded = autoLockExpanded,
                    onExpandedChange = { autoLockExpanded = it }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_auto_lock_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = localTimeout,
                            onValueChange = { localTimeout = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.settings_timeout_label)) },
                            placeholder = { Text(stringResource(R.string.settings_timeout_placeholder)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { autoLockExpanded = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val timeout = localTimeout.toLongOrNull() ?: 300
                                        onAutoLockTimeoutChange(timeout.toString())
                                    }.invokeOnCompletion {
                                        autoLockExpanded = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.action_save))
                            }
                        }
                    }
                }
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
                title = stringResource(R.string.settings_sync_backup),
                icon = Icons.Default.CloudSync,
                color = colors.secondary
            )

            SettingsSwitchItem(
                title = stringResource(R.string.settings_auto_sync),
                subtitle = stringResource(R.string.settings_auto_sync_subtitle),
                icon = Icons.Default.Schedule,
                checked = autoSync,
                onCheckedChange = onAutoSyncChange
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSwitchItem(
                title = stringResource(R.string.settings_conflict_safe_mode),
                subtitle = stringResource(R.string.settings_conflict_safe_mode_subtitle),
                icon = Icons.Default.Shield,
                checked = conflictSafeMode,
                onCheckedChange = onConflictSafeModeChange
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSwitchItem(
                title = stringResource(R.string.settings_backup_before_sync),
                subtitle = stringResource(R.string.settings_backup_before_sync_subtitle),
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
    onResetOnboarding: () -> Unit,
    onClearKnownHostsComplete: () -> Unit,
    onClearCommitEditMsgComplete: () -> Unit,
    onExportLogsComplete: (Boolean, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var showResetConfirm by remember { mutableStateOf(false) }
    var showKnownHostsDialog by remember { mutableStateOf(false) }
    var showCommitEditMsgDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_developer_options),
                icon = Icons.Default.Build,
                color = colors.primary
            )

            SettingsSwitchItem(
                title = stringResource(R.string.settings_verbose_logging),
                subtitle = stringResource(R.string.settings_verbose_logging_subtitle),
                icon = Icons.Default.Terminal,
                checked = verboseLogging,
                onCheckedChange = onVerboseLoggingChange
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableItem(
                title = stringResource(R.string.settings_test_file_picker),
                subtitle = stringResource(R.string.settings_test_file_picker_subtitle),
                icon = Icons.Default.FolderOpen,
                onClick = onTestFilePicker
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableItem(
                title = stringResource(R.string.settings_reset_onboarding),
                subtitle = stringResource(R.string.settings_reset_onboarding_subtitle),
                icon = Icons.Default.AccountTree,
                onClick = { showResetConfirm = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableItem(
                title = stringResource(R.string.settings_clear_known_hosts),
                subtitle = stringResource(R.string.settings_clear_known_hosts_subtitle),
                icon = Icons.Default.Key,
                onClick = { showKnownHostsDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsClickableItem(
                title = stringResource(R.string.settings_clear_commit_editmsg),
                subtitle = stringResource(R.string.settings_clear_commit_editmsg_subtitle),
                icon = Icons.Default.Build,
                onClick = { showCommitEditMsgDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            val emptyLogsText = stringResource(R.string.settings_export_logs_empty)
            val shareTitleText = stringResource(R.string.settings_export_logs_share_title)
            SettingsClickableItem(
                title = stringResource(R.string.settings_export_logs),
                subtitle = stringResource(R.string.settings_export_logs_subtitle),
                icon = Icons.Default.BugReport,
                onClick = {
                    val logFiles = CrashLogManager.getLogFiles()
                    if (logFiles.isEmpty()) {
                        onExportLogsComplete(false, emptyLogsText)
                    } else {
                        val shareIntent = CrashLogManager.createShareIntent(context)
                        context.startActivity(Intent.createChooser(shareIntent, shareTitleText))
                        onExportLogsComplete(true, "Logs exported")
                    }
                }
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(colors.tertiary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = colors.tertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_reset_onboarding),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_reset_onboarding_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetOnboarding()
                        showResetConfirm = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_done))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showKnownHostsDialog) {
        ClearKnownHostsDialog(
            onDismiss = { showKnownHostsDialog = false },
            onDeleted = {
                onClearKnownHostsComplete()
                showKnownHostsDialog = false
            }
        )
    }

    if (showCommitEditMsgDialog) {
        ClearCommitEditMsgDialog(
            onDismiss = { showCommitEditMsgDialog = false },
            onDeleted = {
                onClearCommitEditMsgComplete()
                showCommitEditMsgDialog = false
            }
        )
    }
}

@Composable
private fun ClearKnownHostsDialog(
    onDismiss: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val knownHostsFile = java.io.File(context.filesDir, "ssh_known_hosts")
    val fileContent = remember { if (knownHostsFile.exists()) knownHostsFile.readText() else "" }
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.error.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.settings_clear_known_hosts_confirm_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                if (fileContent.isBlank()) {
                    Text(
                        text = stringResource(R.string.settings_clear_known_hosts_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = fileContent,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (fileContent.isNotBlank()) {
                Button(
                    onClick = {
                        knownHostsFile.delete()
                        onDeleted()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ClearCommitEditMsgDialog(
    onDismiss: () -> Unit,
    onDeleted: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.error.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.settings_clear_commit_editmsg_confirm_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.settings_clear_commit_editmsg_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onDeleted,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.error)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
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
                title = stringResource(R.string.settings_about),
                icon = Icons.Outlined.Info,
                color = colors.tertiary
            )

            SettingsInfoItem(
                title = stringResource(R.string.settings_version),
                value = versionName
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsInfoItem(
                title = stringResource(R.string.settings_build),
                value = versionCode
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsLinkItem(
                title = stringResource(R.string.settings_source_code),
                subtitle = stringResource(R.string.settings_source_code_subtitle),
                icon = Icons.Default.Code,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/JamGmilk/FuwaGit".toUri())
                    context.startActivity(intent)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsLinkItem(
                title = stringResource(R.string.settings_report_issue),
                subtitle = stringResource(R.string.settings_report_issue_subtitle),
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
    setUpstreamOnPush: Boolean,
    applyResult: ApplyConfigResult?,
    onUserConfigSave: (String, String) -> Unit,
    onDefaultBranchSave: (String) -> Unit,
    onSetUpstreamOnPushChange: (Boolean) -> Unit,
    onReload: suspend () -> Unit,
    onApplyToAllRepos: (String, String, Boolean) -> Unit,
    onClearApplyResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var userConfigExpanded by rememberSaveable { mutableStateOf(false) }
    var branchConfigExpanded by rememberSaveable { mutableStateOf(false) }

    var userConfigKey by rememberSaveable { mutableIntStateOf(0) }
    var branchConfigKey by rememberSaveable { mutableIntStateOf(0) }

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
                title = stringResource(R.string.settings_configuration),
                icon = Icons.Default.Code,
                color = colors.secondary
            )

            ExpandableSettingsItem(
                title = stringResource(R.string.settings_user_email),
                subtitle = stringResource(R.string.settings_user_email_subtitle),
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
                        text = stringResource(R.string.settings_user_email_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = localUserName,
                        onValueChange = { localUserName = it },
                        label = { Text(stringResource(R.string.settings_user_name_label)) },
                        placeholder = { Text(stringResource(R.string.settings_user_name_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = localUserEmail,
                        onValueChange = { localUserEmail = it },
                        label = { Text(stringResource(R.string.settings_user_email_input_label)) },
                        placeholder = { Text(stringResource(R.string.settings_user_email_input_placeholder)) },
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
                            Text(stringResource(R.string.action_cancel))
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
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }

            // Show application configuration result dialog
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
                title = stringResource(R.string.settings_default_branch),
                subtitle = stringResource(R.string.settings_default_branch_subtitle, defaultBranch.ifBlank { "main" }),
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
                        text = stringResource(R.string.settings_default_branch_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = localDefaultBranch,
                        onValueChange = { localDefaultBranch = it },
                        label = { Text(stringResource(R.string.settings_default_branch_label)) },
                        placeholder = { Text(stringResource(R.string.settings_default_branch_placeholder)) },
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
                            Text(stringResource(R.string.action_cancel))
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
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            SettingsSwitchItem(
                title = stringResource(R.string.settings_set_upstream_on_push),
                subtitle = stringResource(R.string.settings_set_upstream_on_push_subtitle),
                icon = Icons.Default.CloudSync,
                checked = setUpstreamOnPush,
                onCheckedChange = onSetUpstreamOnPushChange
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

/**
 * 应用到所有仓库对话框
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
                text = stringResource(R.string.apply_to_all_repos_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.apply_to_all_repos_description),
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
                                text = stringResource(R.string.settings_config_name_format, name),
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
                                text = stringResource(R.string.settings_config_email_format, email),
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
                            text = stringResource(R.string.apply_to_all_repos_also_global),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.apply_to_all_repos_also_global_subtitle),
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
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * 应用配置结果对话框
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
                text = if (result.allSuccess) stringResource(R.string.apply_config_result_success) else stringResource(R.string.apply_config_result_issues),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.apply_config_result_success_format, result.successCount, result.totalCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (result.failures.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.apply_config_result_failed_repos),
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
                                    text = stringResource(R.string.settings_error_format, path, error),
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
                Text(stringResource(R.string.action_ok))
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
    language: String,
    dynamicColor: Boolean,
    onDarkModeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var darkModeExpanded by rememberSaveable { mutableStateOf(false) }
    var languageExpanded by rememberSaveable { mutableStateOf(false) }

    val darkModeLabel = when (darkMode) {
        "always_on" -> stringResource(R.string.settings_dark_mode_always_on)
        "always_off" -> stringResource(R.string.settings_dark_mode_always_off)
        else -> stringResource(R.string.settings_dark_mode_system)
    }

    val languageLabel = when (language) {
        "zh" -> stringResource(R.string.settings_language_zh_cn)
        "en" -> stringResource(R.string.settings_language_en)
        else -> stringResource(R.string.settings_language_system)
    }

    ElevatedCard(
        modifier = modifier.border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_appearance),
                icon = Icons.Default.Palette,
                color = colors.primary
            )

            ExpandableSettingsItem(
                title = stringResource(R.string.settings_dark_mode),
                subtitle = darkModeLabel,
                icon = Icons.Default.DarkMode,
                expanded = darkModeExpanded,
                onExpandedChange = { darkModeExpanded = it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val darkModeOptions = listOf(
                        "system" to stringResource(R.string.settings_dark_mode_system),
                        "always_on" to stringResource(R.string.settings_dark_mode_always_on),
                        "always_off" to stringResource(R.string.settings_dark_mode_always_off)
                    )

                    darkModeOptions.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDarkModeChange(value)
                                    darkModeExpanded = false
                                }
                                .padding(vertical = 2.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = darkMode == value,
                                onClick = {
                                    onDarkModeChange(value)
                                    darkModeExpanded = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_dynamic_color),
                    subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
                    icon = Icons.Default.AutoAwesome,
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColorChange
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            ExpandableSettingsItem(
                title = stringResource(R.string.settings_language),
                subtitle = languageLabel,
                icon = Icons.Default.Language,
                expanded = languageExpanded,
                onExpandedChange = { languageExpanded = it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val languageOptions = listOf(
                        "system" to stringResource(R.string.settings_language_system),
                        "zh" to stringResource(R.string.settings_language_zh_cn),
                        "en" to stringResource(R.string.settings_language_en)
                    )

                    languageOptions.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageChange(value)
                                    languageExpanded = false
                                }
                                .padding(vertical = 2.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language == value,
                                onClick = {
                                    onLanguageChange(value)
                                    languageExpanded = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
