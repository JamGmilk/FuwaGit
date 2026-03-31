package jamgmilk.fuwagit.ui.screen.branches

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras

@Composable
fun BranchesScreen(
    branchesViewModel: BranchesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by branchesViewModel.uiState.collectAsState()
    val local = uiState.localBranches
    val remote = uiState.remoteBranches
    val currentBranch = uiState.currentBranch?.name
    val colors = MaterialTheme.colorScheme
    val uiColors = FuwaGitThemeExtras.colors

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var branchToRename by remember { mutableStateOf<String?>(null) }

    ScreenTemplate(
        title = "Branches",
        modifier = modifier,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = { branchesViewModel.loadBranches() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.primaryContainer.copy(alpha = 0.3f), CircleShape)
                        .clip(CircleShape)

                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.primary, CircleShape)
                        .clip(CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Create Branch",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
            if (local.isEmpty() && remote.isEmpty()) {
                EmptyBranchesState()
            } else {
                BranchListContent(
                    localBranches = local,
                    remoteBranches = remote,
                    branchesViewModel = branchesViewModel,
                    showRenameDialog = showRenameDialog,
                    branchToRename = branchToRename,
                    onRenameRequest = { name ->
                        branchToRename = name
                        showRenameDialog = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateBranchDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                branchesViewModel.createBranch(name)
                showCreateDialog = false
            }
        )
    }

    if (showRenameDialog && branchToRename != null) {
        var newName by remember { mutableStateOf(branchToRename!!) }

        LaunchedEffect(branchToRename) {
            newName = branchToRename ?: ""
        }

        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                branchToRename = null
            },
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
                        text = "Rename branch \"$branchToRename\" to a new name.",
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
                    onClick = {
                        branchesViewModel.renameBranch(branchToRename!!, newName)
                        showRenameDialog = false
                        branchToRename = null
                    },
                    enabled = newName.isNotBlank() && newName != branchToRename,
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
                TextButton(onClick = {
                    showRenameDialog = false
                    branchToRename = null
                }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun EmptyBranchesState() {
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
                Icons.Outlined.AccountTree,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                "No branches found",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                "Initialize a repository to see branches",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BranchListContent(
    localBranches: List<GitBranch>,
    remoteBranches: List<GitBranch>,
    branchesViewModel: BranchesViewModel,
    showRenameDialog: Boolean,
    branchToRename: String?,
    onRenameRequest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiState by branchesViewModel.uiState.collectAsState()
    val currentBranch = uiState.currentBranch?.name

    LazyColumn(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader(
                title = "Local Branches",
                subtitle = "${localBranches.size} branches",
                icon = Icons.Outlined.AccountTree,
                color = Color(0xFF4CAF50)
            )
        }

        if (localBranches.isEmpty()) {
            item {
                EmptySectionMessage("No local branches")
            }
        } else {
            items(localBranches, key = { "local:${it.name}" }) { branch ->
                BranchItem(
                    branch = branch,
                    isCurrent = branch.name == currentBranch,
                    onCheckout = { branchesViewModel.checkoutBranch(branch.name) },
                    onMerge = { branchesViewModel.mergeBranch(branch.name) },
                    onRebase = { branchesViewModel.rebaseBranch(branch.name) },
                    onRename = {
                        onRenameRequest(branch.name)
                    },
                    onDelete = { branchesViewModel.deleteBranch(branch.name) }
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
        }

        item {
            SectionHeader(
                title = "Remote Branches",
                subtitle = "${remoteBranches.size} branches",
                icon = Icons.Default.Cloud,
                color = Color(0xFF2196F3)
            )
        }

        if (remoteBranches.isEmpty()) {
            item {
                EmptySectionMessage("No remote branches")
            }
        } else {
            items(remoteBranches, key = { "remote:${it.name}" }) { branch ->
                BranchItem(
                    branch = branch,
                    isCurrent = branch.name == currentBranch,
                    onCheckout = { branchesViewModel.checkoutBranch(branch.name) },
                    onMerge = { branchesViewModel.mergeBranch(branch.name) },
                    onRebase = { },
                    onRename = { },
                    onDelete = { },
                    isRemote = true
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySectionMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
    )
}

@Composable
private fun BranchItem(
    branch: GitBranch,
    isCurrent: Boolean,
    onCheckout: () -> Unit,
    onMerge: () -> Unit,
    onRebase: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    isRemote: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    val accentColor = if (isRemote) Color(0xFF2196F3) else Color(0xFF4CAF50)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isCurrent) accentColor.copy(alpha = 0.1f)
                    else colors.surface.copy(alpha = 0.4f)
                )
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BranchTypeIndicator(
                isRemote = isRemote,
                isCurrent = isCurrent,
                accentColor = accentColor
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = branch.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isRemote) "Remote" else if (isCurrent) "Current branch" else "Local",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .background(accentColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Actions",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!isCurrent) {
                    DropdownMenuItem(
                        text = { Text("Checkout") },
                        onClick = {
                            onCheckout()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                if (!isRemote) {
                    DropdownMenuItem(
                        text = { Text("Merge into current") },
                        onClick = {
                            onMerge()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rebase onto current") },
                        onClick = {
                            onRebase()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ImportExport, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
                if (!isCurrent && !isRemote) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            onRename()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BranchTypeIndicator(
    isRemote: Boolean,
    isCurrent: Boolean,
    accentColor: Color
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                if (isCurrent) accentColor
                else accentColor.copy(alpha = 0.15f),
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRemote) Icons.Default.Cloud else Icons.Default.Code,
            contentDescription = null,
            tint = if (isCurrent) Color.White else accentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CreateBranchDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var branchName by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Branch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("Branch Name") },
                    placeholder = { Text("feature/my-new-feature") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text(
                    text = "Branch will be created from the current HEAD",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(branchName) },
                enabled = branchName.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
