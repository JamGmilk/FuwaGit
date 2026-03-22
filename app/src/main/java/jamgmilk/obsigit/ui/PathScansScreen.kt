package jamgmilk.obsigit.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import jamgmilk.obsigit.ui.theme.ObsiGitTheme

@Composable
fun PathScansScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = MaterialTheme.colorScheme
    val rootStatus by viewModel.rootStatus.collectAsState()
    val pathScanItems by viewModel.pathScanItems.collectAsState()
    val grantedFolders by viewModel.grantedTreeUris.collectAsState()

    val requestReadPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.addGrantedTreeUri(context, uri)
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.refreshVaultItems(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshVaultItems(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { folderPicker.launch(AppVaultOps.defaultDocumentsTreeUri()) },
                icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                text = { Text("Add Folder") },
                containerColor = colors.primary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
                }
                Text(
                    text = "Path Scans",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primary
                )
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
                            onClick = { viewModel.checkRoot() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.secondary)
                        ) {
                            Text("Check Root")
                        }
                    }

                    val allFilesAccess = if (LocalInspectionMode.current) {
                        false
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
                        text = "Root: ${
                            when (rootStatus) {
                                RootStatus.Idle -> "Idle"
                                RootStatus.Checking -> "Checking"
                                RootStatus.Granted -> "Granted"
                                RootStatus.Denied -> "Denied"
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "Granted folder trees: ${grantedFolders.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
                elevation = CardDefaults.elevatedCardElevation(0.dp)
            ) {
                if (pathScanItems.isEmpty()) {
                    Text(
                        text = "No granted folders yet. Tap Add Folder to pick a repo root.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(pathScanItems, key = { it.id }) { item ->
                            PathScanRow(
                                item = item,
                                onRemove = { viewModel.removePathScan(context, item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PathScanRow(item: PathScanItem, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surface.copy(alpha = 0.88f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(item.name, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (item.isGitRepo) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(18.dp))
                }
                if (item.isRemovable) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove scan",
                            tint = colors.error
                        )
                    }
                }
            }

            Text(
                text = item.path,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (item.isGitRepo) "Git repository: yes" else "Git repository: no",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isGitRepo) colors.tertiary else colors.onSurfaceVariant
            )
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun PathScansScreenPreview() {
    ObsiGitTheme {
        PathScansScreen(
            viewModel = AppViewModel(),
            onBack = { }
        )
    }
}
