package jamgmilk.fuwagit.ui.screen.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitResetMode
import jamgmilk.fuwagit.ui.components.ResetConfirmDialog
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.theme.AppColors
import jamgmilk.fuwagit.ui.theme.FuwaGitTheme
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by historyViewModel.uiState.collectAsState()
    val history = uiState.commits
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    ScreenTemplate(
        title = "History",
        modifier = modifier,
        actions = {
            Text(
                text = "${history.size} commits",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            if (history.isEmpty()) {
                EmptyHistoryState()
            } else {
                CommitTimelineList(
                    commits = history,
                    viewModel = historyViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Reset 确认对话框
    val pendingCommit = uiState.pendingResetCommit
    val pendingMode = uiState.pendingResetMode
    if (pendingCommit != null && pendingMode != null) {
        ResetConfirmDialog(
            commit = pendingCommit,
            mode = pendingMode,
            onConfirm = { historyViewModel.confirmReset() },
            onDismiss = { historyViewModel.cancelReset() }
        )
    }
}

@Composable
private fun EmptyHistoryState() {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.Commit,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                "No commits yet",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                "Make your first commit to see history",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CommitTimelineList(
    commits: List<GitCommit>,
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(commits, key = { it.hash }) { commit ->
            CommitTimelineItem(
                commit = commit,
                isLast = commit == commits.last(),
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun CommitTimelineItem(
    commit: GitCommit,
    isLast: Boolean,
    viewModel: HistoryViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val timeFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val relativeTime = remember(commit.timestamp) {
        formatRelativeTime(commit.timestamp)
    }

    val branchColors = remember {
        listOf(
            AppColors.GitCyan,
            AppColors.GitGreen,
            AppColors.GitOrange,
            AppColors.GitPink,
            AppColors.GitPurple
        )
    }
    val lane = abs(commit.hash.hashCode()) % branchColors.size
    val mergeLane = abs((commit.hash + "m").hashCode()) % branchColors.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TimelineIndicator(
            color = branchColors[lane],
            isMerge = commit.isMerge,
            isLast = isLast,
            modifier = Modifier.width(40.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface.copy(alpha = 0.4f))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (commit.isMerge) {
                            MergeBadge()
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = commit.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = if (expanded) 3 else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CommitMetaItem(
                            icon = Icons.Default.Code,
                            text = commit.shortHash,
                            color = colors.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        CommitMetaItem(
                            icon = Icons.Default.Person,
                            text = commit.authorName,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        CommitMetaItem(
                            icon = Icons.Default.Schedule,
                            text = relativeTime,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                CommitDetails(
                    commit = commit,
                    timeFmt = timeFmt,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineIndicator(
    color: Color,
    isMerge: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .width(2.dp)
                .fillMaxSize()
        ) {
            if (!isLast) {
                drawLine(
                    color = colors.outlineVariant.copy(alpha = 0.5f),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
                .size(16.dp)
                .background(
                    if (isMerge) AppColors.GitPurple else color,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isMerge) {
                Icon(
                    Icons.AutoMirrored.Filled.MergeType,
                    contentDescription = "Merge",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
private fun MergeBadge() {
    Box(
        modifier = Modifier
            .background(
                AppColors.GitPurple.copy(alpha = 0.15f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MergeType,
                contentDescription = null,
                tint = AppColors.GitPurple,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "MERGE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.GitPurple,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun CommitMetaItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CommitDetails(
    commit: GitCommit,
    timeFmt: SimpleDateFormat,
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors
    val uiState by viewModel.uiState.collectAsState()
    val commitDetail = uiState.selectedCommitDetail
    val isLoadingDetail = uiState.isLoadingCommitDetail

    // 加载 commit 详情
    LaunchedEffect(commit.hash) {
        viewModel.loadCommitDetail(commit)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Commit 元信息
        DetailRow(
            icon = Icons.Default.Code,
            label = "Hash",
            value = commit.hash,
            isMonospace = true
        )

        DetailRow(
            icon = Icons.Default.Person,
            label = "Author",
            value = commit.authorName
        )

        DetailRow(
            icon = Icons.Default.Email,
            label = "Email",
            value = commit.authorEmail,
            isMonospace = true
        )

        DetailRow(
            icon = Icons.Default.Schedule,
            label = "Date",
            value = timeFmt.format(Date(commit.timestamp))
        )

        if (commit.isMerge) {
            DetailRow(
                icon = Icons.Default.AccountTree,
                label = "Parents",
                value = commit.parentHashes.map { it.take(7) }.joinToString(", ")
            )
        }

        // 文件变更统计
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        if (isLoadingDetail) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (commitDetail != null) {
            // 变更统计摘要
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(
                    value = "${commitDetail.totalFiles}",
                    label = "Files",
                    color = colors.primary
                )
                StatChip(
                    value = "+${commitDetail.totalAdditions}",
                    label = "Additions",
                    color = Color(0xFF4CAF50)
                )
                StatChip(
                    value = "-${commitDetail.totalDeletions}",
                    label = "Deletions",
                    color = Color(0xFFF44336)
                )
            }

            // 文件变更列表
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Changed Files",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )

            if (commitDetail.fileChanges.isEmpty()) {
                Text(
                    text = "No file changes",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    commitDetail.fileChanges.forEach { fileChange ->
                        FileChangeItem(fileChange = fileChange)
                    }
                }
            }
        }

        // Reset 操作按钮
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        var showResetMenu by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { showResetMenu = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Reset to this commit", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showResetMenu,
                onDismissRequest = { showResetMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Soft Reset", color = Color(0xFF4CAF50)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showResetMenu = false
                        viewModel.requestReset(commit, GitResetMode.SOFT)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Mixed Reset", color = Color(0xFFFF9800)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Replay,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showResetMenu = false
                        viewModel.requestReset(commit, GitResetMode.MIXED)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Hard Reset", color = Color(0xFFF44336)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showResetMenu = false
                        viewModel.requestReset(commit, GitResetMode.HARD)
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    val colors = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@Composable
private fun StatChip(
    value: String,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
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
}

@Composable
private fun FileChangeItem(
    fileChange: jamgmilk.fuwagit.domain.model.git.GitCommitFileChange
) {
    val colors = MaterialTheme.colorScheme
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = colors.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 变更类型图标
                    Icon(
                        imageVector = when (fileChange.changeType) {
                            jamgmilk.fuwagit.domain.model.git.GitChangeType.Added -> Icons.Default.Add
                            jamgmilk.fuwagit.domain.model.git.GitChangeType.Removed -> Icons.Default.Delete
                            jamgmilk.fuwagit.domain.model.git.GitChangeType.Renamed -> Icons.Default.Refresh
                            else -> Icons.Default.Edit
                        },
                        contentDescription = null,
                        tint = when (fileChange.changeType) {
                            jamgmilk.fuwagit.domain.model.git.GitChangeType.Added -> Color(0xFF4CAF50)
                            jamgmilk.fuwagit.domain.model.git.GitChangeType.Removed -> Color(0xFFF44336)
                            jamgmilk.fuwagit.domain.model.git.GitChangeType.Renamed -> Color(0xFF2196F3)
                            else -> Color(0xFFFF9800)
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = fileChange.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurfaceVariant
                    )
                }
                Text(
                    text = fileChange.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 行数变化
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (fileChange.additions > 0) {
                    Text(
                        text = "+${fileChange.additions}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                if (fileChange.deletions > 0) {
                    Text(
                        text = "-${fileChange.deletions}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}
