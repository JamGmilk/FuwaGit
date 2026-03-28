package jamgmilk.obsigit.ui.screen.repo

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.obsigit.ui.AppViewModel
import jamgmilk.obsigit.ui.RepoFolderItem
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras
import jamgmilk.obsigit.ui.theme.Sakura50
import jamgmilk.obsigit.ui.theme.Sakura80
import jamgmilk.obsigit.ui.theme.Sakura90
import jamgmilk.obsigit.ui.components.ScreenTemplate
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigateToStatus: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    val context = LocalContext.current
    val folders by viewModel.repoItems.collectAsState()
    val selectedTarget by viewModel.targetPath.collectAsState()

    var showCloneDialog by remember { mutableStateOf(false) }
    var cloneUrl by remember { mutableStateOf("") }
    var expandedFab by remember { mutableStateOf(false) }
    var selectedItemForSheet by remember { mutableStateOf<RepoFolderItem?>(null) }
    var showRemoteDialog by remember { mutableStateOf<RepoFolderItem?>(null) }
    var showInfoDialog by remember { mutableStateOf<RepoFolderItem?>(null) }
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

    Box(modifier = modifier.fillMaxSize()) {
        ScreenTemplate(
            title = "Repositories",
            modifier = Modifier.fillMaxSize()
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
                elevation = CardDefaults.elevatedCardElevation(0.dp)
            ) {
                if (folders.isEmpty()) {
                    EmptyReposState()
                } else {
                    RepoListContent(
                        folders = folders,
                        selectedTarget = selectedTarget,
                        onSetTarget = { path ->
                            viewModel.setTargetPath(context, path)
                            onNavigateToStatus()
                        },
                        onItemLongClick = { selectedItemForSheet = it }
                    )
                }
            }
        }

        RepoSpeedDial(
            expanded = expandedFab,
            onExpandedChange = { expandedFab = it },
            onCloneRemote = { showCloneDialog = true },
            onAddLocal = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                }
                folderPicker.launch(intent)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    if (showCloneDialog) {
        CloneRepoDialog(
            cloneUrl = cloneUrl,
            onCloneUrlChange = { cloneUrl = it },
            onDismiss = {
                showCloneDialog = false
                cloneUrl = ""
            },
            onClone = {
                showCloneDialog = false
                cloneUrl = ""
            }
        )
    }

    selectedItemForSheet?.let { item ->
        RepoOptionsSheet(
            item = item,
            sheetState = sheetState,
            onDismiss = { selectedItemForSheet = null },
            onRemove = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    viewModel.removeRepo(context, item)
                    selectedItemForSheet = null
                }
            },
            onConfigureRemote = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showRemoteDialog = item
                    selectedItemForSheet = null
                }
            },
            onShowInfo = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showInfoDialog = item
                    selectedItemForSheet = null
                }
            }
        )
    }

    showRemoteDialog?.let { item ->
        val currentRemoteUrl = remember(item) {
            item.localPath?.let { viewModel.getRemoteUrl(it) } ?: ""
        }
        var remoteUrl by remember { mutableStateOf(currentRemoteUrl) }

        AlertDialog(
            onDismissRequest = { showRemoteDialog = null },
            title = { Text("Configure Remote", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Set the remote 'origin' URL for this repository.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = { Text("Remote URL") },
                        placeholder = { Text("https://github.com/user/repo.git") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        item.localPath?.let { viewModel.configureRemote(it, "origin", remoteUrl) }
                        showRemoteDialog = null
                    },
                    enabled = remoteUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Sakura80)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    showInfoDialog?.let { item ->
        val info = remember(item) { 
            if (item.localPath != null) viewModel.getRepoInfo(item.localPath) else emptyMap()
        }
        
        AlertDialog(
            onDismissRequest = { showInfoDialog = null },
            title = { Text("Repository Info", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(info.toList()) { (key, value) ->
                        Column {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.labelMedium,
                                color = Sakura80,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Sakura80)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RepoListContent(
    folders: List<RepoFolderItem>,
    selectedTarget: String?,
    onSetTarget: (String) -> Unit,
    onItemLongClick: (RepoFolderItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier.padding(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Tap to select, long press for options",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(folders, key = { it.path }) { item ->
                RepoItemCard(
                    item = item,
                    isSelected = item.localPath == selectedTarget,
                    onClick = { item.localPath?.let { onSetTarget(it) } },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RepoItemCard(
    item: RepoFolderItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val lastModifiedText = if (item.lastModified > 0) {
        dateFormat.format(Date(item.lastModified))
    } else {
        "Unknown"
    }

    val accentColor = if (item.isGitRepo) Color(0xFF4CAF50) else Color(0xFFFF9800)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Sakura80 else uiColors.cardBorder,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) Sakura80.copy(alpha = 0.08f) else colors.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Sakura80 else accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.isGitRepo) Icons.Default.Folder else Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Sakura80
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = item.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isGitRepo) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (item.isGitRepo) "Git repository" else "Not a git repo",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                    }

                    Text(
                        text = lastModifiedText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyReposState(
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Sakura80.copy(alpha = 0.1f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = Sakura80.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = "No repositories yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant
            )

            Text(
                text = "Use the + button to add a repository",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepoOptionsSheet(
    item: RepoFolderItem,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onConfigureRemote: () -> Unit,
    onShowInfo: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = uiColors.cardContainer,
        contentColor = colors.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = Sakura80.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Sakura80,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.path,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SheetOptionItem(
                icon = Icons.Default.Delete,
                title = "Remove from list",
                subtitle = "Remove this repository from the list",
                tint = colors.error,
                onClick = onRemove
            )

            SheetOptionItem(
                icon = Icons.Default.Settings,
                title = "Configure Remote",
                subtitle = "Set up remote origin URL",
                onClick = onConfigureRemote
            )

            SheetOptionItem(
                icon = Icons.Default.Info,
                title = "Repository Info",
                subtitle = "View detailed information",
                onClick = onShowInfo
            )
        }
    }
}

@Composable
private fun SheetOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accentColor = tint ?: Sakura80

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = accentColor.copy(alpha = 0.12f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (tint != null) tint else colors.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CloneRepoDialog(
    cloneUrl: String,
    onCloneUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onClone: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clone Remote Repository", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = cloneUrl,
                    onValueChange = onCloneUrlChange,
                    label = { Text("Repository URL") },
                    placeholder = { Text("https://github.com/user/repo.git") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sakura80,
                        focusedLabelColor = Sakura80
                    )
                )

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
                            text = "The repository will be cloned to app storage",
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
                enabled = cloneUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Sakura80),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Clone")
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
fun RepoSpeedDial(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCloneRemote: () -> Unit,
    onAddLocal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, label = "fab_rotation")

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                SpeedDialAction(
                    icon = Icons.Default.CloudDownload,
                    label = "Clone Remote",
                    color = Color(0xFF2196F3),
                    onClick = {
                        onCloneRemote()
                        onExpandedChange(false)
                    }
                )
                SpeedDialAction(
                    icon = Icons.Default.Add,
                    label = "Add Local",
                    color = Color(0xFF4CAF50),
                    onClick = {
                        onAddLocal()
                        onExpandedChange(false)
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = Sakura80,
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
private fun SpeedDialAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
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
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            containerColor = color,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}
