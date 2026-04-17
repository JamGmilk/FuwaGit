package jamgmilk.fuwagit.ui.screen.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.ui.components.SubSettingsTemplate
import jamgmilk.fuwagit.ui.screen.credentials.CredentialSelectDialog
import jamgmilk.fuwagit.ui.screen.credentials.CredentialType

@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sshKeys: List<SshKey> = emptyList(),
    onTestSshConnection: (host: String, sshKeyUuid: String) -> Unit,
    sshTestResult: SshTestResult = SshTestResult.Idle
) {
    val context = LocalContext.current

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    SubSettingsTemplate(
        title = stringResource(R.string.screen_permissions),
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SystemPermissionsCard(
                onRequestAllFilesAccess = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = "package:${context.packageName}".toUri()
                        })
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            )

            SshTestCard(
                sshKeys = sshKeys,
                onTestSshConnection = onTestSshConnection,
                sshTestResult = sshTestResult
            )
        }
    }
}

/**
 * Represents the result of an SSH connection test.
 */
sealed class SshTestResult {
    data object Idle : SshTestResult()
    data object Testing : SshTestResult()
    data class Success(val message: String) : SshTestResult()
    data class Failure(val message: String) : SshTestResult()
}

@Composable
private fun SystemPermissionsCard(
    onRequestAllFilesAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isInspectionMode = LocalInspectionMode.current

    fun checkAllFilesStatus(): PermissionStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) PermissionStatus.Granted else PermissionStatus.Denied
        } else {
            val writeGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (writeGranted) PermissionStatus.Granted else PermissionStatus.Denied
        }
    }

    var allFilesStatus by remember {
        mutableStateOf(
            if (isInspectionMode) PermissionStatus.Unknown else checkAllFilesStatus()
        )
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_RESUME) {
                    allFilesStatus = checkAllFilesStatus()
                }
            }
        })
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.permissions_system_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionItem(
                    icon = Icons.Default.Storage,
                    title = stringResource(R.string.permissions_all_files_access),
                    description = stringResource(R.string.permissions_all_files_access_desc),
                    status = allFilesStatus,
                    actionLabel = stringResource(R.string.permissions_grant),
                    onAction = onRequestAllFilesAccess,
                    accentColor = colors.primary
                )
            }
        }
    }
}

/**
 * SSH Test Card - allows users to test SSH key connectivity.
 */
