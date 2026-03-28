package jamgmilk.fuwagit.ui.screen.status

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import jamgmilk.fuwagit.ui.components.RefreshAction
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.theme.FuwaGitTheme
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
    modifier: Modifier = Modifier
) {
    val uiState by statusViewModel.uiState.collectAsState()
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

    LaunchedEffect(uiState.repoPath) {
        if (uiState.repoPath != null) {
            statusViewModel.refreshAll()
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
                    onFetch = { },
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
                    accentColor = Color(0xFF4CAF50),
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
}

@Composable
private fun ActionToolbar(
    stats: StatusStats,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onFetch: () -> Unit,
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
                    color = Color(0xFF1E88E5),
                    enabled = true,
                    onClick = onPull,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.CloudUpload,
                    label = "Push",
                    color = Color(0xFF43A047),
                    enabled = true,
                    onClick = onPush,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Default.CloudDownload,
                    label = "Fetch",
                    color = Color(0xFF7B1FA2),
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
                    color = Color(0xFF4CAF50),
                    enabled = stats.unstaged + stats.untracked > 0,
                    onClick = onStageAll,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    label = "Unstage",
                    color = Color(0xFFFF9800),
                    enabled = stats.staged > 0,
                    onClick = onUnstageAll,
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
                            if (isRepo) Color(0xFF4CAF50).copy(alpha = 0.15f)
                            else colors.error.copy(alpha = 0.15f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRepo) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isRepo) Color(0xFF4CAF50) else colors.error,
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
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Modified",
            count = stats.modified,
            color = Color(0xFF1E88E5),
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Added",
            count = stats.added,
            color = Color(0xFF43A047),
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Removed",
            count = stats.removed,
            color = Color(0xFFE53935),
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
            addedRatio to Color(0xFF43A047),
            modifiedRatio to Color(0xFF1E88E5),
            removedRatio to Color(0xFFE53935),
            untrackedRatio to Color(0xFF78909C)
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
        GitChangeType.Added -> Color(0xFF43A047) to "A"
        GitChangeType.Modified -> Color(0xFF1E88E5) to "M"
        GitChangeType.Removed -> Color(0xFFE53935) to "D"
        GitChangeType.Untracked -> Color(0xFF78909C) to "?"
        GitChangeType.Renamed -> Color(0xFFFFA000) to "R"
        GitChangeType.Conflicting -> Color(0xFFD81B60) to "!"
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
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Pending,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$stagedCount staged",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
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

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun StatusModulePreview() {
    FuwaGitTheme {
        StatusScreen(statusViewModel = jamgmilk.fuwagit.di.AppContainer.createStatusViewModel())
    }
}
