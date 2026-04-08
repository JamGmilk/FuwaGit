package jamgmilk.fuwagit.ui.screen.credentials

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var selectedUuid by remember { mutableStateOf<String?>(null) }

    val hasHttps = httpsCredentials.isNotEmpty()
    val hasSsh = sshKeys.isNotEmpty()

    LaunchedEffect(hasHttps, hasSsh) {
        if (!hasHttps && hasSsh) selectedTab = 1
        else if (hasHttps) selectedTab = 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab 切换区域
                if (hasHttps && hasSsh) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CredentialTabChip(
                            label = "HTTPS",
                            icon = Icons.Default.Link,
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                selectedUuid = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CredentialTabChip(
                            label = "SSH",
                            icon = Icons.Default.Key,
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                selectedUuid = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 核心列表逻辑
                val colors = MaterialTheme.colorScheme
                val currentItems = if (selectedTab == 0) httpsCredentials else sshKeys
                val currentType = if (selectedTab == 0) CredentialType.HTTPS else CredentialType.SSH

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.small) // 关键修复：外层统一裁切圆角
                        .border(1.dp, colors.outlineVariant, AppShapes.small)
                        .background(colors.surfaceContainerLow)
                ) {
                    if (currentItems.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No credentials found", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    } else {
                        currentItems.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = colors.outlineVariant.copy(alpha = 0.5f)
                                )
                            }

                            val accentColor = if (currentType == CredentialType.HTTPS) colors.primary else colors.tertiary

                            when (item) {
                                is HttpsCredential -> {
                                    CredentialSelectItem(
                                        title = item.host,
                                        subtitle = item.username,
                                        tag = null,
                                        icon = Icons.Default.Link,
                                        selected = item.uuid == selectedUuid,
                                        accentColor = accentColor,
                                        onClick = { selectedUuid = item.uuid }
                                    )
                                }
                                is SshKey -> {
                                    CredentialSelectItem(
                                        title = item.name,
                                        subtitle = "${item.fingerprint.take(24)}...",
                                        tag = item.type,
                                        icon = Icons.Default.Key,
                                        selected = item.uuid == selectedUuid,
                                        accentColor = accentColor,
                                        onClick = { selectedUuid = item.uuid }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedUuid?.let {
                        onSelect(it, if (selectedTab == 0) CredentialType.HTTPS else CredentialType.SSH)
                    }
                },
                enabled = selectedUuid != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accentColor.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accentColor else colors.outline.copy(alpha = 0.3f)
        )
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
private fun CredentialSelectItem(
    title: String,
    subtitle: String,
    tag: String?,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) accentColor.copy(alpha = 0.08f) else Color.Transparent)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                if (tag != null) {
                    Box(modifier = Modifier.background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(tag, style = MaterialTheme.typography.labelSmall, color = accentColor, fontSize = 10.sp)
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (selected) {
            Icon(Icons.Default.Check, null, tint = accentColor, modifier = Modifier.size(20.dp))
        }
    }
}