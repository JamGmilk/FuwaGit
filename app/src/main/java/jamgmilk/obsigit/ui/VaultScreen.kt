package jamgmilk.obsigit.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun VaultScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val folders by viewModel.vaultItems.collectAsState()
    val grantedUris by viewModel.grantedTreeUris.collectAsState()
    val selectedTarget by viewModel.targetPath.collectAsState()

    val requestReadPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri -> if (uri != null) viewModel.addGrantedTreeUri(context, uri) }
    )

    LaunchedEffect(Unit) {
        viewModel.refreshVaultItems(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Vault",
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
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Permissions and Folder Grants", fontWeight = FontWeight.Bold, color = colors.primary)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                            } else {
                                requestReadPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("Grant Storage")
                    }
                    Button(
                        onClick = { folderPicker.launch(AppVaultOps.defaultDocumentsTreeUri()) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.secondary)
                    ) {
                        Text("Pick Folder")
                    }
                }

                val allFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    true
                }

                Text(
                    text = "All-files access: ${if (allFilesAccess) "Granted" else "Not granted"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = "Granted folder trees: ${grantedUris.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = "Includes Obsidian sandbox and app sandbox auto-detection",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        HorizontalDivider(color = colors.primary.copy(alpha = 0.25f))

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            if (folders.isEmpty()) {
                Text(
                    text = "No folders loaded yet. Grant permission and pick a vault folder.",
                    modifier = Modifier.padding(16.dp),
                    color = colors.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(folders, key = { it.path }) { item ->
                        VaultItemRow(
                            item = item,
                            isSelectedTarget = item.localPath != null && item.localPath == selectedTarget,
                            onSetTarget = {
                                item.localPath?.let { path ->
                                    viewModel.setTargetPath(path)
                                    viewModel.switchPage(AppPage.GitTerminal)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultItemRow(
    item: VaultFolderItem,
    isSelectedTarget: Boolean,
    onSetTarget: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val canSetTarget = item.localPath != null
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(enabled = canSetTarget, onClick = onSetTarget),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelectedTarget) colors.primaryContainer else Color.White.copy(alpha = 0.86f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(item.name, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (item.isGitRepo) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF6C00), modifier = Modifier.size(18.dp))
                }
            }

            Text(
                text = item.path,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Owner: ${item.owner}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = if (item.isGitRepo) "Git repository: yes" else "Git repository: no",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isGitRepo) Color(0xFF2E7D32) else colors.onSurfaceVariant
            )

            Text(
                text = if (canSetTarget) {
                    if (item.isGitRepo) "Use as Git target" else "Use as Git target (git init available)"
                } else {
                    "Not selectable as Git target"
                },
                color = if (canSetTarget) colors.primary else colors.outline,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable(enabled = canSetTarget, onClick = onSetTarget)
            )
        }
    }
}
