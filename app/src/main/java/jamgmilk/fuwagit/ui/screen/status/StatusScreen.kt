package jamgmilk.fuwagit.ui.screen.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.ui.components.OperationResultDialog
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.components.TwoStepConfirmDialog
import jamgmilk.fuwagit.ui.util.ViewModelMessagesMapper

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
    modifier: Modifier = Modifier,
    onViewDiff: ((String, Boolean) -> Unit)? = null
) {
    val uiState by statusViewModel.uiState.collectAsStateWithLifecycle()
    val files = uiState.workspaceFiles
    val staged = remember(files) { files.filter { it.isStaged } }
    val workspace = remember(files) { files.filter { !it.isStaged } }
    var commitMessage by remember { mutableStateOf("") }

    val isRepo = uiState.isGitRepo
    val terminalLogs = uiState.terminalOutput
    val currentBranch = uiState.currentBranch
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(uiState.repoPath) {
        if (uiState.repoPath != null) {
            statusViewModel.refreshAll()
        }
    }

    val statusStats by remember(files, staged, workspace) {
        derivedStateOf {
            var untrackedCount = 0
            var modifiedCount = 0
            var addedCount = 0
            var removedCount = 0
            for (file in files) {
                when (file.changeType) {
                    GitChangeType.Untracked -> untrackedCount++
                    GitChangeType.Modified -> modifiedCount++
                    GitChangeType.Added -> addedCount++
                    GitChangeType.Removed -> removedCount++
                    else -> {}
                }
            }
            StatusStats(
                totalChanges = files.size,
                staged = staged.size,
                unstaged = workspace.size,
                untracked = untrackedCount,
                modified = modifiedCount,
                added = addedCount,
                removed = removedCount
            )
        }
    }

    ScreenTemplate(
        title = stringResource(R.string.screen_status),
        modifier = modifier,
        actions = {
            RefreshIconButton(
                onClick = { statusViewModel.refreshWorkspace() },
                modifier = Modifier.size(36.dp)
            )
        }
    ) {
        RepositoryStatusCard(
            isRepo = uiState.isGitRepo,
            repoName = uiState.repoName,
            targetPath = uiState.repoPath,
            currentBranch = currentBranch,
            isLoading = uiState.isCheckingRepo,
            error = uiState.error,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(
            visible = !isRepo && uiState.repoPath != null && !uiState.isCheckingRepo,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            InitRepositoryCard(
                onInit = { statusViewModel.initRepo() },
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = isRepo && !uiState.isCheckingRepo,
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
                    onFetch = { statusViewModel.fetchRepo() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        AnimatedVisibility(
            visible = isRepo && !uiState.isCheckingRepo,
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
                    title = stringResource(R.string.status_workspace),
                    subtitle = stringResource(R.string.status_workspace_subtitle),
                    files = workspace,
                    modifier = Modifier.weight(1f),
                    accentColor = colors.primary,
                    onFileAction = { file ->
                        statusViewModel.stageFile(file.path)
                    },
                    onDiscard = { file ->
                        statusViewModel.requestDiscardChanges(file.path)
                    },
                    onViewDiff = { file ->
                        onViewDiff?.invoke(file.path, false)
                    },
                    emptyMessage = stringResource(R.string.status_working_directory_clean)
                )

                FileSectionCard(
                    title = stringResource(R.string.status_staged),
                    subtitle = stringResource(R.string.status_staged_subtitle),
                    files = staged,
                    modifier = Modifier.weight(1f),
                    accentColor = colors.tertiary,
                    onFileAction = { file -> statusViewModel.unstageFile(file.path) },
                    onViewDiff = { file ->
                        onViewDiff?.invoke(file.path, true)
                    },
                    emptyMessage = stringResource(R.string.status_nothing_to_commit)
                )
            }
        }

        AnimatedVisibility(
            visible = isRepo && !uiState.isCheckingRepo,
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

        AnimatedVisibility(
            visible = isRepo && !uiState.isCheckingRepo,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            TerminalLogsCard(
                logs = terminalLogs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
    }

    // Dangerous operation double-confirmation dialog
    val pendingOperation = uiState.pendingOperation
    val pendingTarget = uiState.pendingOperationTarget
    if (pendingOperation != null && pendingTarget != null) {
        when (pendingOperation) {
            DangerousOperationType.DISCARD_CHANGES -> {
                TwoStepConfirmDialog(
                    operationType = DangerousOperationType.DISCARD_CHANGES,
                    targetName = pendingTarget,
                    description = stringResource(R.string.status_discard_description),
                    warningMessage = stringResource(R.string.status_discard_warning),
                    confirmText = stringResource(R.string.status_discard_confirm),
                    onConfirm = { statusViewModel.confirmDiscardChanges() },
                    onDismiss = { statusViewModel.cancelPendingOperation() }
                )
            }
            else -> {}
        }
    }

    // 冲突解决对话框
    val conflictResult = uiState.conflictResult
    if (conflictResult != null && uiState.isResolvingConflict) {
        val isRebase = conflictResult.operationType == "REBASE"
        val allResolved = conflictResult.allResolved

        jamgmilk.fuwagit.ui.components.ConflictResolutionDialog(
            conflictResult = conflictResult,
            onResolveConflict = { filePath ->
                statusViewModel.markConflictResolved(filePath)
            },
            onFinish = {
                if (isRebase && allResolved) {
                    statusViewModel.continueRebase()
                } else {
                    statusViewModel.finishConflictResolution()
                }
            },
            onAbort = {
                if (conflictResult.operationType == "REBASE") {
                    statusViewModel.abortRebase()
                } else {
                    statusViewModel.cancelConflictResolution()
                }
            },
            onDismiss = { statusViewModel.cancelConflictResolution() }
        )
    }

    // Operation result feedback dialog
    val operationResult = uiState.operationResult
    if (operationResult != null) {
        val operationType = pendingOperation ?: DangerousOperationType.DISCARD_CHANGES
        OperationResultDialog(
            result = operationResult,
            operationType = operationType,
            onDismiss = { statusViewModel.clearOperationResult() }
        )
    }
}

@Composable
private fun RefreshIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotationCount by remember { mutableStateOf(0) }
    val targetRotation = rotationCount * 360f
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1500)
    )

    FilledTonalIconButton(
        onClick = {
            rotationCount += 3
            onClick()
        },
        modifier = modifier
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = stringResource(R.string.status_refresh_description),
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = animatedRotation }
        )
    }
}
