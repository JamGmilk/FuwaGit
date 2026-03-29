package jamgmilk.fuwagit.ui.screen.status

import jamgmilk.fuwagit.ui.screen.repo.RepoViewModel

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.domain.model.GitBranch
import jamgmilk.fuwagit.domain.model.GitChangeType
import jamgmilk.fuwagit.domain.model.GitFileStatus
import jamgmilk.fuwagit.domain.model.GitRemote
import jamgmilk.fuwagit.domain.model.GitStash
import jamgmilk.fuwagit.domain.model.GitTag
import jamgmilk.fuwagit.ui.components.CleanDialog
import jamgmilk.fuwagit.ui.components.RefreshAction
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.theme.FuwaGitTheme
import jamgmilk.fuwagit.ui.theme.AppColors
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import jamgmilk.fuwagit.ui.theme.Sakura50
import jamgmilk.fuwagit.ui.theme.Sakura80
import jamgmilk.fuwagit.ui.theme.Sakura90

data class StatusStats(
    val totalChanges: Int,
    val staged: Int,
    val unstaged: Int,
    val untracked: Int,
    val modified: Int,
    val added: Int,
    val removed: Int
)

@Composable
fun StatusScreen(
    statusViewModel: StatusViewModel,
    repoViewModel: RepoViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by statusViewModel.uiState.collectAsState()
    val repoUiState by repoViewModel.uiState.collectAsState()
    val files = uiState.workspaceFiles
    val staged = remember(files) { files.filter { it.isStaged } }
    val workspace = remember(files) { files.filter { !it.isStaged } }
    var commitMessage by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    val isRepo = uiState.isGitRepo
    val statusText = uiState.statusMessage
    val terminalLogs = uiState.terminalOutput
    val targetPath = uiState.repoPath
    val currentBranch = uiState.currentBranch
    val stashList = uiState.stashList
    val tagList = uiState.tagList
    val remoteList = uiState.remoteList

    LaunchedEffect(repoUiState.targetPath) {
        statusViewModel.setRepoPath(repoUiState.targetPath)
    }

    var showStashDialog by remember { mutableStateOf(false) }
    var showStashListDialog by remember { mutableStateOf(false) }
    var selectedStash by remember { mutableStateOf<GitStash?>(null) }
    var showApplyStashDialog by remember { mutableStateOf(false) }
    var showDropStashDialog by remember { mutableStateOf(false) }

    var showTagDialog by remember { mutableStateOf(false) }
    var showTagListDialog by remember { mutableStateOf(false) }
    var showRemoteListDialog by remember { mutableStateOf(false) }
    var showRenameBranchDialog by remember { mutableStateOf(false) }
    var showCleanDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        if (uiState.repoPath != null) {
            statusViewModel.refreshAll()
            statusViewModel.refreshStashList()
            statusViewModel.refreshTagList()
            statusViewModel.refreshRemoteList()
        }
    }

    LaunchedEffect(uiState.repoPath) {
        if (uiState.repoPath != null) {
            statusViewModel.refreshStashList()
            statusViewModel.refreshTagList()
            statusViewModel.refreshRemoteList()
        }
    }

    val statusStats = remember(files, staged, workspace) {
        StatusStats(
            totalChanges = files.size,
            staged = staged.size,
            unstaged = workspace.count { it.changeType != GitChangeType.Untracked },
            untracked = workspace.count { it.changeType == GitChangeType.Untracked },
            modified = files.count { it.changeType == GitChangeType.Modified },
            added = files.count { it.changeType == GitChangeType.Added },
            removed = files.count { it.changeType == GitChangeType.Removed }
        )
    }

    ScreenTemplate(
        title = "Status",
        modifier = modifier,
        actions = {
            RefreshAction(onRefresh = { statusViewModel.refreshWorkspace() })
        }
    ) {
        RepositoryStatusCard(
            isRepo = isRepo,
            statusText = statusText,
            targetPath = targetPath,
            currentBranch = currentBranch,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(
            visible = isRepo,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionToolbar(
                    stats = statusStats,
                    onStageAll = { statusViewModel.stageAll() },
                    onUnstageAll = { statusViewModel.unstageAll() },
                    onPull = { statusViewModel.pullRepo() },
                    onPush = { statusViewModel.pushRepo() },
                    onStash = { showStashDialog = true },
                    onShowStashList = { showStashListDialog = true },
                    onFetch = { statusViewModel.fetchRepo() },
                    onTag = { showTagDialog = true },
                    onShowTagList = { showTagListDialog = true },
                    onShowRemoteList = { showRemoteListDialog = true },
                    onRenameBranch = { showRenameBranchDialog = true },
                    onClean = { showCleanDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                ChangesOverviewCard(
                    stats = statusStats,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        AnimatedVisibility(
            visible = isRepo,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FileSectionCard(
                    title = "Workspace",
                    subtitle = "Unstaged changes",
                    files = workspace,
                    modifier = Modifier.weight(1f),
                    accentColor = Sakura80,
                    onFileAction = { file ->
                        when (file.changeType) {
                            GitChangeType.Untracked -> statusViewModel.stageFile(file.path)
                            GitChangeType.Removed -> statusViewModel.discardChanges(file.path)
                            else -> statusViewModel.stageFile(file.path)
                        }
                    },
                    emptyMessage = "Working directory clean"
                )

                FileSectionCard(
                    title = "Staged",
                    subtitle = "Ready to commit",
                    files = staged,
                    modifier = Modifier.weight(1f),
                    accentColor = AppColors.GitGreen,
                    onFileAction = { file -> statusViewModel.unstageFile(file.path) },
                    emptyMessage = "Nothing to commit"
                )
            }
        }

        AnimatedVisibility(
            visible = isRepo,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            CommitCard(
                commitMessage = commitMessage,
                onCommitMessageChange = { commitMessage = it },
                onCommit = {
                    if (commitMessage.isNotBlank()) {
                        statusViewModel.commitChanges(commitMessage)
                        commitMessage = ""
                    }
                },
                stagedCount = staged.size,
                modifier = Modifier.fillMaxWidth()
            )
        }

        TerminalLogsCard(
            logs = terminalLogs,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )
    }

    if (showStashDialog) {
        StashDialog(
            onDismiss = { showStashDialog = false },
            onStash = { message ->
                statusViewModel.stashChanges(message)
                showStashDialog = false
            }
        )
    }

    if (showStashListDialog) {
        StashListDialog(
            stashList = stashList,
            onDismiss = { showStashListDialog = false },
            onApplyStash = { stash ->
                statusViewModel.applyStash(stash.index)
                showStashListDialog = false
            },
            onDropStash = { stash ->
                statusViewModel.dropStash(stash.index)
            }
        )
    }

    if (showTagDialog) {
        TagDialog(
            onDismiss = { showTagDialog = false },
            onCreateTag = { tagName, message ->
                statusViewModel.createTag(tagName, message)
                showTagDialog = false
            }
        )
    }

    if (showTagListDialog) {
        TagListDialog(
            tagList = tagList,
            onDismiss = { showTagListDialog = false },
            onDeleteTag = { tag ->
                statusViewModel.deleteTag(tag.name)
            }
        )
    }

    if (showRemoteListDialog) {
        RemoteListDialog(
            remoteList = remoteList,
            onDismiss = { showRemoteListDialog = false },
            onDeleteRemote = { remote ->
                statusViewModel.deleteRemote(remote.name)
            }
        )
    }

    if (showRenameBranchDialog) {
        RenameBranchDialog(
            currentBranch = currentBranch?.name ?: "",
            onDismiss = { showRenameBranchDialog = false },
            onRename = { oldName, newName ->
                statusViewModel.renameBranch(oldName, newName)
                showRenameBranchDialog = false
            }
        )
    }

    if (showCleanDialog) {
        CleanDialog(
            onDismiss = { showCleanDialog = false },
            onClean = { dryRun ->
                statusViewModel.cleanRepo(dryRun)
                showCleanDialog = false
            }
        )
    }
}

@Composable
private fun ActionToolbar(
    stats: StatusStats,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onStash: () -> Unit,
    onShowStashList: () -> Unit,
    onFetch: () -> Unit,
    onTag: () -> Unit,
    onShowTagList: () -> Unit,
    onShowRemoteList: () -> Unit,
    onRenameBranch: () -> Unit,
    onClean: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.CloudDownload,
                    label = "Pull",
                    color = AppColors.GitBlue,
                    enabled = true,
                    onClick = onPull,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.CloudUpload,
                    label = "Push",
                    color = AppColors.GitGreen,
                    enabled = true,
                    onClick = onPush,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.CloudDownload,
                    label = "Fetch",
                    color = AppColors.GitPurple,
                    enabled = true,
                    onClick = onFetch,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.Check,
                    label = "Stage All",
                    color = AppColors.GitGreen,
                    enabled = stats.unstaged + stats.untracked > 0,
                    onClick = onStageAll,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    label = "Unstage",
                    color = AppColors.GitOrange,
                    enabled = stats.staged > 0,
                    onClick = onUnstageAll,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.Archive,
                    label = "Stash",
                    color = AppColors.GitBrown,
                    enabled = stats.unstaged + stats.untracked > 0 || stats.staged > 0,
                    onClick = onStash,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Archive,
                    label = "Stash List",
                    color = AppColors.GitGrey,
                    enabled = true,
                    onClick = onShowStashList,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.LocalOffer,
                    label = "Tag",
                    color = AppColors.GitPink,
                    enabled = true,
                    onClick = onTag,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.LocalOffer,
                    label = "Tag List",
                    color = AppColors.GitPurple,
                    enabled = true,
                    onClick = onShowTagList,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    icon = Icons.Default.Cloud,
                    label = "Remotes",
                    color = AppColors.GitCyan,
                    enabled = true,
                    onClick = onShowRemoteList,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Edit,
                    label = "Rename",
                    color = AppColors.GitDeepOrange,
                    enabled = true,
                    onClick = onRenameBranch,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.Delete,
                    label = "Clean",
                    color = AppColors.GitRed,
                    enabled = stats.untracked > 0,
                    onClick = onClean,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) color.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) color else colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) color else colors.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun RepositoryStatusCard(
    isRepo: Boolean,
    statusText: String,
    targetPath: String?,
    currentBranch: GitBranch?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isRepo) uiColors.cardContainer else colors.errorContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isRepo) AppColors.GitGreen.copy(alpha = 0.15f)
                            else colors.error.copy(alpha = 0.15f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRepo) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isRepo) AppColors.GitGreen else colors.error,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRepo) "Repository Active" else "Not a Git Repository",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isRepo) colors.primary else colors.error
                    )
                    targetPath?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isRepo && currentBranch != null) {
                Spacer(Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Sakura50.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = Sakura80,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Current branch:",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = currentBranch.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Sakura90
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangesOverviewCard(
    stats: StatusStats,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Changes Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${stats.totalChanges} files",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            StatChipsRow(stats = stats)

            if (stats.totalChanges > 0) {
                ChangeTypeBarChart(
                    stats = stats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                )
            }
        }
    }
}

