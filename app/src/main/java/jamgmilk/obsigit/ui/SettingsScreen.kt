package jamgmilk.obsigit.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val rootStatus by viewModel.rootStatus.collectAsState()
    val selectedPath by viewModel.targetPath.collectAsState()
    var autoSync by rememberSaveable { mutableStateOf(false) }
    var credentialsStored by rememberSaveable { mutableStateOf(false) }
    var conflictSafeMode by rememberSaveable { mutableStateOf(true) }
    var backupBeforeSync by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = colors.primary
        )

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Execution", fontWeight = FontWeight.Bold)
                Text("Current Git target: ${selectedPath ?: "Not selected"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.checkRoot() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("Check Root")
                    }
                    Text(
                        text = when (rootStatus) {
                            RootStatus.Idle -> "Idle"
                            RootStatus.Checking -> "Checking"
                            RootStatus.Granted -> "Granted"
                            RootStatus.Denied -> "Denied"
                        },
                        color = when (rootStatus) {
                            RootStatus.Granted -> Color(0xFF2E7D32)
                            RootStatus.Denied -> Color(0xFFC62828)
                            else -> colors.onSurfaceVariant
                        }
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Storage", fontWeight = FontWeight.Bold)
                Text("Manage storage grants and defaults.")
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondary)
                ) {
                    Text("Open Storage Access Settings")
                }
            }
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    "Future Settings",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )

                ListItem(
                    headlineContent = { Text("Store Git credentials") },
                    supportingContent = { Text("Use secure token or keychain provider") },
                    leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = credentialsStored,
                            onCheckedChange = { credentialsStored = it }
                        )
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Auto sync schedule") },
                    supportingContent = { Text("Run periodic pull/push in background") },
                    leadingContent = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = autoSync,
                            onCheckedChange = { autoSync = it }
                        )
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Safe conflict strategy") },
                    supportingContent = { Text("Prefer no-overwrite during merge") },
                    leadingContent = { Icon(Icons.Default.Sync, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = conflictSafeMode,
                            onCheckedChange = { conflictSafeMode = it }
                        )
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Backup before sync") },
                    supportingContent = { Text("Create snapshot before pull/rebase") },
                    leadingContent = { Icon(Icons.Default.Backup, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = backupBeforeSync,
                            onCheckedChange = { backupBeforeSync = it }
                        )
                    }
                )
            }
        }
    }
}
