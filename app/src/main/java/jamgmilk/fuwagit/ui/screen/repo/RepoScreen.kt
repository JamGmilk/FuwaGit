package jamgmilk.fuwagit.ui.screen.repo

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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.ui.AppViewModel
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import jamgmilk.fuwagit.ui.theme.Sakura80
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoScreen(
    repoViewModel: RepoViewModel,
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onNavigateToStatus: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors
    val context = LocalContext.current
    val uiState by repoViewModel.uiState.collectAsState()
    val folders = uiState.repoItems
    val selectedTarget = uiState.targetPath
    val scope = rememberCoroutineScope()

    var showCloneDialog by remember { mutableStateOf(false) }
    var cloneUrl by remember { mutableStateOf("") }
    var expandedFab by remember { mutableStateOf(false) }
    var selectedItemForSheet by remember { mutableStateOf<RepoFolderItem?>(null) }
    var showRemoteDialog by remember { mutableStateOf<RepoFolderItem?>(null) }
    var showInfoDialog by remember { mutableStateOf<RepoFolderItem?>(null) }
    val sheetState = rememberModalBottomSheetState()

    var remoteUrlState by remember { mutableStateOf("") }
    var repoInfoState by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { uri ->
            repoViewModel.addGrantedTreeUri(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        repoViewModel.initializeStorage(context)
        repoViewModel.refreshPersistedUris(context)
        repoViewModel.refreshRepoItems(context)
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
                            repoViewModel.setTargetPath(context, path)
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
            context = context,
            onDismiss = { selectedItemForSheet = null },
            onRemove = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    repoViewModel.removeRepo(context, item)
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
        LaunchedEffect(item) {
            item.localPath?.let { path ->
                remoteUrlState = repoViewModel.getRemoteUrl(path) ?: ""
            }
        }

        ConfigureRemoteDialog(
            repoName = item.name,
            currentUrl = remoteUrlState,
            onDismiss = {
                showRemoteDialog = null
                remoteUrlState = ""
            },
            onSave = { newUrl ->
                item.localPath?.let { repoViewModel.configureRemote(it, "origin", newUrl) }
                showRemoteDialog = null
                remoteUrlState = ""
            }
        )
    }

    showInfoDialog?.let { item ->
        LaunchedEffect(item) {
            item.localPath?.let { path ->
                repoInfoState = repoViewModel.getRepoInfo(path)
            }
        }

        RepoInfoDialog(
            repoName = item.name,
            repoPath = item.path,
            isGitRepo = item.isGitRepo,
            repoInfo = repoInfoState,
            onDismiss = {
                showInfoDialog = null
                repoInfoState = emptyMap()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepoListContent(
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
fun RepoItemCard(
    item: RepoFolderItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

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
fun EmptyReposState(modifier: Modifier = Modifier) {
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
fun RepoOptionsSheet(
    item: RepoFolderItem,
    sheetState: SheetState,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onConfigureRemote: () -> Unit,
    onShowInfo: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = uiColors.cardContainer,
        contentColor = colors.onSurface,
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(32.dp, 4.dp),
                    shape = CircleShape,
                    color = colors.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            RepoHeader(
                item = item,
                onCopyPath = {
                    clipboardManager.setText(AnnotatedString(item.path))
                    scope.launch {
                        kotlinx.coroutines.delay(300)
                        onDismiss()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colors.outline.copy(alpha = 0.15f))
            Spacer(Modifier.height(12.dp))

            Text(
                text = "OPTIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RepoOptionItem(
                    icon = Icons.Default.Link,
                    title = "Configure Remote",
                    subtitle = "Set push/pull remote URL",
                    accentColor = Color(0xFF2196F3),
                    onClick = onConfigureRemote
                )

                RepoOptionItem(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy Path",
                    subtitle = item.path,
                    accentColor = Color(0xFF607D8B),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(item.path))
                    }
                )

                RepoOptionItem(
                    icon = Icons.Default.FolderShared,
                    title = "Open in Files",
                    subtitle = "Browse repository folder",
                    accentColor = Color(0xFF795548),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary:${item.path}"), "resource/folder")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("file://${item.path}")
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                // Fallback - copy to clipboard
                                clipboardManager.setText(AnnotatedString(item.path))
                            }
                        }
                        scope.launch {
                            kotlinx.coroutines.delay(300)
                            onDismiss()
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = colors.outline.copy(alpha = 0.15f))
            Spacer(Modifier.height(12.dp))

            Text(
                text = "DANGER ZONE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.error.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            RepoOptionItem(
                icon = Icons.Default.Delete,
                title = "Remove from List",
                subtitle = "Remove this repository from the list",
                accentColor = colors.error,
                onClick = onRemove
            )
        }
    }
}

@Composable
private fun RepoHeader(
    item: RepoFolderItem,
    onCopyPath: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accentColor = if (item.isGitRepo) Color(0xFF4CAF50) else Color(0xFFFF9800)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Sakura80.copy(alpha = 0.12f),
                        Sakura80.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (item.isGitRepo) Color(0xFF4CAF50) else Color(0xFFFF9800),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (item.isGitRepo) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (item.isGitRepo) "Git Repository" else "Not a Git",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onCopyPath,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy path",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = item.path,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RepoOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ConfigureRemoteDialog(
    repoName: String,
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    val colors = MaterialTheme.colorScheme

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
                    Icons.Default.Link,
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
                    placeholder = { Text("https://github.com/user/repo.git") },
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
                onClick = { onSave(url) },
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
fun RepoInfoDialog(
    repoName: String,
    repoPath: String,
    isGitRepo: Boolean,
    repoInfo: Map<String, String>,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF9C27B0).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Repository Info",
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
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isGitRepo) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFF9800).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "This directory is not a Git repository",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                LazyColumn(
                    modifier = Modifier.height(if (isGitRepo) 340.dp else 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(repoInfo.toList()) { (key, value) ->
                        RepoInfoItem(
                            icon = getInfoIcon(key),
                            label = key,
                            value = value,
                            accentColor = getInfoColor(key),
                            onCopy = { clipboardManager.setText(AnnotatedString(value)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun RepoInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    onCopy: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getInfoIcon(key: String): ImageVector {
    return when {
        key.contains("Branch", ignoreCase = true) -> Icons.Default.AccountTree
        key.contains("Commit", ignoreCase = true) -> Icons.Default.Source
        key.contains("Remote", ignoreCase = true) -> Icons.Default.Sync
        key.contains("Date", ignoreCase = true) -> Icons.Default.Schedule
        key.contains("Path", ignoreCase = true) -> Icons.Default.Folder
        key.contains("Status", ignoreCase = true) -> Icons.Default.CheckCircle
        key.contains("Hash", ignoreCase = true) -> Icons.Default.AccountTree
        else -> Icons.Default.Info
    }
}

private fun getInfoColor(key: String): Color {
    return when {
        key.contains("Branch", ignoreCase = true) -> Color(0xFF2196F3)
        key.contains("Commit", ignoreCase = true) -> Color(0xFF4CAF50)
        key.contains("Remote", ignoreCase = true) -> Color(0xFFFF5722)
        key.contains("Date", ignoreCase = true) -> Color(0xFF9C27B0)
        key.contains("Path", ignoreCase = true) -> Color(0xFF607D8B)
        key.contains("Status", ignoreCase = true) -> Color(0xFF4CAF50)
        key.contains("Hash", ignoreCase = true) -> Color(0xFF795548)
        key.contains("Error", ignoreCase = true) -> Color(0xFFE53935)
        else -> Sakura80
    }
}

@Composable
fun CloneRepoDialog(
    cloneUrl: String,
    onCloneUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onClone: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Sakura80.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Sakura80,
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
                            text = "Clone to app's internal storage",
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

@Composable
fun RepoSpeedDial(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCloneRemote: () -> Unit,
    onAddLocal: () -> Unit,
    modifier: Modifier = Modifier
) {
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
fun SpeedDialAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val uiColors = FuwaGitThemeExtras.colors

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
