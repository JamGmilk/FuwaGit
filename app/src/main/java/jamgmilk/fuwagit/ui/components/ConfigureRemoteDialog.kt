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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.ui.screen.credentials.CredentialSelectDialog
import jamgmilk.fuwagit.ui.screen.credentials.CredentialType
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras

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
    val colors = MaterialTheme.colorScheme

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

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF2196F3).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Link,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Configure Remote",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = repoName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Set the remote 'origin' URL for pushing and pulling code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        focusedLabelColor = Color(0xFF2196F3),
                        cursorColor = Color(0xFF2196F3)
                    )
                )

                CredentialSelectionButton(
                    selectedHttpsUuid = selectedHttpsUuid,
                    selectedSshUuid = selectedSshUuid,
                    httpsCredentials = httpsCredentials,
                    sshKeys = sshKeys,
                    onClick = { showCredentialDialog = true }
                )

                if (currentUrl.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Remote is configured",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(url, selectedHttpsUuid, selectedSshUuid) },
                enabled = url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp)
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
        },
        shape = RoundedCornerShape(24.dp)
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
    val colors = MaterialTheme.colorScheme
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

    val iconColor = when {
        selectedHttpsUuid != null -> FuwaGitThemeExtras.colors.mizuiroAccent
        selectedSshUuid != null -> FuwaGitThemeExtras.colors.mizuiroAccentDark
        else -> colors.onSurfaceVariant
    }

    val icon = when {
        selectedHttpsUuid != null -> Icons.Default.Link
        selectedSshUuid != null -> Icons.Default.Key
        else -> Icons.Default.Key
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (hasSelection) iconColor.copy(alpha = 0.1f) else colors.surfaceVariant.copy(alpha = 0.5f),
        border = if (hasSelection) {
            androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.4f))
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (hasSelection) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = if (hasSelection) iconColor else colors.onSurfaceVariant
                )
                if (!hasSelection) {
                    Text(
                        text = "Use this credential for push/pull operations",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
