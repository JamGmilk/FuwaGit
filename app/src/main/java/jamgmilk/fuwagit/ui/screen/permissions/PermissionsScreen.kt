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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import jamgmilk.fuwagit.ui.AppViewModel
import jamgmilk.fuwagit.ui.RootStatus
import jamgmilk.fuwagit.ui.components.SubSettingsTemplate
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import jamgmilk.fuwagit.ui.theme.Sakura50
import jamgmilk.fuwagit.ui.theme.Sakura80
import jamgmilk.fuwagit.ui.theme.Sakura90

@Composable
fun PermissionsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors
    val rootStatus by viewModel.rootStatus.collectAsState()
    val grantedFolders by viewModel.grantedTreeUris.collectAsState()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    SubSettingsTemplate(
        title = "Permissions",
        onBack = onBack,
        modifier = modifier
    ) {
        SystemPermissionsCard(
            rootStatus = rootStatus,
            onRequestAllFilesAccess = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = "package:${context.packageName}".toUri()
                    })
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            },
            onCheckRoot = { viewModel.checkRoot() }
        )

        ScopedStorageCard(
            grantedFoldersCount = grantedFolders.size
        )
    }
}

@Composable
private fun SystemPermissionsCard(
    rootStatus: RootStatus,
    onRequestAllFilesAccess: () -> Unit,
    onCheckRoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    val allFilesStatus = if (LocalInspectionMode.current) {
        PermissionStatus.Unknown
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (Environment.isExternalStorageManager()) PermissionStatus.Granted else PermissionStatus.Denied
    } else {
        val writeGranted = ContextCompat.checkSelfPermission(
            androidx.compose.ui.platform.LocalContext.current,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (writeGranted) PermissionStatus.Granted else PermissionStatus.Denied
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Sakura80.copy(alpha = 0.1f),
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
                        tint = Sakura80,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "System Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Sakura80
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionItem(
                    icon = Icons.Default.Storage,
                    title = "All Files Access",
                    description = "Required for Git operations on all directories",
                    status = allFilesStatus,
                    actionLabel = if (allFilesStatus == PermissionStatus.Granted) "Granted" else "Grant",
                    onAction = onRequestAllFilesAccess,
                    actionEnabled = allFilesStatus != PermissionStatus.Granted,
                    accentColor = Sakura80
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(uiColors.cardBorder)
                )

                PermissionItem(
                    icon = Icons.Default.Security,
                    title = "Root Access",
                    description = "Optional: Required for system-level Git operations",
                    status = when (rootStatus) {
                        RootStatus.Idle -> PermissionStatus.Unknown
                        RootStatus.Checking -> PermissionStatus.Unknown
                        RootStatus.Granted -> PermissionStatus.Granted
                        RootStatus.Denied -> PermissionStatus.Denied
                    },
                    actionLabel = when (rootStatus) {
                        RootStatus.Checking -> "Checking..."
                        else -> "Check"
                    },
                    onAction = onCheckRoot,
                    actionEnabled = rootStatus != RootStatus.Checking,
                    accentColor = Sakura90
                )
            }
        }
    }
}

@Composable
private fun ScopedStorageCard(
    grantedFoldersCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Sakura50.copy(alpha = 0.15f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Sakura80,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Scoped Storage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Sakura80
                        )
                        Text(
                            text = "SAF folder access",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Sakura80.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Sakura80,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Folder Grants",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$grantedFoldersCount folders with SAF access",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (grantedFoldersCount > 0) Color(0xFF4CAF50).copy(alpha = 0.15f) else colors.surfaceVariant
                    ) {
                        Text(
                            text = grantedFoldersCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (grantedFoldersCount > 0) Color(0xFF4CAF50) else colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                            text = "Folder grants are managed automatically when you add local repositories. Remove them by long-pressing an item in the Repo list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
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
    actionEnabled: Boolean,
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
                enabled = actionEnabled,
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
    val (color, icon, text) = when (status) {
        PermissionStatus.Granted -> Triple(Color(0xFF4CAF50), Icons.Default.CheckCircle, "Granted")
        PermissionStatus.Denied -> Triple(Color(0xFFE53935), Icons.Default.Error, "Denied")
        PermissionStatus.Unknown -> Triple(Color(0xFFFFA000), Icons.Default.Info, "Unknown")
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
