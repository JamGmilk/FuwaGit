package jamgmilk.fuwagit.ui.screen.myrepos

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.ui.components.CleanPreviewDialog
import jamgmilk.fuwagit.ui.components.CleanResultDialog
import jamgmilk.fuwagit.ui.components.ConfigureRemoteDialog
import jamgmilk.fuwagit.ui.components.EmptyState
import jamgmilk.fuwagit.ui.screen.credentials.CredentialType
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.state.RepoInfo
import jamgmilk.fuwagit.ui.theme.AppShapes
import jamgmilk.fuwagit.ui.util.ViewModelMessagesMapper
import kotlinx.coroutines.launch
import java.text.DateFormat
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

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(Unit) {
        myReposViewModel.initializeStorage(context)
        myReposViewModel.loadCredentials()
    }

    val itemForSheet = remember { mutableStateOf<RepoFolderItem?>(null) }
    val pendingItemForDialog = remember { mutableStateOf<RepoFolderItem?>(null) }
    val showConfigureRemoteDialog = remember { mutableStateOf(false) }
    val showCleanConfirmationDialog = remember { mutableStateOf(false) }
    val showRepoInfoDialog = remember { mutableStateOf(false) }
    val showDeleteConfirmationDialog = remember { mutableStateOf(false) }
    val pendingDeleteItem = remember { mutableStateOf<RepoFolderItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        ScreenTemplate(
            title = stringResource(R.string.myrepos_screen_title),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShapes.medium),
                shape = AppShapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                if (folders.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.FolderOpen,
                        title = stringResource(R.string.myrepos_no_repos_title),
                        description = stringResource(R.string.myrepos_no_repos_description),
                        modifier = modifier
                    )
                } else {
                    RepoListContent(
                        folders = folders,
                        selectedTarget = selectedTarget,
                        onSetTarget = { path ->
                            scope.launch {
                                myReposViewModel.setCurrentRepo(path)
                            }
                        },
                        onItemLongClick = { item ->
                            itemForSheet.value = item
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onNavigateToAddRepository,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = AppShapes.medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.myrepos_add_repository_description)
            )
        }
    }

    itemForSheet.value?.let { item ->
        RepoOptionsSheet(
            item = item,
            sheetState = sheetState,
            onDismiss = { itemForSheet.value = null },
            onRemove = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    pendingDeleteItem.value = item
                    showDeleteConfirmationDialog.value = true
                    itemForSheet.value = null
                }
            },
            onConfigureRemote = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    pendingItemForDialog.value = item
                    itemForSheet.value = null
                    showConfigureRemoteDialog.value = true
                }
            },
            onClean = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    scope.launch { myReposViewModel.setCurrentRepo(item.path) }
                    pendingItemForDialog.value = item
                    itemForSheet.value = null
                    showCleanConfirmationDialog.value = true
                }
            },
            onShowInfo = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    pendingItemForDialog.value = item
                    itemForSheet.value = null
                    showRepoInfoDialog.value = true
                }
            }
        )
    }

    if (showConfigureRemoteDialog.value && pendingItemForDialog.value != null) {
        val item = pendingItemForDialog.value!!
        var remoteUrl by remember { mutableStateOf("") }
        var isUrlLoaded by remember { mutableStateOf(false) }
        val savedRepo = uiState.savedRepos.find { it.path == item.path }
        val savedCredentialId = savedRepo?.credentialId
        val savedCredentialType = when {
            savedCredentialId != null && uiState.httpsCredentials.any { it.uuid == savedCredentialId } -> CredentialType.HTTPS
            savedCredentialId != null && uiState.sshKeys.any { it.uuid == savedCredentialId } -> CredentialType.SSH
            else -> null
        }

        LaunchedEffect(item) {
            remoteUrl = myReposViewModel.getRemoteUrl(item.path) ?: ""
            isUrlLoaded = true
        }

        if (isUrlLoaded) {
            ConfigureRemoteDialog(
                repoName = item.alias.ifBlank { item.path.substringAfterLast("/") },
                currentUrl = remoteUrl,
                selectedCredentialUuid = savedCredentialId,
                selectedCredentialType = savedCredentialType,
                httpsCredentials = uiState.httpsCredentials,
                sshKeys = uiState.sshKeys,
                onDismiss = {
                    showConfigureRemoteDialog.value = false
                    pendingItemForDialog.value = null
                    isUrlLoaded = false
                },
                onSave = { newUrl, httpsUuid, sshUuid ->
                    scope.launch {
                        myReposViewModel.configureRemote(item.path, "origin", newUrl, httpsUuid, sshUuid)
                    }
                    showConfigureRemoteDialog.value = false
                    pendingItemForDialog.value = null
                    isUrlLoaded = false
                }
            )
        }
    }

    if (showCleanConfirmationDialog.value && pendingItemForDialog.value != null) {
        val item = pendingItemForDialog.value!!
        ShowCleanConfirmationDialog(
            repoPath = item.path,
            onDismiss = {
                showCleanConfirmationDialog.value = false
                pendingItemForDialog.value = null
            },
            onRequestPreview = {
                myReposViewModel.requestCleanPreview()
            }
        )
    }

    if (showRepoInfoDialog.value && pendingItemForDialog.value != null) {
        val item = pendingItemForDialog.value!!
        var repoInfo by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        var repoGitConfig by remember { mutableStateOf("") }

        LaunchedEffect(item) {
            repoInfo = myReposViewModel.getRepoInfo(item.path)
            repoGitConfig = myReposViewModel.getRepoGitConfig(item.path)
        }

        RepoInfoDialog(
            repoName = item.alias.ifBlank { item.path.substringAfterLast("/") },
            repoPath = item.path,
            isGitRepo = item.isGitRepo,
            repoInfo = repoInfo,
            repoGitConfig = repoGitConfig,
            onDismiss = {
                showRepoInfoDialog.value = false
                pendingItemForDialog.value = null
            }
        )
    }

    if (showDeleteConfirmationDialog.value && pendingDeleteItem.value != null) {
        DeleteConfirmationDialog(
            item = pendingDeleteItem.value!!,
            onConfirm = {
                scope.launch {
                    myReposViewModel.removeRepo(pendingDeleteItem.value!!)
                    showDeleteConfirmationDialog.value = false
                    pendingDeleteItem.value = null
                }
            },
            onDismiss = {
                showDeleteConfirmationDialog.value = false
                pendingDeleteItem.value = null
            }
        )
    }

    CleanDialogs(
        uiState = uiState,
        currentRepoInfo = currentRepoInfo,
        myReposViewModel = myReposViewModel
    )
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.myrepos_tap_select_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
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

    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date()) }
    val lastModifiedText = if (item.lastModified > 0) {
        dateFormat.format(Date(item.lastModified))
    } else {
        stringResource(R.string.myrepos_unknown_date)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.small)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.primary else colors.outlineVariant,
                shape = AppShapes.small
            ),
        shape = AppShapes.small,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) colors.primary.copy(alpha = 0.1f) else colors.surface
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
                color = if (isSelected) colors.primary else colors.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.isGitRepo) Icons.Default.Folder else Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = if (isSelected) colors.onPrimary else colors.onSecondaryContainer,
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
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.primary
                        ) {
                            Text(
                                text = stringResource(R.string.myrepos_active_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!item.isGitRepo) {
                        Row(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .background(
                                    color = colors.errorContainer,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = colors.onErrorContainer,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = stringResource(R.string.myrepos_not_git_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onErrorContainer,
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
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoOptionsSheet(
    item: RepoFolderItem,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onConfigureRemote: () -> Unit,
    onClean: () -> Unit,
    onShowInfo: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        contentColor = colors.onSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            RepoHeader(
                item = item,
                onCopyPath = {
                    clipboardManager.setText(AnnotatedString(item.path))
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                color = colors.outlineVariant
            )

            Text(
                text = stringResource(R.string.myrepos_options_header),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RepoOptionsSheetItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.myrepos_show_info),
                    subtitle = stringResource(R.string.myrepos_show_info_subtitle),
                    accentColor = colors.primary,
                    onClick = onShowInfo
                )

                RepoOptionsSheetItem(
                    icon = Icons.Default.Link,
                    title = stringResource(R.string.myrepos_configure_remote),
                    subtitle = stringResource(R.string.myrepos_configure_remote_subtitle),
                    accentColor = colors.secondary,
                    onClick = onConfigureRemote
                )

            }

            HorizontalDivider(
                modifier = Modifier.padding(12.dp),
                color = colors.outlineVariant
            )

            Text(
                text = stringResource(R.string.myrepos_danger_zone),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.error.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RepoOptionsSheetItem(
                    icon = Icons.Default.CleaningServices,
                    title = stringResource(R.string.myrepos_clean_repository),
                    subtitle = stringResource(R.string.myrepos_clean_repository_subtitle),
                    accentColor = colors.error,
                    onClick = onClean
                )

                RepoOptionsSheetItem(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.myrepos_remove_from_list),
                    subtitle = stringResource(R.string.myrepos_remove_from_list_subtitle),
                    accentColor = colors.error,
                    onClick = onRemove
                )
            }
        }
    }
}

@Composable
private fun RepoHeader(
    item: RepoFolderItem,
    onCopyPath: () -> Unit
) {
    val context = LocalContext.current
    
    // Pre-fetch strings for use in non-composable contexts
    val strPathCopied = stringResource(R.string.myrepos_path_copied)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = AppShapes.small
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val colors = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (item.isGitRepo) colors.primary else colors.tertiary,
                    shape = AppShapes.extraSmall
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            //Spacer(Modifier.height(2.dp))

            Text(
                text = item.path,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            onCopyPath()
                            Toast.makeText(context, strPathCopied, Toast.LENGTH_SHORT).show()
                        }
                    )
            )
        }
    }
}

