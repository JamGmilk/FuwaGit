package jamgmilk.fuwagit.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitCommitFileChange
import jamgmilk.fuwagit.domain.model.git.GitResetMode
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun CommitDetails(
    commit: GitCommit,
    details: CommitDetailsState,
    timeFmt: SimpleDateFormat,
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier,
    onViewDiff: ((DiffViewRequest) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val commitDetail = details.commitDetails[commit.hash]
    val isLoadingDetail = commit.hash in details.loadingCommitDetails
    val hasDetailError = commit.hash in details.detailLoadErrors

    LaunchedEffect(commit.hash) {
        viewModel.loadCommitDetailIfNeeded(commit)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DetailRow(
            icon = Icons.Default.Code,
            label = stringResource(R.string.credential_info_hash),
            value = commit.hash,
            isMonospace = true
        )

        DetailRow(
            icon = Icons.Default.Person,
            label = stringResource(R.string.credential_info_author),
            value = commit.authorName
        )

        DetailRow(
            icon = Icons.Default.Email,
            label = stringResource(R.string.credential_info_email),
            value = commit.authorEmail,
            isMonospace = true
        )

        DetailRow(
            icon = Icons.Default.Schedule,
            label = stringResource(R.string.credential_info_date),
            value = timeFmt.format(Date(commit.timestamp))
        )

        if (commit.isMerge) {
            DetailRow(
                icon = Icons.Default.AccountTree,
                label = stringResource(R.string.history_parents_label),
                value = commit.parentHashes.joinToString(", ") { it.take(7) }
            )
        }

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
        } else if (commitDetail != null || hasDetailError) {
            if (commitDetail != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip(
                        value = "${commitDetail.totalFiles}",
                        label = stringResource(R.string.history_files_label),
                        color = colors.primary
                    )
                    StatChip(
                        value = "+${commitDetail.totalAdditions}",
                        label = stringResource(R.string.history_additions_label),
                        color = colors.primary
                    )
                    StatChip(
                        value = "-${commitDetail.totalDeletions}",
                        label = stringResource(R.string.history_deletions_label),
                        color = colors.error
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.history_changed_files),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                if (commitDetail.fileChanges.isEmpty()) {
                    Text(
                        text = stringResource(R.string.history_no_file_changes),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        commitDetail.fileChanges.forEach { fileChange ->
                            FileChangeItem(
                                fileChange = fileChange,
                                commitHash = commit.hash,
                                onViewDiff = onViewDiff
                            )
                        }
                    }
                }
            }

            if (hasDetailError) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.history_detail_load_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.retryCommitDetail(commit) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }

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
                Text(stringResource(R.string.history_reset_to_commit), style = MaterialTheme.typography.labelLarge)
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
                    text = { Text(stringResource(R.string.dialog_reset_soft_label), color = colors.primary) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showResetMenu = false
                        viewModel.requestReset(commit, GitResetMode.SOFT)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dialog_reset_mixed_label), color = colors.tertiary) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Replay,
                            contentDescription = null,
                            tint = colors.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showResetMenu = false
                        viewModel.requestReset(commit, GitResetMode.MIXED)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dialog_reset_hard_label), color = colors.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = colors.error,
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
    icon: ImageVector,
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
    fileChange: GitCommitFileChange,
    commitHash: String,
    onViewDiff: ((DiffViewRequest) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = colors.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                if (onViewDiff != null) {
                    val parentHash = "$commitHash^"
                    onViewDiff(DiffViewRequest(fileChange.path, parentHash, commitHash))
                }
            })
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
                    Icon(
                        imageVector = when (fileChange.changeType) {
                            GitChangeType.Added -> Icons.Default.Add
                            GitChangeType.Removed -> Icons.Default.Delete
                            GitChangeType.Renamed -> Icons.Default.Refresh
                            else -> Icons.Default.Edit
                        },
                        contentDescription = null,
                        tint = when (fileChange.changeType) {
                            GitChangeType.Added -> colors.primary
                            GitChangeType.Removed -> colors.error
                            GitChangeType.Renamed -> colors.secondary
                            else -> colors.tertiary
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (fileChange.additions > 0) {
                    Text(
                        text = "+${fileChange.additions}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }
                if (fileChange.deletions > 0) {
                    Text(
                        text = "-${fileChange.deletions}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.error
                    )
                }
            }
        }
    }
}
