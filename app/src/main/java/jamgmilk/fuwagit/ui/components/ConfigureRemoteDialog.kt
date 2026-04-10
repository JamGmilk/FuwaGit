package jamgmilk.fuwagit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.ui.screen.credentials.CredentialSelectDialog
import jamgmilk.fuwagit.ui.screen.credentials.CredentialType
import jamgmilk.fuwagit.ui.theme.AppShapes

@Composable
fun ConfigureRemoteDialog(
    repoName: String,
    currentUrl: String,
    selectedCredentialUuid: String? = null,
    selectedCredentialType: CredentialType? = null,
    httpsCredentials: List<HttpsCredential> = emptyList(),
    sshKeys: List<SshKey> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (url: String, httpsCredentialUuid: String?, sshKeyUuid: String?) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    var showCredentialDialog by remember { mutableStateOf(false) }
    var selectedHttpsUuid by remember { mutableStateOf<String?>(selectedCredentialUuid.takeIf { selectedCredentialType == CredentialType.HTTPS }) }
    var selectedSshUuid by remember { mutableStateOf<String?>(selectedCredentialUuid.takeIf { selectedCredentialType == CredentialType.SSH }) }
    var credentialError by remember { mutableStateOf<String?>(null) }

    val isUrlHttps = url.trim().lowercase().startsWith("https://")
    val isUrlSsh = url.trim().lowercase().startsWith("ssh://") ||
                   url.trim().lowercase().startsWith("git@") ||
                   url.trim().lowercase().contains("@") && url.trim().contains(":")
    val hasCredentialSelection = selectedHttpsUuid != null || selectedSshUuid != null
    val isCredentialMatch = when {
        !hasCredentialSelection -> true
        isUrlHttps && selectedHttpsUuid != null -> true
        isUrlSsh && selectedSshUuid != null -> true
        else -> false
    }
    val saveEnabled = url.isNotBlank() && isCredentialMatch
    val mismatchErrorText = stringResource(R.string.remote_credential_url_mismatch)

    if (showCredentialDialog) {
        CredentialSelectDialog(
            title = stringResource(R.string.remote_configure_select_credential),
            httpsCredentials = httpsCredentials,
            sshKeys = sshKeys,
            onDismiss = { showCredentialDialog = false },
            onSelect = { uuid, type ->
                when (type) {
                    CredentialType.HTTPS -> {
                        selectedHttpsUuid = uuid
                        selectedSshUuid = null
                        credentialError = if (isUrlSsh) mismatchErrorText else null
                    }
                    CredentialType.SSH -> {
                        selectedSshUuid = uuid
                        selectedHttpsUuid = null
                        credentialError = if (isUrlHttps) mismatchErrorText else null
                    }
                    else -> {}
                }
                showCredentialDialog = false
            }
        )
    }

    DialogWithIcon(
        onDismiss = onDismiss,
        icon = Icons.Filled.Link,
        title = stringResource(R.string.myrepos_configure_remote),
        subtitle = repoName,
        content = {
//            TipInDialog(
//                icon = Icons.Default.Info,
//                text = "Set the remote 'origin' URL for pushing and pulling code.")

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.remote_url_label)) },
                singleLine = true,
                shape = AppShapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            )

            if (credentialError != null) {
                Text(
                    text = credentialError!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            CredentialSelectionButton(
                selectedHttpsUuid = selectedHttpsUuid,
                selectedSshUuid = selectedSshUuid,
                httpsCredentials = httpsCredentials,
                sshKeys = sshKeys,
                onClick = { showCredentialDialog = true }
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(url, selectedHttpsUuid, selectedSshUuid) },
                enabled = saveEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = AppShapes.extraSmall
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun CredentialSelectionButton(
    selectedHttpsUuid: String?,
    selectedSshUuid: String?,
    httpsCredentials: List<HttpsCredential>,
    sshKeys: List<SshKey>,
    onClick: () -> Unit
) {
    val hasSelection = selectedHttpsUuid != null || selectedSshUuid != null

    val httpsLabel = stringResource(R.string.credentials_host_label)
    val sshLabel = stringResource(R.string.remote_ssh_key_label)
    val credentialLabel = stringResource(R.string.remote_credential_label)
    
    val label = when {
        selectedHttpsUuid != null -> {
            val cred = httpsCredentials.find { it.uuid == selectedHttpsUuid }
            "${cred?.username ?: httpsLabel}@${cred?.host ?: ""}"
        }
        selectedSshUuid != null -> {
            val key = sshKeys.find { it.uuid == selectedSshUuid }
            key?.name ?: sshLabel
        }
        else -> credentialLabel
    }

    val icon = when {
        selectedHttpsUuid != null -> Icons.Default.Link
        selectedSshUuid != null -> Icons.Default.Key
        else -> Icons.Default.Key
    }

    Surface(
        shape = AppShapes.extraSmall,
        color = if (hasSelection) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (hasSelection) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.extraSmall)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (hasSelection) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        shape = AppShapes.extraSmall
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (hasSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (hasSelection) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (hasSelection) FontWeight.SemiBold else FontWeight.Normal,
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}
