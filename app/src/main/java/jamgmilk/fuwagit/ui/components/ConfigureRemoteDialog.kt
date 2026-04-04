package jamgmilk.fuwagit.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.ui.screen.credentials.CredentialSelectDialog
import jamgmilk.fuwagit.ui.screen.credentials.CredentialType
import jamgmilk.fuwagit.ui.theme.AppShapes

@Composable
fun ConfigureRemoteDialog(
    repoName: String,
    currentUrl: String,
    httpsCredentials: List<HttpsCredential> = emptyList(),
    sshKeys: List<SshKey> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (url: String, httpsCredentialUuid: String?, sshKeyUuid: String?) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    var showCredentialDialog by remember { mutableStateOf(false) }
    var selectedHttpsUuid by remember { mutableStateOf<String?>(null) }
    var selectedSshUuid by remember { mutableStateOf<String?>(null) }

    if (showCredentialDialog) {
        CredentialSelectDialog(
            title = "Select Remote Credential",
            httpsCredentials = httpsCredentials,
            sshKeys = sshKeys,
            onDismiss = { showCredentialDialog = false },
            onSelect = { uuid, type ->
                when (type) {
                    CredentialType.HTTPS -> {
                        selectedHttpsUuid = uuid
                        selectedSshUuid = null
                    }
                    CredentialType.SSH -> {
                        selectedSshUuid = uuid
                        selectedHttpsUuid = null
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
        title = "Configure Remote",
        subtitle = repoName,
        content = {
            TipInDialog(
                icon = Icons.Default.Info,
                text = "Set the remote 'origin' URL for pushing and pulling code.")

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Remote URL") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Source,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = AppShapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            )

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
                enabled = url.isNotBlank(),
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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

    val label = when {
        selectedHttpsUuid != null -> {
            val cred = httpsCredentials.find { it.uuid == selectedHttpsUuid }
            "${cred?.username ?: "HTTPS"}@${cred?.host ?: ""}"
        }
        selectedSshUuid != null -> {
            val key = sshKeys.find { it.uuid == selectedSshUuid }
            key?.name ?: "SSH Key"
        }
        else -> "Bind Credential (optional)"
    }

//    val iconColor = when {
//        selectedHttpsUuid != null -> FuwaGitThemeExtras.colors.mizuiroAccent
//        selectedSshUuid != null -> FuwaGitThemeExtras.colors.mizuiroAccentDark
//        else -> colors.onSurfaceVariant
//    }

    val icon = when {
        selectedHttpsUuid != null -> Icons.Default.Link
        selectedSshUuid != null -> Icons.Default.Key
        else -> Icons.Default.Key
    }

    Surface(
        shape = AppShapes.extraSmall,
        color = if (hasSelection) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (hasSelection) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.extraSmall)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
                //modifier = Modifier.size(18.dp)
            )
        }
    }
}
