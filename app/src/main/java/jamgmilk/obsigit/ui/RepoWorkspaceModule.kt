package jamgmilk.obsigit.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun RepoWorkspaceModule(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val currentTab by viewModel.workspaceTab.collectAsState()
    val uiColors = ObsiGitThemeExtras.colors

    LaunchedEffect(Unit) {
        viewModel.refreshWorkspace()
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WorkspaceTabHeader(
                selected = currentTab,
                onSelect = viewModel::setWorkspaceTab
            )
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "workspace_tab_animation"
            ) { tab ->
                when (tab) {
                    WorkspaceTab.Status -> StatusModuleContent(viewModel)
                    WorkspaceTab.History -> HistoryModuleContent(viewModel)
                    WorkspaceTab.Branches -> BranchesModuleContent(viewModel)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceTabHeader(
    selected: WorkspaceTab,
    onSelect: (WorkspaceTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WorkspaceTab.entries.forEach { tab ->
            FilterChip(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                label = { Text(tab.name) }
            )
        }
    }
}

@Composable
private fun StatusModuleContent(viewModel: AppViewModel) {
    val files by viewModel.workspaceFiles.collectAsState()
    val staged = remember(files) { files.filter { it.isStaged } }
    val workspace = remember(files) { files.filter { !it.isStaged } }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusFileListSection(
            title = "Workspace",
            hint = "Swipe right to stage, left to discard",
            files = workspace,
            modifier = Modifier.weight(1f),
            viewModel = viewModel
        )
        StatusFileListSection(
            title = "Index (Staged)",
            hint = "Swipe left to unstage",
            files = staged,
            modifier = Modifier.weight(1f),
            viewModel = viewModel
        )
    }
}

@Composable
private fun StatusFileListSection(
    title: String,
    hint: String,
    files: List<GitFileStatus>,
    modifier: Modifier,
    viewModel: AppViewModel
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$title (${files.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No files", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files, key = { "${it.path}:${it.isStaged}" }) { file ->
                        WorkspaceFileStatusRow(file, viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceFileStatusRow(file: GitFileStatus, viewModel: AppViewModel) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!file.isStaged) {
                        viewModel.stageFile(file.path)
                    }
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (file.isStaged) {
                        viewModel.unstageFile(file.path)
                    } else {
                        viewModel.discardChanges(file.path)
                    }
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32)
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFC62828)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .padding(horizontal = 16.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                val icon = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Add
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Restore
                    else -> Icons.Default.Check
                }
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
    ) {
        ListItem(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)),
            headlineContent = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                Text(
                    text = file.path,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                val (icon, tint) = when (file.changeType) {
                    GitChangeType.Added -> Icons.Default.Add to Color(0xFF43A047)
                    GitChangeType.Modified -> Icons.Default.Edit to Color(0xFF1E88E5)
                    GitChangeType.Removed -> Icons.Default.RemoveCircle to Color(0xFFE53935)
                    GitChangeType.Untracked -> Icons.Default.Schedule to Color(0xFF78909C)
                    GitChangeType.Renamed -> Icons.Default.DoneAll to Color(0xFFFFA000)
                    GitChangeType.Conflicting -> Icons.Default.Restore to Color(0xFFD81B60)
                }
                Icon(icon, contentDescription = null, tint = tint)
            },
            trailingContent = {
                if (file.isStaged) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "staged", tint = Color(0xFF43A047))
                }
            }
        )
    }
}

@Composable
private fun HistoryModuleContent(viewModel: AppViewModel) {
    val history by viewModel.commitHistory.collectAsState()

    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No history available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(history, key = { it.hash }) { commit ->
                CommitPipelineRow(commit)
            }
        }
    }
}

@Composable
private fun CommitPipelineRow(commit: GitCommit) {
    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val branchColors = remember {
        listOf(
            Color(0xFF00BCD4),
            Color(0xFF4CAF50),
            Color(0xFFFF9800),
            Color(0xFFE91E63),
            Color(0xFF7C4DFF)
        )
    }
    val lane = abs(commit.hash.hashCode()) % branchColors.size
    val mergeLane = abs((commit.hash + "m").hashCode()) % branchColors.size
    val isMergeCommit = mergeLane != lane && commit.message.contains("merge", ignoreCase = true)
    val avatarText = commit.authorName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase(Locale.getDefault()) }
        .ifBlank { "?" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GitBranchGraph(
            lane = lane,
            mergeLane = mergeLane,
            merge = isMergeCommit,
            palette = branchColors,
            modifier = Modifier
                .width(56.dp)
                .height(68.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = commit.shortHash,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = commit.authorName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = commit.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = timeFmt.format(Date(commit.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GitBranchGraph(
    lane: Int,
    mergeLane: Int,
    merge: Boolean,
    palette: List<Color>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val laneCount = palette.size
        val centerY = size.height / 2f
        val laneX = { i: Int -> size.width * (i + 1) / (laneCount + 1) }

        repeat(laneCount) { i ->
            val color = palette[i].copy(alpha = if (i == lane || i == mergeLane) 0.45f else 0.22f)
            drawLine(
                color = color,
                start = Offset(laneX(i), 0f),
                end = Offset(laneX(i), size.height),
                strokeWidth = 2.dp.toPx()
            )
        }

        if (merge) {
            val path = Path().apply {
                moveTo(laneX(mergeLane), 0f)
                cubicTo(
                    laneX(mergeLane), centerY * 0.5f,
                    laneX(lane), centerY * 0.8f,
                    laneX(lane), centerY
                )
            }
            drawPath(path, color = palette[mergeLane], style = Stroke(width = 2.4.dp.toPx()))
        }

        drawCircle(
            color = palette[lane],
            radius = 6.dp.toPx(),
            center = Offset(laneX(lane), centerY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = 2.dp.toPx(),
            center = Offset(laneX(lane), centerY)
        )
    }
}

@Composable
private fun BranchesModuleContent(viewModel: AppViewModel) {
    val branches by viewModel.branches.collectAsState()
    val local = remember(branches) { branches.filter { !it.isRemote } }
    val remote = remember(branches) { branches.filter { it.isRemote } }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BranchListPanel(
            title = "Local Branches",
            branches = local,
            modifier = Modifier.weight(1f),
            viewModel = viewModel
        )
        BranchListPanel(
            title = "Remote Branches",
            branches = remote,
            modifier = Modifier.weight(1f),
            viewModel = viewModel
        )
    }
}

@Composable
private fun BranchListPanel(
    title: String,
    branches: List<GitBranch>,
    modifier: Modifier,
    viewModel: AppViewModel
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (branches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No branches", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    branches.forEach { branch ->
                        BranchItemRow(branch, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchItemRow(branch: GitBranch, viewModel: AppViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.checkoutBranch(branch.name) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = branch.name,
                    fontWeight = if (branch.isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Icon(
                    imageVector = if (branch.isRemote) Icons.Default.Cloud else Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = if (branch.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "branch actions")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Checkout") },
                            onClick = {
                                viewModel.checkoutBranch(branch.name)
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Merge") },
                            onClick = {
                                viewModel.mergeBranch(branch.name)
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rebase") },
                            onClick = {
                                viewModel.rebaseBranch(branch.name)
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                viewModel.deleteBranch(branch.name)
                                expanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            enabled = !branch.isCurrent
                        )
                    }
                }
            }
        )
    }
}
