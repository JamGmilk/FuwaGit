package jamgmilk.obsigit.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.unit.dp
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkspaceScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    val currentTab by viewModel.workspaceTab.collectAsState()
    val targetPath by viewModel.targetPath.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshWorkspace()
    }

    BackHandler {
        viewModel.switchPage(AppPage.Repo)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.switchPage(AppPage.Repo) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
                }
                Column {
                    Text(
                        text = "Workspace",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary
                    )
                    Text(
                        text = targetPath?.let { AppRepoOps.shortDisplayPath(java.io.File(it)) } ?: "No repo",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = uiColors.navBarContainer,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == WorkspaceTab.Status,
                    onClick = { viewModel.setWorkspaceTab(WorkspaceTab.Status) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Status") },
                    label = { Text("Status") }
                )
                NavigationBarItem(
                    selected = currentTab == WorkspaceTab.History,
                    onClick = { viewModel.setWorkspaceTab(WorkspaceTab.History) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = currentTab == WorkspaceTab.Branches,
                    onClick = { viewModel.setWorkspaceTab(WorkspaceTab.Branches) },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = "Branches") },
                    label = { Text("Branches") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "workspace_tab_transition"
            ) { tab ->
                when (tab) {
                    WorkspaceTab.Status -> StatusModule(viewModel)
                    WorkspaceTab.History -> HistoryModule(viewModel)
                    WorkspaceTab.Branches -> BranchesModule(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusModule(viewModel: AppViewModel) {
    val files by viewModel.workspaceFiles.collectAsState()
    var commitMessage by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ElevatedCard(
            modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No changes in workspace", color = colors.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files, key = { it.path + it.isStaged }) { file ->
                        FileStatusRow(file, viewModel)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

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
                enabled = commitMessage.isNotBlank() && files.any { it.isStaged }
            ) {
                Text("Commit")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileStatusRow(file: GitFileStatus, viewModel: AppViewModel) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!file.isStaged) viewModel.stageFile(file.path)
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
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50) // Green for stage
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336) // Red for unstage/discard
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Add else Icons.Default.Restore,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            ListItem(
                headlineContent = { Text(file.name, fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text(file.path, style = MaterialTheme.typography.bodySmall) },
                leadingContent = {
                    val (icon, tint) = when (file.changeType) {
                        GitChangeType.Added -> Icons.Default.Add to Color(0xFF4CAF50)
                        GitChangeType.Modified -> Icons.Default.CheckCircle to Color(0xFF2196F3)
                        GitChangeType.Removed -> Icons.Default.RemoveCircle to Color(0xFFF44336)
                        GitChangeType.Untracked -> Icons.Default.MoreVert to Color.Gray
                        else -> Icons.Default.Check to Color.Gray
                    }
                    Icon(icon, contentDescription = null, tint = tint)
                },
                trailingContent = {
                    if (file.isStaged) {
                        Icon(Icons.Default.Check, contentDescription = "Staged", tint = Color(0xFF4CAF50))
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryModule(viewModel: AppViewModel) {
    val history by viewModel.commitHistory.collectAsState()
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors

    ElevatedCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp)
    ) {
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history available", color = colors.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(history) { commit ->
                    CommitRow(commit)
                }
            }
        }
    }
}

@Composable
fun CommitRow(commit: GitCommit) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dotColor = remember { 
        val colors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFFE91E63))
        colors[commit.hash.take(1).toIntOrNull(16)?.rem(colors.size) ?: 0]
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple Branch Graph
        Box(modifier = Modifier.width(32.dp).height(64.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = dotColor,
                    radius = 6.dp.toPx(),
                    center = Offset(size.width / 2, size.height / 2)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
        }
        
        Spacer(Modifier.width(8.dp))
        
        // Avatar Placeholder
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = commit.authorName.take(1).uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = commit.shortHash,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = commit.authorName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = commit.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = dateFormat.format(Date(commit.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BranchesModule(viewModel: AppViewModel) {
    val branches by viewModel.branches.collectAsState()
    val localBranches = branches.filter { !it.isRemote }
    val remoteBranches = branches.filter { it.isRemote }
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, uiColors.cardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = uiColors.cardContainer),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                item {
                    Text(
                        "Local Branches",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary
                    )
                }
                items(localBranches) { branch ->
                    BranchRow(branch, viewModel)
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Remote Branches",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primary
                    )
                }
                items(remoteBranches) { branch ->
                    BranchRow(branch, viewModel)
                }
            }
        }
    }
}

@Composable
fun BranchRow(branch: GitBranch, viewModel: AppViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    
    ListItem(
        headlineContent = { 
            Text(
                branch.name, 
                fontWeight = if (branch.isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (branch.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ) 
        },
        leadingContent = { 
            Icon(
                if (branch.isRemote) Icons.Default.Cloud else Icons.Default.AccountTree, 
                contentDescription = null,
                tint = if (branch.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        trailingContent = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Actions")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Checkout") },
                    onClick = {
                        viewModel.checkoutBranch(branch.name)
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Merge") },
                    onClick = {
                        viewModel.mergeBranch(branch.name)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Rebase") },
                    onClick = {
                        viewModel.rebaseBranch(branch.name)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        viewModel.deleteBranch(branch.name)
                        showMenu = false
                    },
                    enabled = !branch.isCurrent
                )
            }
        },
        modifier = Modifier.clickable { viewModel.checkoutBranch(branch.name) }
    )
}
