package jamgmilk.obsigit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusModule(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val files by viewModel.workspaceFiles.collectAsState()
    val staged = remember(files) { files.filter { it.isStaged } }
    val workspace = remember(files) { files.filter { !it.isStaged } }
    var commitMessage by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors

    val isRepo by viewModel.isGitRepo.collectAsState()
    val statusText by viewModel.gitStatusText.collectAsState()
    val terminalLogs by viewModel.terminalOutput.collectAsState()
    val targetPath by viewModel.targetPath.collectAsState()

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.refreshWorkspace()
        viewModel.checkRepoStatus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(280)) + slideInVertically(
                initialOffsetY = { -it / 5 },
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            )
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isRepo) uiColors.cardContainer else colors.errorContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRepo) "Repository Active" else "Repository Not Found",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isRepo) {
                Button(
                    onClick = { viewModel.initRepo() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = targetPath != null
                ) { Text("git init") }
            } else {
                PinkActionButton("status", onClick = { viewModel.showStatusInTerminal() })
                PinkActionButton("stage all", onClick = { viewModel.stageAll() })
                PinkActionButton("unstage", onClick = { viewModel.unstageAll() }, outlined = true)
                PinkActionButton("pull", onClick = { viewModel.pullRepo() })
                PinkActionButton("push", onClick = { viewModel.pushRepo() })
            }
        }

        FileStatusOverview(
            workspaceFiles = workspace,
            stagedFiles = staged,
            onStageAll = { viewModel.stageAll() },
            onUnstageAll = { viewModel.unstageAll() }
        )

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
                accentColor = Color(0xFFE91E63),
                onFileAction = { file ->
                    when {
                        file.changeType == GitChangeType.Untracked -> viewModel.stageFile(file.path)
                        file.changeType == GitChangeType.Removed -> viewModel.discardChanges(file.path)
                        else -> viewModel.stageFile(file.path)
                    }
                },
                onDiscard = { file -> viewModel.discardChanges(file.path) },
                emptyMessage = "Working directory clean"
            )
            
            FileSectionCard(
                title = "Index",
                subtitle = "Staged for commit",
                files = staged,
                modifier = Modifier.weight(1f),
                accentColor = Color(0xFF4CAF50),
                onFileAction = { file -> viewModel.unstageFile(file.path) },
                showStagedIndicator = true,
                emptyMessage = "Nothing to commit"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(uiColors.cardContainer)
                .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = commitMessage,
                onValueChange = { commitMessage = it },
                label = { Text("Commit Message") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Button(
                onClick = {
                    if (commitMessage.isNotBlank()) {
                        viewModel.commitChanges(commitMessage)
                        commitMessage = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = commitMessage.isNotBlank() && staged.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Commit")
            }
        }

        Column(modifier = Modifier.height(200.dp).fillMaxWidth()) {
            Text(
                "Terminal Logs",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onBackground,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            val listState = rememberLazyListState()
            LaunchedEffect(terminalLogs.size) {
                if (terminalLogs.isNotEmpty()) {
                    listState.animateScrollToItem(terminalLogs.size - 1)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(uiColors.terminalBackground)
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(uiColors.terminalBackground),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(terminalLogs) { log ->
                        Text(
                            text = log,
                            color = uiColors.terminalText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileStatusOverview(
    workspaceFiles: List<GitFileStatus>,
    stagedFiles: List<GitFileStatus>,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    
    val totalFiles = workspaceFiles.size + stagedFiles.size
    val workspaceStats = remember(workspaceFiles) { 
        ChangeTypeStats.fromFiles(workspaceFiles) 
    }
    val stagedStats = remember(stagedFiles) { 
        ChangeTypeStats.fromFiles(stagedFiles) 
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Changes Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$totalFiles files changed",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (totalFiles > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFE91E63), CircleShape)
                            )
                            Text(
                                text = "Workspace",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "(${workspaceFiles.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        
                        ChangeTypeBarChart(
                            stats = workspaceStats,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        
                        ChangeTypeLegend(
                            stats = workspaceStats,
                            showZero = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                            Text(
                                text = "Staged",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "(${stagedFiles.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        
                        ChangeTypeBarChart(
                            stats = stagedStats,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        
                        ChangeTypeLegend(
                            stats = stagedStats,
                            showZero = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (workspaceFiles.isNotEmpty()) {
                        Button(
                            onClick = onStageAll,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stage All", fontSize = 13.sp)
                        }
                    }
                    if (stagedFiles.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onUnstageAll,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.primary)
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Unstage All", fontSize = 13.sp)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "No changes detected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

data class ChangeTypeStats(
    val added: Int = 0,
    val modified: Int = 0,
    val removed: Int = 0,
    val untracked: Int = 0,
    val renamed: Int = 0,
    val conflicting: Int = 0
) {
    val total: Int get() = added + modified + removed + untracked + renamed + conflicting
    
    companion object {
        fun fromFiles(files: List<GitFileStatus>): ChangeTypeStats {
            return ChangeTypeStats(
                added = files.count { it.changeType == GitChangeType.Added },
                modified = files.count { it.changeType == GitChangeType.Modified },
                removed = files.count { it.changeType == GitChangeType.Removed },
                untracked = files.count { it.changeType == GitChangeType.Untracked },
                renamed = files.count { it.changeType == GitChangeType.Renamed },
                conflicting = files.count { it.changeType == GitChangeType.Conflicting }
            )
        }
    }
}

@Composable
private fun ChangeTypeBarChart(
    stats: ChangeTypeStats,
    modifier: Modifier = Modifier
) {
    val total = stats.total.toFloat().coerceAtLeast(1f)
    
    val segments = listOf(
        stats.added to Color(0xFF43A047),
        stats.modified to Color(0xFF1E88E5),
        stats.removed to Color(0xFFE53935),
        stats.untracked to Color(0xFF78909C),
        stats.renamed to Color(0xFFFFA000),
        stats.conflicting to Color(0xFFD81B60)
    ).filter { it.first > 0 }
    
    if (segments.isEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        )
        return
    }
    
    Canvas(modifier = modifier) {
        var currentX = 0f
        val cornerRadius = 4.dp.toPx()
        
        segments.forEach { (count, color) ->
            val width = (count / total) * size.width
            if (width > 0) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(currentX, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
                currentX += width
            }
        }
    }
}

@Composable
private fun ChangeTypeLegend(
    stats: ChangeTypeStats,
    showZero: Boolean = true,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        stats.added to "A" to Color(0xFF43A047),
        stats.modified to "M" to Color(0xFF1E88E5),
        stats.removed to "D" to Color(0xFFE53935),
        stats.untracked to "?" to Color(0xFF78909C),
        stats.renamed to "R" to Color(0xFFFFA000),
        stats.conflicting to "!" to Color(0xFFD81B60)
    ).filter { showZero || it.first.first > 0 }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { (data, color) ->
            val (count, label) = data
            if (count > 0 || showZero) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileSectionCard(
    title: String,
    subtitle: String,
    files: List<GitFileStatus>,
    modifier: Modifier = Modifier,
    accentColor: Color,
    onFileAction: (GitFileStatus) -> Unit,
    onDiscard: ((GitFileStatus) -> Unit)? = null,
    showStagedIndicator: Boolean = false,
    emptyMessage: String
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    
    ElevatedCard(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    Box(
                        modifier = Modifier
                            .background(accentColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = files.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (showStagedIndicator) Icons.Default.CheckCircle else Icons.Default.Folder,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
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
                            onAction = { onFileAction(file) },
                            onDiscard = onDiscard?.let { { it(file) } },
                            showStagedIndicator = showStagedIndicator
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileStatusItem(
    file: GitFileStatus,
    accentColor: Color,
    onAction: () -> Unit,
    onDiscard: (() -> Unit)?,
    showStagedIndicator: Boolean
) {
    val colors = MaterialTheme.colorScheme
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onAction()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDiscard?.invoke()
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
                SwipeToDismissBoxValue.StartToEnd -> accentColor
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .padding(horizontal = 12.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                val icon = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> if (showStagedIndicator) Icons.Rounded.Remove else Icons.Rounded.Add
                    SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Delete
                    else -> Icons.Default.Check
                }
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
    ) {
        val (changeIcon, changeColor, changeLabel) = when (file.changeType) {
            GitChangeType.Added -> Triple(Icons.Rounded.Add, Color(0xFF43A047), "A")
            GitChangeType.Modified -> Triple(Icons.Rounded.Edit, Color(0xFF1E88E5), "M")
            GitChangeType.Removed -> Triple(Icons.Rounded.Delete, Color(0xFFE53935), "D")
            GitChangeType.Untracked -> Triple(Icons.Default.Schedule, Color(0xFF78909C), "?")
            GitChangeType.Renamed -> Triple(Icons.Default.History, Color(0xFFFFA000), "R")
            GitChangeType.Conflicting -> Triple(Icons.Default.Error, Color(0xFFD81B60), "!")
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(changeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = changeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = changeColor,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
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
            
            if (showStagedIndicator) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PinkActionButton(text: String, onClick: () -> Unit, outlined: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            border = BorderStroke(1.5.dp, colors.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.animateContentSize()
        ) {
            Text(text, color = colors.primary, fontWeight = FontWeight.Bold)
        }
    } else {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.animateContentSize()
        ) {
            Text(text, color = colors.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
