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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Commit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.domain.model.GitCommit
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.theme.AppColors
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

    LaunchedEffect(uiState.repoPath) {
        if (uiState.repoPath != null) {
            historyViewModel.loadCommitHistory()
        }
    }

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
                .height(500.dp)
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
                    onRevert = { commit -> historyViewModel.revertCommit(commit.hash) },
                    onCherrypick = { commit -> historyViewModel.cherryPick(commit.hash) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
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
    onRevert: (GitCommit) -> Unit,
    onCherrypick: (GitCommit) -> Unit,
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
                onRevert = { onRevert(commit) },
                onCherrypick = { onCherrypick(commit) }
            )
        }
    }
}

@Composable
private fun CommitTimelineItem(
    commit: GitCommit,
    isLast: Boolean,
    onRevert: () -> Unit,
    onCherrypick: () -> Unit
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CommitMetaItem(
                            icon = Icons.Default.Code,
                            text = commit.shortHash,
                            color = colors.secondary
                        )
                        CommitMetaItem(
                            icon = Icons.Default.Person,
                            text = commit.authorName,
                            color = colors.onSurfaceVariant
                        )
                        CommitMetaItem(
                            icon = Icons.Default.Schedule,
                            text = relativeTime,
                            color = colors.onSurfaceVariant
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
                    onRevert = onRevert,
                    onCherrypick = onCherrypick,
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
    color: Color
) {
    Row(
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
    onRevert: () -> Unit,
    onCherrypick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionChip(
                text = "Revert",
                color = AppColors.GitOrange,
                onClick = onRevert,
                modifier = Modifier.weight(1f)
            )
            ActionChip(
                text = "Cherry-pick",
                color = AppColors.GitPurple,
                onClick = onCherrypick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionChip(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
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