@Composable
fun RepoOptionsSheetItem(
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

// TODO: review
@Composable
fun RepoInfoDialog(
    repoName: String,
    repoPath: String,
    isGitRepo: Boolean,
    repoInfo: Map<String, String>,
    repoGitConfig: String = "",
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
                    .background(colors.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.myrepos_repo_info_title),
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
                        color = colors.tertiary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.myrepos_not_git_repo_warning),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.error
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
                            accentColor = MaterialTheme.colorScheme.primary,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(value))
                            }
                        )
                    }
                }

                if (repoGitConfig.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = ".git/config",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = repoGitConfig,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_close))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// TODO: review
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
                    contentDescription = stringResource(R.string.action_copy),
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
private fun ShowCleanConfirmationDialog(
    repoPath: String,
    onDismiss: () -> Unit,
    onRequestPreview: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.tertiary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = colors.tertiary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.myrepos_clean_untracked_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.myrepos_clean_untracked_description),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onRequestPreview()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_preview))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun CleanDialogs(
    uiState: RepoUiState,
    currentRepoInfo: RepoInfo,
    myReposViewModel: MyReposViewModel
) {
    val untrackedFiles = uiState.untrackedFilesForClean
    val isCleanPreviewing = uiState.isCleanPreviewing
    val cleanMessage = uiState.cleanMessage

    if (untrackedFiles.isNotEmpty() || cleanMessage != null || isCleanPreviewing) {
        val localizedMessage = if (cleanMessage != null) {
            stringResource(ViewModelMessagesMapper.mapMessageToResource(cleanMessage))
        } else null
        CleanPreviewDialog(
            untrackedFiles = untrackedFiles,
            message = if (isCleanPreviewing) stringResource(R.string.myrepos_scanning_files) else localizedMessage,
            onConfirm = { myReposViewModel.confirmCleanUntracked() },
            onDismiss = { myReposViewModel.clearCleanPreview() }
        )
    }

    val cleanedFiles = uiState.cleanedFilesForResult
    if (cleanedFiles.isNotEmpty()) {
        CleanResultDialog(
            cleanedFiles = cleanedFiles,
            onSuccess = { myReposViewModel.loadSavedRepos() },
            onDismiss = { myReposViewModel.clearCleanResult() }
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    item: RepoFolderItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onDismiss()
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(colors.error.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.myrepos_delete_confirm_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.myrepos_delete_confirm_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = item.alias.ifBlank { item.path.substringAfterLast("/") },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                            }
                            Text(
                                text = item.path,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.myrepos_delete_confirm_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onConfirm()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    onDismiss()
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

