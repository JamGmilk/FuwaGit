package jamgmilk.fuwagit.ui.screen.credentials

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.ui.theme.AppShapes

enum class CredentialType {
    HTTPS, SSH, BOTH
}

@Composable
fun CredentialSelectDialog(
    title: String = "Select Credential",
    httpsCredentials: List<HttpsCredential> = emptyList(),
    sshKeys: List<SshKey> = emptyList(),
    onDismiss: () -> Unit,
    onSelect: (uuid: String, type: CredentialType) -> Unit
) {

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedHttpsUuid by remember { mutableStateOf<String?>(null) }
    var selectedSshUuid by remember { mutableStateOf<String?>(null) }

    val showBoth = httpsCredentials.isNotEmpty() && sshKeys.isNotEmpty()
    val showHttpsOnly = httpsCredentials.isNotEmpty() && sshKeys.isEmpty()
    val showSshOnly = sshKeys.isNotEmpty() && httpsCredentials.isEmpty()

    if (showBoth) {
        selectedTab = 0
    } else if (showHttpsOnly) {
        selectedTab = 1
    } else if (showSshOnly) {
        selectedTab = 2
    }

    val effectiveTabCount = when {
        showBoth -> 2
        showHttpsOnly || showSshOnly -> 1
        else -> 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (effectiveTabCount == 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CredentialTabChip(
                            label = "HTTPS",
                            icon = Icons.Default.Link,
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        CredentialTabChip(
                            label = "SSH",
                            icon = Icons.Default.Key,
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                when (selectedTab) {
                    0, 1 -> {
                        if (showBoth || showHttpsOnly) {
                            HttpsCredentialList(
                                credentials = httpsCredentials,
                                selectedUuid = selectedHttpsUuid,
                                onSelect = { selectedHttpsUuid = it }
                            )
                        }
                    }
                    else -> {
                        if (showBoth || showSshOnly) {
                            SshKeyList(
                                keys = sshKeys,
                                selectedUuid = selectedSshUuid,
                                onSelect = { selectedSshUuid = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (selectedTab) {
                        0, 1 -> selectedHttpsUuid?.let { onSelect(it, CredentialType.HTTPS) }
                        else -> selectedSshUuid?.let { onSelect(it, CredentialType.SSH) }
                    }
                },
                enabled = (selectedTab != 2 && selectedHttpsUuid != null) ||
                        (selectedTab == 2 && selectedSshUuid != null),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Select")
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
private fun CredentialTabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val accentColor = if (label == "HTTPS") colors.primary else colors.tertiary

    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accentColor.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, accentColor)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentColor else colors.onSurface
            )
        }
    }
}

@Composable
private fun HttpsCredentialList(
    credentials: List<HttpsCredential>,
    selectedUuid: String?,
    onSelect: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accentColor = colors.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, AppShapes.small)
            .background(colors.surfaceContainerLow, AppShapes.small),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (credentials.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No HTTPS Credentials",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        } else {
            credentials.forEachIndexed { index, cred ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = colors.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                HttpsCredentialSelectItem(
                    credential = cred,
                    selected = cred.uuid == selectedUuid,
                    onClick = { onSelect(cred.uuid) }
                )
            }
        }
    }
}

@Composable
private fun HttpsCredentialSelectItem(
    credential: HttpsCredential,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accentColor = colors.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) accentColor.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = credential.host,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = credential.username,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SshKeyList(
    keys: List<SshKey>,
    selectedUuid: String?,
    onSelect: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, AppShapes.small)
            .background(colors.surfaceContainerLow, AppShapes.small),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (keys.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No SSH Keys",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        } else {
            keys.forEachIndexed { index, key ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = colors.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                SshKeySelectItem(
                    key = key,
                    selected = key.uuid == selectedUuid,
                    onClick = { onSelect(key.uuid) }
                )
            }
        }
    }
}

@Composable
private fun SshKeySelectItem(
    key: SshKey,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accentColor = colors.tertiary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) accentColor.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Key,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = key.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = key.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = key.fingerprint.take(24) + "...",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
