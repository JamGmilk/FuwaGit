package jamgmilk.fuwagit.ui.screen.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.ui.util.formatRelativeTime
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

private val BRANCH_COLORS = listOf(
    Color(0xFF6750A4),
    Color(0xFF0061A4),
    Color(0xFF006A60),
    Color(0xFF606200),
    Color(0xFF984061),
    Color(0xFF705573),
    Color(0xFF006D3B),
    Color(0xFF8B5000),
    Color(0xFF415F91),
    Color(0xFFBA1A1A)
)

@Composable
internal fun CommitTimelineItem(
    commit: GitCommit,
    details: CommitDetailsState,
    isLast: Boolean,
    viewModel: HistoryViewModel,
    onViewDiff: ((DiffViewRequest) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val expanded = commit.hash in details.expandedCommitHashes
    val colors = MaterialTheme.colorScheme
    val timeFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val relativeTime = formatRelativeTime(commit.timestamp)

    val laneIndex = details.commitLanes[commit.hash] ?: (abs(commit.hash.hashCode()) % BRANCH_COLORS.size)
    val laneColor = BRANCH_COLORS[laneIndex % BRANCH_COLORS.size]

    Row(modifier = modifier.fillMaxWidth()) {
        TimelineIndicator(
            color = laneColor,
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
                    .clickable { viewModel.toggleCommitExpanded(commit.hash) }
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
                    onClick = { viewModel.toggleCommitExpanded(commit.hash) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) stringResource(R.string.action_collapse) else stringResource(R.string.action_expand),
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
                    details = details,
                    timeFmt = timeFmt,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onViewDiff = onViewDiff
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
                    if (isMerge) colors.tertiary else color,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isMerge) {
                Icon(
                    Icons.AutoMirrored.Filled.MergeType,
                    contentDescription = stringResource(R.string.history_merge_badge),
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
private fun MergeBadge() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .background(
                colors.tertiary.copy(alpha = 0.15f),
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
                tint = colors.tertiary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = stringResource(R.string.history_merge_badge),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.tertiary,
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
