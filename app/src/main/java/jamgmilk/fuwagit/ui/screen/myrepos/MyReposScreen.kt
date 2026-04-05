package jamgmilk.fuwagit.ui.screen.myrepos

import android.annotation.SuppressLint
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.ui.components.CleanPreviewDialog
import jamgmilk.fuwagit.ui.components.CleanResultDialog
import jamgmilk.fuwagit.ui.components.ConfigureRemoteDialog
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.theme.AppShapes
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReposScreen(
    myReposViewModel: MyReposViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAddRepository: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by myReposViewModel.uiState.collectAsStateWithLifecycle()
    val currentRepoInfo by myReposViewModel.currentRepoInfo.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val folders = uiState.repoItems
    val selectedTarget = currentRepoInfo.repoPath

    var showCleanDialog by remember { mutableStateOf(false) }
    var selectedItemForSheet by remember { mutableStateOf<RepoFolderItem?>(null) }
    var showRemoteDialog by remember { mutableStateOf<RepoFolderItem?>(null) }
    var showInfoDialog by remember { mutableStateOf<RepoFolderItem?>(null) }

    val sheetState = rememberModalBottomSheetState()

    var remoteUrlState by remember { mutableStateOf("") }
    var repoInfoState by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        myReposViewModel.initializeStorage(context)
        myReposViewModel.loadCredentials()
    }

    Box(modifier = modifier.fillMaxSize()) {
        ScreenTemplate(
            title = "My Repos",
            modifier = Modifier.fillMaxSize()
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, FuwaGitThemeExtras.colors.cardBorder, AppShapes.medium),
                shape = AppShapes.medium,
                colors = CardDefaults.elevatedCardColors(containerColor = FuwaGitThemeExtras.colors.cardContainer),
                elevation = CardDefaults.elevatedCardElevation(0.dp)
            ) {
                if (folders.isEmpty()) {
                    EmptyReposState()
                } else {
                    RepoListContent(
                        folders = folders,
                        selectedTarget = selectedTarget,
                        onSetTarget = { path ->
                            scope.launch {
                                myReposViewModel.setCurrentRepo(path)
                            }
                        },
                        onItemLongClick = { selectedItemForSheet = it }
                    )
                }
            }
        }

        RepoSpeedDial(
            onAddRepository = onNavigateToAddRepository,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
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
                    scope.launch {
                        myReposViewModel.removeRepo(item)
                    }
                    selectedItemForSheet = null
                }
            },
            onConfigureRemote = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showRemoteDialog = item
                    selectedItemForSheet = null
                }
            },
            onClean = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    scope.launch {
                        myReposViewModel.setCurrentRepo(item.path)
                    }
                    selectedItemForSheet = null
                    showCleanDialog = true
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
            remoteUrlState = myReposViewModel.getRemoteUrl(item.path) ?: ""
        }

        ConfigureRemoteDialog(
            repoName = item.alias.ifBlank { item.path.substringAfterLast("/") },
            currentUrl = remoteUrlState,
            httpsCredentials = uiState.httpsCredentials,
            sshKeys = uiState.sshKeys,
            onDismiss = {
                showRemoteDialog = null
                remoteUrlState = ""
            },
            onSave = { newUrl, httpsUuid, sshUuid ->
                myReposViewModel.configureRemote(item.path, "origin", newUrl, httpsUuid, sshUuid)
                showRemoteDialog = null
                remoteUrlState = ""
            }
        )
    }

    showInfoDialog?.let { item ->
        LaunchedEffect(item) {
            repoInfoState = myReposViewModel.getRepoInfo(item.path)
        }

        RepoInfoDialog(
            repoName = item.alias.ifBlank { item.path.substringAfterLast("/") },
            repoPath = item.path,
            isGitRepo = item.isGitRepo,
            repoInfo = repoInfoState,
            onDismiss = {
                showInfoDialog = null
                repoInfoState = emptyMap()
            }
        )
    }

    // Clean 棰?瑙堝璇濇??細鏄剧ず灏?瑕佸垹闄ょ?鏂?浠跺垪琛?
    val untrackedFiles = uiState.untrackedFilesForClean
    val isCleanPreviewing = uiState.isCleanPreviewing
    val cleanMessage = uiState.cleanMessage

    if (untrackedFiles.isNotEmpty() || cleanMessage != null || isCleanPreviewing) {
        CleanPreviewDialog(
            untrackedFiles = untrackedFiles,
            message = if (isCleanPreviewing) "Scanning for untracked files..." else cleanMessage,
            onConfirm = { myReposViewModel.confirmCleanUntracked() },
            onDismiss = { myReposViewModel.clearCleanPreview() }
        )
    }

    // Clean 缁撴灉瀵硅瘽妗??細鏄剧ず宸插?闄ょ?鏂?浠跺垪琛?
    val cleanedFiles = uiState.cleanedFilesForResult
    if (cleanedFiles.isNotEmpty()) {
        CleanResultDialog(
            cleanedFiles = cleanedFiles,
            onSuccess = { myReposViewModel.loadSavedRepos() },
            onDismiss = { myReposViewModel.clearCleanResult() }
        )
    }

    // Clean 纭瀵硅瘽妗??細璇锋眰棰?瑙?
    if (showCleanDialog && untrackedFiles.isEmpty()) {
        AlertDialog(
            onDismissRequest = { showCleanDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFFF9800).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Clean Untracked Files",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Remove untracked files from the working directory. This action cannot be undone. Click 'Preview' to see which files will be deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentRepoInfo.repoPath?.let { path ->
                            myReposViewModel.requestCleanPreview()
                        }
                        showCleanDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Preview")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            items(folders, key = { it.path }, contentType = { "repo_item" }) { item ->
                RepoItemCard(
                    item = item,
                    isSelected = item.path == selectedTarget,
                    onClick = { onSetTarget(item.path) },
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
                color = if (isSelected) FuwaGitThemeExtras.colors.mizuiroAccent else uiColors.cardBorder,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.08f) else colors.surface.copy(alpha = 0.5f)
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
                color = if (isSelected) FuwaGitThemeExtras.colors.mizuiroAccent else accentColor.copy(alpha = 0.15f),
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
                        text = item.alias.ifBlank { item.path.substringAfterLast("/") },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FuwaGitThemeExtras.colors.mizuiroAccent
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

                    if (!item.isGitRepo) {
                        Row(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .background(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Not a Git",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                    }

                }

                Text(
                    text = item.shortPath,
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
                            imageVector = Icons.Default.Source,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = item.formattedSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
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
                color = FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.1f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.6f),
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

@SuppressLint("UseKtx")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoOptionsSheet(
    item: RepoFolderItem,
    sheetState: SheetState,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onConfigureRemote: () -> Unit,
    onClean: () -> Unit,
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
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp)
        ) {
            RepoHeader(
                item = item,
                onCopyPath = {
                    clipboardManager.setText(AnnotatedString(item.path))
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                color = colors.outline.copy(alpha = 0.15f)
            )

            Text(
                text = "OPTIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RepoOptionItem(
                    icon = Icons.Default.Link,
                    title = "Configure Remote",
                    subtitle = "Set push/pull remote URL",
                    accentColor = Color(0xFF2196F3),
                    onClick = onConfigureRemote
                )

            }

            HorizontalDivider(
                modifier = Modifier.padding(12.dp),
                color = colors.outline.copy(alpha = 0.15f)
            )

            Text(
                text = "DANGER ZONE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.error.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )

            RepoOptionItem(
                icon = Icons.Default.CleaningServices,
                title = "Clean Repository",
                subtitle = "Remove untracked files",
                accentColor = Color(0xFFE91E63),
                onClick = onClean
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
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (item.isGitRepo) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.alias.ifBlank { item.path.substringAfterLast("/") },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            //Spacer(Modifier.height(6.dp))

            Text(
                text = item.path,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                        onCopyPath()
                        Toast.makeText(context, "Path copied", Toast.LENGTH_SHORT).show()
                    }
                )
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
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun RepoInfoDialog(
    repoName: String,
    repoPath: String,
    isGitRepo: Boolean,
    repoInfo: Map<String, String>,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
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
            Column(modifier = Modifier.fillMaxWidth()) {
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
                    items(repoInfo.toList(), key = { it.first }, contentType = { "info_item" }) { (key, value) ->
                        RepoInfoItem(
                            icon = getInfoIcon(key),
                            label = key,
                            value = value,
                            accentColor = getInfoColor(key),
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(value))
                            }
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

@Composable
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
        else -> FuwaGitThemeExtras.colors.mizuiroAccent
    }
}

@Composable
fun RepoSpeedDial(
    onAddRepository: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onAddRepository,
        containerColor = FuwaGitThemeExtras.colors.mizuiroAccent,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
        shape = AppShapes.medium,
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add Repository"
        )
    }
}
