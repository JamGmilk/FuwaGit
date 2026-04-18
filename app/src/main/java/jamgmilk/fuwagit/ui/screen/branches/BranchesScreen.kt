package jamgmilk.fuwagit.ui.screen.branches

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.ui.components.ConflictResolutionDialog
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.ui.components.DialogWithIcon
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.components.TipInDialog
import jamgmilk.fuwagit.ui.components.TwoStepConfirmDialog
import jamgmilk.fuwagit.ui.screen.tags.TagsContent
import jamgmilk.fuwagit.ui.screen.tags.TagsDialogs
import jamgmilk.fuwagit.ui.screen.tags.TagsViewModel
import jamgmilk.fuwagit.ui.theme.AppShapes
import kotlinx.coroutines.launch


@Composable
fun BranchesScreen(
    branchesViewModel: BranchesViewModel,
    modifier: Modifier = Modifier,
    tagsViewModel: TagsViewModel? = null,
    onCreateTag: ((String) -> Unit)? = null,
    onShowInHistory: ((String) -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by branchesViewModel.uiState.collectAsStateWithLifecycle()
    val local = uiState.localBranches
    val remote = uiState.remoteBranches
    val currentBranch = uiState.currentBranch?.name
    val colors = MaterialTheme.colorScheme

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var branchToRename by remember { mutableStateOf<String?>(null) }
    var showTagsView by remember { mutableStateOf(false) }

    ScreenTemplate(
        title = stringResource(R.string.screen_branches),
        modifier = modifier.fillMaxSize(),
        snackbarHostState = snackbarHostState,
        actions = {
            if (tagsViewModel != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !showTagsView,
                        onClick = { showTagsView = false },
                        label = { Text(stringResource(R.string.nav_branches)) }
                    )
                    FilterChip(
                        selected = showTagsView,
                        onClick = { showTagsView = true },
                        label = { Text(stringResource(R.string.nav_tags)) }
                    )
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShapes.medium),
            shape = AppShapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            if (showTagsView && tagsViewModel != null) {
                TagsContent(tagsViewModel = tagsViewModel)
            } else {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { branchesViewModel.loadBranches() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(colors.primaryContainer.copy(alpha = 0.3f), CircleShape)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.branches_refresh_description),
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
                                contentDescription = stringResource(R.string.branches_create_branch_description),
                                tint = colors.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

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
                            onCreateTag = onCreateTag,
                            onShowInHistory = onShowInHistory,
                            snackbarHostState = snackbarHostState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Tags 对话框（当在 BranchesScreen 中显示 Tags 时）
    if (showTagsView && tagsViewModel != null) {
        TagsDialogs(tagsViewModel = tagsViewModel)
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
        var newName by remember(branchToRename) {
            mutableStateOf(branchToRename ?: "")
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
                        .background(colors.error.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.branches_rename_branch),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.branches_rename_description, branchToRename!!),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.branches_new_branch_name_label)) },
                        placeholder = { Text(stringResource(R.string.branches_new_branch_name_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            focusedLabelColor = colors.primary
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
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_rename))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    branchToRename = null
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Event handling for snackbar
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        branchesViewModel.events.collect { event ->
            when (event) {
                is BranchUiEvent.DeleteSuccess -> {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vm_branch_deleted_success, event.branchName)) }
                }
                is BranchUiEvent.DeleteError -> {
                    val message = if (event.suggestion != null) {
                        "${event.reason}\n${event.suggestion}"
                    } else {
                        event.reason
                    }
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
                is BranchUiEvent.MergeSuccess -> {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vm_merge_success, event.branchName)) }
                }
                is BranchUiEvent.MergeConflict -> {
                    scope.launch { snackbarHostState.showSnackbar(event.hint) }
                }
                is BranchUiEvent.MergeError -> {
                    scope.launch { snackbarHostState.showSnackbar(event.suggestion) }
                }
                is BranchUiEvent.RebaseSuccess -> {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vm_rebase_success, event.branchName)) }
                }
                is BranchUiEvent.RebaseConflict -> {
                    scope.launch { snackbarHostState.showSnackbar(event.hint) }
                }
                is BranchUiEvent.RebaseError -> {
                    scope.launch { snackbarHostState.showSnackbar(event.suggestion) }
                }
                is BranchUiEvent.CheckoutSuccess -> {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vm_checkout_success, event.branchName)) }
                }
                is BranchUiEvent.CreateBranchSuccess -> {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vm_branch_created_success, event.branchName)) }
                }
                is BranchUiEvent.RenameSuccess -> {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.vm_branch_renamed_success, event.newName)) }
                }
                is BranchUiEvent.ConflictResolved -> {
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                }
                is BranchUiEvent.AbortRebaseSuccess -> {
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                }
                is BranchUiEvent.ContinueRebaseSuccess -> {
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                }
                is BranchUiEvent.Error -> {
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                }
            }
        }
    }

    val pendingOperation = uiState.pendingOperation
    val conflictResult = uiState.conflictResult
    val pendingTarget = uiState.pendingOperationTarget
    if (pendingOperation != null && pendingTarget != null) {
        when (pendingOperation) {
            DangerousOperationType.DELETE_BRANCH -> {
                TwoStepConfirmDialog(
                    operationType = DangerousOperationType.DELETE_BRANCH,
                    targetName = pendingTarget,
                    description = stringResource(R.string.branches_delete_description),
                    warningMessage = stringResource(R.string.branches_delete_warning),
                    confirmText = stringResource(R.string.branches_delete_confirm),
                    onConfirm = { branchesViewModel.confirmDeleteBranch(force = false) },
                    onDismiss = { branchesViewModel.cancelPendingOperation() }
                )
            }
            DangerousOperationType.MERGE -> {
                TwoStepConfirmDialog(
                    operationType = DangerousOperationType.MERGE,
                    targetName = pendingTarget,
                    description = stringResource(R.string.branches_merge_description),
                    warningMessage = stringResource(R.string.branches_merge_warning),
                    confirmText = stringResource(R.string.branches_merge_confirm),
                    onConfirm = { branchesViewModel.confirmMergeBranch() },
                    onDismiss = { branchesViewModel.cancelPendingOperation() }
                )
            }
            DangerousOperationType.REBASE -> {
                TwoStepConfirmDialog(
                    operationType = DangerousOperationType.REBASE,
                    targetName = pendingTarget,
                    description = stringResource(R.string.branches_rebase_description),
                    warningMessage = stringResource(R.string.branches_rebase_warning),
                    confirmText = stringResource(R.string.branches_rebase_confirm),
                    onConfirm = { branchesViewModel.confirmRebaseBranch() },
                    onDismiss = { branchesViewModel.cancelPendingOperation() }
                )
            }
            else -> {}
        }
    }

    // ConflictResolutionDialog
    if (conflictResult != null && uiState.isResolvingConflict) {
        val isRebase = conflictResult.operationType == "REBASE"
        val allResolved = conflictResult.allResolved

        ConflictResolutionDialog(
            conflictResult = conflictResult,
            onResolveConflict = { filePath ->
                branchesViewModel.markConflictResolved(filePath)
            },
            onFinish = {
                if (isRebase && allResolved) {
                    // 对于 Rebase，调用 continueRebase
                    branchesViewModel.continueRebase()
                } else {
                    branchesViewModel.finishConflictResolution()
                }
            },
            onAbort = {
                if (conflictResult.operationType == "REBASE") {
                    branchesViewModel.abortRebase()
                } else {
                    branchesViewModel.cancelConflictResolution()
                }
            },
            onDismiss = { branchesViewModel.cancelConflictResolution() }
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
                stringResource(R.string.branches_no_branches_found),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                stringResource(R.string.branches_init_repo_message),
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
    onCreateTag: ((String) -> Unit)?,
    onShowInHistory: ((String) -> Unit)?,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiState by branchesViewModel.uiState.collectAsStateWithLifecycle()
    val currentBranch = uiState.currentBranch?.name

    LazyColumn(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
    ) {
        item(contentType = "header") {
            SectionHeader(
                title = stringResource(R.string.branches_local_branches),
                subtitle = stringResource(R.string.branches_count_format, localBranches.size),
                icon = Icons.Outlined.AccountTree,
                color = colors.primary
            )
        }

        if (localBranches.isEmpty()) {
            item {
                EmptySectionMessage(stringResource(R.string.branches_no_local_branches))
            }
        } else {
            items(
                items = localBranches,
                key = { "local:${it.name}" },
                contentType = { "branch_item" }
            ) { branch ->
                BranchItem(
                    branch = branch,
                    isCurrent = branch.name == currentBranch,
                    branchesViewModel = branchesViewModel,
                    onCheckout = { branchesViewModel.checkoutBranch(branch.name) },
                    onMerge = { branchesViewModel.requestMergeBranch(branch.name) },
                    onRebase = { branchesViewModel.requestRebaseBranch(branch.name) },
                    onRename = {
                        onRenameRequest(branch.name)
                    },
                    onDelete = { branchesViewModel.requestDeleteBranch(branch.name) },
                    onCreateTag = onCreateTag,
                    onShowInHistory = onShowInHistory,
                    snackbarHostState = snackbarHostState
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
                title = stringResource(R.string.branches_remote_branches),
                subtitle = stringResource(R.string.branches_count_format, remoteBranches.size),
                icon = Icons.Default.Cloud,
                color = colors.secondary
            )
        }

        if (remoteBranches.isEmpty()) {
            item {
                EmptySectionMessage(stringResource(R.string.branches_no_remote_branches))
            }
        } else {
            items(
                items = remoteBranches,
                key = { "remote:${it.name}" },
                contentType = { "remote_branch" }
            ) { branch ->
                BranchItem(
                    branch = branch,
                    isCurrent = branch.name == currentBranch,
                    branchesViewModel = branchesViewModel,
                    onCheckout = { branchesViewModel.checkoutBranch(branch.name) },
                    onMerge = null,
                    onRebase = null,
                    onRename = null,
                    onDelete = null,
                    onCreateTag = null,
                    onShowInHistory = null,
                    snackbarHostState = snackbarHostState,
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
    branchesViewModel: BranchesViewModel,
    onCheckout: () -> Unit,
    onMerge: (() -> Unit)?,
    onRebase: (() -> Unit)?,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onCreateTag: ((String) -> Unit)?,
    onShowInHistory: ((String) -> Unit)?,
    snackbarHostState: SnackbarHostState,
    isRemote: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val strings = BranchItemStrings(
        mergeOnlyLocal = stringResource(R.string.branches_merge_only_local),
        rebaseOnlyLocal = stringResource(R.string.branches_rebase_only_local),
        deleteOnlyLocal = stringResource(R.string.branches_delete_only_local),
        renameOnlyLocal = stringResource(R.string.branches_rename_only_local),
        nameCopied = stringResource(R.string.branches_name_copied),
        branchNameClipboard = stringResource(R.string.branches_branch_name_clipboard),
        remoteLabel = stringResource(R.string.branches_remote_label),
        currentBranchSubtitle = stringResource(R.string.branches_current_branch_subtitle),
        localSubtitle = stringResource(R.string.branches_local_subtitle),
        activeBadge = stringResource(R.string.branches_active_badge),
        actionsDesc = stringResource(R.string.status_file_actions)
    )

    val accentColor = if (isRemote) colors.secondary else colors.primary

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
                    text = if (isRemote) strings.remoteLabel else if (isCurrent) strings.currentBranchSubtitle else strings.localSubtitle,
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
                        text = strings.activeBadge,
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
                    contentDescription = strings.actionsDesc,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (isCurrent) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.branches_rename)) },
                        onClick = {
                            onRename?.invoke()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.branches_create_tag)) },
                        onClick = {
                            onCreateTag?.invoke(branch.name)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.branches_copy_name)) },
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(strings.branchNameClipboard, branch.name))
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.branches_name_copied)) }
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isRemote) stringResource(R.string.branches_checkout_remote)
                                else stringResource(R.string.branches_checkout)
                            )
                        },
                        onClick = {
                            onCheckout()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )

                    if (!isRemote) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.branches_merge_into_current)) },
                            onClick = {
                                onMerge?.invoke() ?: run {
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.branches_merge_only_local)) }
                                }
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.branches_rebase_onto_current)) },
                            onClick = {
                                onRebase?.invoke() ?: run {
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.branches_rebase_only_local)) }
                                }
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ImportExport, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.branches_rename)) },
                            onClick = {
                                onRename?.invoke() ?: run {
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.branches_rename_only_local)) }
                                }
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.branches_delete_branch)) },
                            onClick = {
                                onDelete?.invoke() ?: run {
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.branches_delete_only_local)) }
                                }
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
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
            imageVector = if (isRemote) Icons.Default.Cloud else Icons.Outlined.AccountTree,
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

    DialogWithIcon(
        onDismiss = onDismiss,
        icon = Icons.Default.Add,
        title = stringResource(R.string.branches_create_new_branch),
        subtitle = stringResource(R.string.branches_create_from_head),
        confirmButton = {
            Button(
                onClick = { onCreate(branchName) },
                enabled = branchName.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        OutlinedTextField(
            value = branchName,
            onValueChange = { branchName = it },
            label = { Text(stringResource(R.string.branches_branch_name_label)) },
            placeholder = { Text(stringResource(R.string.branches_branch_name_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        TipInDialog(
            icon = Icons.Default.Info,
            text = stringResource(R.string.branches_create_tip)
        )
    }
}

// ✅ 数据类持有字符串，避免每次重组都调用 stringResource
private data class BranchItemStrings(
    val mergeOnlyLocal: String,
    val rebaseOnlyLocal: String,
    val deleteOnlyLocal: String,
    val renameOnlyLocal: String,
    val nameCopied: String,
    val branchNameClipboard: String,
    val remoteLabel: String,
    val currentBranchSubtitle: String,
    val localSubtitle: String,
    val activeBadge: String,
    val actionsDesc: String
)