@Composable
private fun SshTestCard(
    sshKeys: List<SshKey>,
    onTestSshConnection: (host: String, sshKeyUuid: String) -> Unit,
    sshTestResult: SshTestResult,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var host by remember { mutableStateOf("git@github.com") }
    var selectedKeyUuid by remember { mutableStateOf<String?>(null) }
    var selectedKeyName by remember { mutableStateOf<String?>(null) }
    var showKeySelector by remember { mutableStateOf(false) }

    var lastTestedHost by remember { mutableStateOf<String?>(null) }
    var lastTestedKeyUuid by remember { mutableStateOf<String?>(null) }
    var hostError by remember { mutableStateOf<String?>(null) }

    val displayResult = remember(host, selectedKeyUuid, sshTestResult, lastTestedHost, lastTestedKeyUuid) {
        derivedStateOf {
            when (sshTestResult) {
                is SshTestResult.Idle -> SshTestResult.Idle
                is SshTestResult.Testing -> sshTestResult
                is SshTestResult.Success -> {
                    if (host == lastTestedHost && selectedKeyUuid == lastTestedKeyUuid) {
                        sshTestResult
                    } else {
                        SshTestResult.Idle
                    }
                }
                is SshTestResult.Failure -> {
                    if (host == lastTestedHost && selectedKeyUuid == lastTestedKeyUuid) {
                        sshTestResult
                    } else {
                        SshTestResult.Idle
                    }
                }
            }
        }
    }.value

    LaunchedEffect(sshTestResult) {
        if (sshTestResult is SshTestResult.Success || sshTestResult is SshTestResult.Failure) {
            lastTestedHost = host
            lastTestedKeyUuid = selectedKeyUuid
        }
    }

    LaunchedEffect(host) {
        val trimmedHost = host.trim()
        hostError = when {
            trimmedHost.isBlank() -> null
            !trimmedHost.contains("@") -> "Host must be in user@hostname format (e.g., git@github.com)"
            trimmedHost.split("@").size != 2 || trimmedHost.split("@")[0].isBlank() || trimmedHost.split("@")[1].isBlank() ->
                "Host must be in user@hostname format (e.g., git@github.com)"
            else -> null
        }
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.primaryContainer.copy(alpha = 0.15f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.ssh_test_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Host input
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.ssh_test_host_label)) },
                    placeholder = { Text(stringResource(R.string.ssh_test_host_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    isError = hostError != null,
                    supportingText = hostError?.let { { Text(it) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary,
                        cursorColor = colors.primary,
                        errorBorderColor = colors.error,
                        errorLabelColor = colors.error
                    )
                )

                // SSH Key selector trigger
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showKeySelector = true },
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = if (selectedKeyUuid != null) colors.primary else colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ssh_test_select_key),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                text = selectedKeyName ?: stringResource(R.string.ssh_test_key_not_selected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedKeyUuid != null) colors.onSurface else colors.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Test button
                val isTesting = displayResult is SshTestResult.Testing
                val hasResult = displayResult is SshTestResult.Success || displayResult is SshTestResult.Failure

                Button(
                    onClick = {
                        if (selectedKeyUuid != null && host.isNotBlank() && hostError == null) {
                            onTestSshConnection(host.trim(), selectedKeyUuid!!)
                        }
                    },
                    enabled = selectedKeyUuid != null && host.isNotBlank() && hostError == null && !isTesting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        disabledContainerColor = colors.primary.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ssh_test_testing))
                    } else {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ssh_test_button))
                    }
                }

                // Result display
                if (hasResult) {
                    val isSuccess = displayResult is SshTestResult.Success
                    val message = when (displayResult) {
                        is SshTestResult.Success -> displayResult.message
                        is SshTestResult.Failure -> displayResult.message
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSuccess) colors.primary.copy(alpha = 0.1f) else colors.error.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isSuccess) colors.primary else colors.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isSuccess) stringResource(R.string.ssh_test_result_success) else stringResource(R.string.ssh_test_result_failure),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSuccess) colors.primary else colors.error
                                )
                            }
                            
                            if (message.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.surface.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.onSurface,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // SSH Key Selection Dialog
    if (showKeySelector) {
        CredentialSelectDialog(
            title = stringResource(R.string.ssh_test_select_key),
            onDismiss = { showKeySelector = false },
            onSelect = { uuid, type ->
                if (type == CredentialType.SSH || type == CredentialType.BOTH) {
                    val key = sshKeys.find { it.uuid == uuid }
                    if (key != null) {
                        selectedKeyUuid = key.uuid
                        selectedKeyName = key.name
                    }
                }
                showKeySelector = false
            },
            sshKeys = sshKeys,
            httpsCredentials = emptyList(),
            initialType = CredentialType.SSH
        )
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    status: PermissionStatus,
    actionLabel: String,
    onAction: () -> Unit,
    accentColor: Color
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = accentColor.copy(alpha = 0.15f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            StatusBadge(status = status)
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onAction,
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = accentColor.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: PermissionStatus) {
    val colors = MaterialTheme.colorScheme
    val (color, icon, text) = when (status) {
        PermissionStatus.Granted -> Triple(colors.primary, Icons.Default.CheckCircle, stringResource(R.string.permissions_status_granted))
        PermissionStatus.Denied -> Triple(colors.error, Icons.Default.Error, stringResource(R.string.permissions_status_denied))
        PermissionStatus.Unknown -> Triple(colors.tertiary, Icons.Default.Info, stringResource(R.string.permissions_status_unknown))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

private enum class PermissionStatus {
    Granted, Denied, Unknown
}
