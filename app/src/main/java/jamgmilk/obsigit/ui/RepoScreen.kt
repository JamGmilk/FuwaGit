package jamgmilk.obsigit.ui

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jamgmilk.obsigit.ui.theme.ObsiGitTheme
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    val context = LocalContext.current
    val folders by viewModel.repoItems.collectAsState()
    val selectedTarget by viewModel.targetPath.collectAsState()

    var showCloneDialog by remember { mutableStateOf(value = false) }
    var expandedFab by remember { mutableStateOf(value = false) }
    var selectedItemForSheet by remember { mutableStateOf<RepoFolderItem?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data?.data?.let { uri ->
            viewModel.addGrantedTreeUri(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPersistedUris(context)
        viewModel.refreshRepoItems(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            RepoSpeedDial(
                expanded = expandedFab,
                onExpandedChange = { expandedFab = it },
                onCloneRemote = { showCloneDialog = true },
            ) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                }
                folderPicker.launch(intent)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Repo",
                style = MaterialTheme.typography.titleLarge,
                color = colors.primary
            )

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
                elevation = CardDefaults.elevatedCardElevation(0.dp)
            ) {
                if (folders.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = colors.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No folders loaded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = "Use the + button to add a repository.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Tap to select, long press for options.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(folders, key = { it.path }) { item ->
                                RepoItemRow(
                                    item = item,
                                    isSelectedTarget = (item.localPath != null) && (item.localPath == selectedTarget),
                                    onSetTarget = {
                                        item.localPath?.let { path ->
                                            viewModel.setTargetPath(context, path)
                                            viewModel.switchPage(AppPage.GitTerminal)
                                        }
                                    },
                                    onLongClick = {
                                        selectedItemForSheet = item
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCloneDialog) {
        AlertDialog(
            onDismissRequest = { showCloneDialog = false },
            title = { Text(text = "Clone Remote Repo") },
            text = { Text(text = "Cloning functionality is not yet implemented.") },
            confirmButton = {
                TextButton(onClick = { showCloneDialog = false }) {
                    Text(text = "OK")
                }
            }
        )
    }

    if (selectedItemForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedItemForSheet = null },
            sheetState = sheetState,
            containerColor = uiColors.cardContainer,
            contentColor = colors.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = selectedItemForSheet?.name ?: "Repository",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(text = "Remove from list") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = colors.error) },
                    modifier = Modifier.combinedClickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            selectedItemForSheet?.let { viewModel.removeRepo(context, it) }
                            selectedItemForSheet = null
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text(text = "Remote config") },
                    leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.combinedClickable {
                        // TODO
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedItemForSheet = null }
                    }
                )
                ListItem(
                    headlineContent = { Text(text = "More info") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.combinedClickable {
                        // TODO
                        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedItemForSheet = null }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepoItemRow(
    item: RepoFolderItem,
    isSelectedTarget: Boolean,
    onSetTarget: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    val canSetTarget = item.localPath != null
    val containerColor = if (isSelectedTarget) colors.primaryContainer else uiColors.cardContainer
    
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val lastModifiedText = if (item.lastModified > 0) {
        dateFormat.format(Date(item.lastModified))
    } else {
        "Unknown"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = { if (canSetTarget) onSetTarget() },
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
            }

            Text(
                text = item.path,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (item.isGitRepo) "Git repository: yes" else "Git repository: no",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isGitRepo) colors.tertiary else colors.onSurfaceVariant
                )
                Text(
                    text = "Modified: $lastModifiedText",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun RepoSpeedDial(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCloneRemote: () -> Unit,
    onAddLocal: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, label = "fab_rotation")

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                SpeedDialAction(
                    icon = Icons.Default.CloudDownload,
                    label = "Clone Remote Repo",
                    onClick = {
                        onCloneRemote()
                        onExpandedChange(false)
                    }
                )
                SpeedDialAction(
                    icon = Icons.Default.Add,
                    label = "Add Local Repo",
                    onClick = {
                        onAddLocal()
                        onExpandedChange(false)
                    }
                )
            }
        }
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = colors.primary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Expand",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
fun SpeedDialAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(end = 4.dp)
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
            elevation = CardDefaults.elevatedCardElevation(2.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp),
            containerColor = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun RepoScreenPreview() {
    ObsiGitTheme {
        RepoScreen(viewModel = AppViewModel())
    }
}