@Composable
private fun StatChipsRow(stats: StatusStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(
            label = "Staged",
            count = stats.staged,
            color = AppColors.GitGreen,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Modified",
            count = stats.modified,
            color = AppColors.GitBlue,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Added",
            count = stats.added,
            color = AppColors.GitGreen,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Removed",
            count = stats.removed,
            color = AppColors.GitRed,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ChangeTypeBarChart(
    stats: StatusStats,
    modifier: Modifier = Modifier
) {
    val total = stats.totalChanges.toFloat().coerceAtLeast(1f)
    val addedRatio = stats.added / total
    val modifiedRatio = stats.modified / total
    val removedRatio = stats.removed / total
    val untrackedRatio = stats.untracked / total

    Canvas(modifier = modifier) {
        val cornerRadius = 5.dp.toPx()
        var currentX = 0f
        val gap = 2.dp.toPx()

        val segments = listOf(
            addedRatio to AppColors.GitGreen,
            modifiedRatio to AppColors.GitBlue,
            removedRatio to AppColors.GitRed,
            untrackedRatio to AppColors.GitBlueGrey
        )

        segments.forEach { (ratio, color) ->
            if (ratio > 0.01f) {
                val width = (size.width - gap * 3) * ratio
                drawRoundRect(
                    color = color,
                    topLeft = Offset(currentX, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
                currentX += width + gap
            }
        }
    }
}

@Composable
private fun FileSectionCard(
    title: String,
    subtitle: String,
    files: List<GitFileStatus>,
    modifier: Modifier = Modifier,
    accentColor: Color,
    onFileAction: (GitFileStatus) -> Unit,
    emptyMessage: String
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    ElevatedCard(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = accentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor
                    ) {
                        Text(
                            text = files.size.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(files, key = { "${it.path}:${it.isStaged}" }) { file ->
                        FileStatusItem(
                            file = file,
                            accentColor = accentColor,
                            onAction = { onFileAction(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileStatusItem(
    file: GitFileStatus,
    accentColor: Color,
    onAction: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val (changeColor, changeLabel) = when (file.changeType) {
        GitChangeType.Added -> AppColors.GitGreen to "A"
        GitChangeType.Modified -> AppColors.GitBlue to "M"
        GitChangeType.Removed -> AppColors.GitRed to "D"
        GitChangeType.Untracked -> AppColors.GitBlueGrey to "?"
        GitChangeType.Renamed -> AppColors.GitAmber to "R"
        GitChangeType.Conflicting -> AppColors.GitDarkPink to "!"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onAction),
        color = colors.surface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = changeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = changeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = changeColor,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CommitCard(
    commitMessage: String,
    onCommitMessageChange: (String) -> Unit,
    onCommit: () -> Unit,
    stagedCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors
    val canCommit = commitMessage.isNotBlank() && stagedCount > 0

    ElevatedCard(
        modifier = modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Sakura80.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Sakura80,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Commit Changes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (stagedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.GitGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Pending,
                                contentDescription = null,
                                tint = AppColors.GitGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$stagedCount staged",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.GitGreen
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = commitMessage,
                onValueChange = onCommitMessageChange,
                placeholder = {
                    Text(
                        "Enter commit message...",
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Sakura80,
                    unfocusedBorderColor = colors.outline.copy(alpha = 0.3f)
                )
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = canCommit, onClick = onCommit),
                shape = RoundedCornerShape(14.dp),
                color = if (canCommit) Sakura80 else colors.surfaceVariant
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = if (canCommit) Color.White else colors.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Commit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (canCommit) Color.White else colors.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalLogsCard(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    ElevatedCard(
        modifier = modifier.border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.terminalBackground),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terminal",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant
                )
                if (logs.isNotEmpty()) {
                    Text(
                        text = "${logs.size} lines",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(uiColors.terminalBackground)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No output yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(logs) { log ->
                            Text(
                                text = log,
                                color = uiColors.terminalText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StashDialog(
    onDismiss: () -> Unit,
    onStash: (String?) -> Unit
) {
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(AppColors.GitBrown.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Archive,
                    contentDescription = null,
                    tint = AppColors.GitBrown,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Stash Changes",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Stash your current changes to apply them later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message (optional)") },
                    placeholder = { Text("Enter stash message...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF795548),
                        focusedLabelColor = Color(0xFF795548)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onStash(message.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Stash")
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
fun StashListDialog(
    stashList: List<GitStash>,
    onDismiss: () -> Unit,
    onApplyStash: (GitStash) -> Unit,
    onDropStash: (GitStash) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF607D8B).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Archive,
                    contentDescription = null,
                    tint = Color(0xFF607D8B),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Stash List",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (stashList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = null,
                            tint = Color(0xFF607D8B).copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No stashes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stashList) { stash ->
                        StashItemCard(
                            stash = stash,
                            onApply = { onApplyStash(stash) },
                            onDrop = { onDropStash(stash) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun StashItemCard(
    stash: GitStash,
    onApply: () -> Unit,
    onDrop: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF607D8B).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "stash@{${stash.index}}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF607D8B),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onApply,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Apply",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDrop,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Drop",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stash.message,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TagDialog(
    onDismiss: () -> Unit,
    onCreateTag: (String, String?) -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFE91E63).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Create Tag",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Create a new tag at the current commit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag name") },
                    placeholder = { Text("e.g., v1.0.0") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE91E63),
                        focusedLabelColor = Color(0xFFE91E63)
                    )
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message (optional)") },
                    placeholder = { Text("Enter tag message...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE91E63),
                        focusedLabelColor = Color(0xFFE91E63)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateTag(tagName, message.ifBlank { null }) },
                enabled = tagName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Create")
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
fun TagListDialog(
    tagList: List<GitTag>,
    onDismiss: () -> Unit,
    onDeleteTag: (GitTag) -> Unit
) {
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
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Tags",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (tagList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = Color(0xFF9C27B0).copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No tags",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tagList) { tag ->
                        TagItemCard(
                            tag = tag,
                            onDelete = { onDeleteTag(tag) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun TagItemCard(
    tag: GitTag,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF9C27B0).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF9C27B0),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = tag.commitHash,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun RemoteListDialog(
    remoteList: List<GitRemote>,
    onDismiss: () -> Unit,
    onDeleteRemote: (GitRemote) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF00BCD4).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color(0xFF00BCD4),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Remotes",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (remoteList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            tint = Color(0xFF00BCD4).copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No remotes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(remoteList) { remote ->
                        RemoteItemCard(
                            remote = remote,
                            onDelete = { onDeleteRemote(remote) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun RemoteItemCard(
    remote: GitRemote,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surface),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF00BCD4).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = remote.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF00BCD4),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Fetch: ${remote.fetchUrl}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            remote.pushUrl?.let { pushUrl ->
                if (pushUrl.isNotBlank()) {
                    Text(
                        text = "Push: $pushUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun RenameBranchDialog(
    currentBranch: String,
    onDismiss: () -> Unit,
    onRename: (String, String) -> Unit
) {
    var newName by remember { mutableStateOf(currentBranch) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFFF5722).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFFFF5722),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Rename Branch",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Rename branch \"$currentBranch\" to a new name.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New branch name") },
                    placeholder = { Text("Enter new name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF5722),
                        focusedLabelColor = Color(0xFFFF5722)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onRename(currentBranch, newName) },
                enabled = newName.isNotBlank() && newName != currentBranch,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Rename")
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
