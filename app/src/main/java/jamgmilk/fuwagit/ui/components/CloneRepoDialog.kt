package jamgmilk.fuwagit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.ui.screen.myrepos.HttpsCredentialItem
import jamgmilk.fuwagit.ui.screen.myrepos.SshKeyItem
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneRepoDialog(
    cloneUrl: String,
    onCloneUrlChange: (String) -> Unit,
    localPath: String,
    onPickFolder: () -> Unit,
    isDirectoryEmpty: Boolean,
    error: String?,
    httpsCredentials: List<HttpsCredentialItem>,
    sshKeys: List<SshKeyItem>,
    selectedHttpsUuid: String?,
    selectedSshUuid: String?,
    useCredential: Boolean,
    onHttpsSelected: (String?) -> Unit,
    onSshSelected: (String?) -> Unit,
    onUseCredentialChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onClone: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val isHttps = cloneUrl.startsWith("http://") || cloneUrl.startsWith("https://")
    val isSsh = cloneUrl.startsWith("git@") || cloneUrl.startsWith("ssh://")
    val showCredentials = useCredential && ((isHttps && httpsCredentials.isNotEmpty()) || (isSsh && sshKeys.isNotEmpty()))

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = FuwaGitThemeExtras.colors.mizuiroAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Clone Repository",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = cloneUrl,
                    onValueChange = onCloneUrlChange,
                    label = { Text("Repository URL") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (isHttps || isSsh) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isHttps) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFF2196F3).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isHttps) "HTTPS" else "SSH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isHttps) Color(0xFF4CAF50) else Color(0xFF2196F3),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FuwaGitThemeExtras.colors.mizuiroAccent,
                        focusedLabelColor = FuwaGitThemeExtras.colors.mizuiroAccent,
                        cursorColor = FuwaGitThemeExtras.colors.mizuiroAccent
                    )
                )

                if ((isHttps && httpsCredentials.isNotEmpty()) || (isSsh && sshKeys.isNotEmpty())) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useCredential,
                            onCheckedChange = onUseCredentialChange,
                            colors = CheckboxDefaults.colors(checkedColor = FuwaGitThemeExtras.colors.mizuiroAccent)
                        )
                        Text(
                            text = "Use saved credentials",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (showCredentials) {
                        if (isHttps && httpsCredentials.isNotEmpty()) {
                            CredentialDropdown(
                                label = "HTTPS Credential",
                                items = httpsCredentials.map { it.uuid to it.displayName },
                                selectedUuid = selectedHttpsUuid,
                                onSelected = onHttpsSelected,
                                accentColor = Color(0xFF4CAF50)
                            )
                        }

                        if (isSsh && sshKeys.isNotEmpty()) {
                            CredentialDropdown(
                                label = "SSH Key",
                                items = sshKeys.map { it.uuid to it.displayName },
                                selectedUuid = selectedSshUuid,
                                onSelected = onSshSelected,
                                accentColor = Color(0xFF2196F3)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (localPath.isBlank()) colors.surfaceVariant.copy(alpha = 0.5f)
                        else if (isDirectoryEmpty) Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else Color(0xFFE53935).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (localPath.isBlank()) Icons.Default.FolderOpen
                                else if (isDirectoryEmpty) Icons.Default.CheckCircle
                                else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (localPath.isBlank()) colors.onSurfaceVariant
                                else if (isDirectoryEmpty) Color(0xFF4CAF50)
                                else Color(0xFFE53935),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (localPath.isBlank()) "Select target folder"
                                       else if (isDirectoryEmpty) "Folder is empty"
                                       else "Folder is not empty",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (localPath.isBlank()) colors.onSurfaceVariant
                                else if (isDirectoryEmpty) Color(0xFF4CAF50)
                                else Color(0xFFE53935)
                            )
                        }
                    }

                    IconButton(
                        onClick = onPickFolder,
                        modifier = Modifier
                            .size(48.dp)
                            .background(FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "Pick folder",
                            tint = FuwaGitThemeExtras.colors.mizuiroAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (localPath.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = localPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (error != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE53935).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE53935)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
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
                            text = "Repository will be cloned to the selected folder with its name",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onClone,
                enabled = cloneUrl.isNotBlank() && localPath.isNotBlank() && isDirectoryEmpty,
                colors = ButtonDefaults.buttonColors(containerColor = FuwaGitThemeExtras.colors.mizuiroAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Clone")
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
