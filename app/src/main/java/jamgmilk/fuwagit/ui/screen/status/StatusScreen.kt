package jamgmilk.fuwagit.ui.screen.status

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.ui.components.OperationResultDialog
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.components.TwoStepConfirmDialog

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
    modifier: Modifier = Modifier
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

    val statusStats = remember(files, staged, workspace) {
        StatusStats(
            totalChanges = files.size,
            staged = staged.size,
            unstaged = workspace.size,
            untracked = workspace.count { it.changeType == GitChangeType.Untracked },
            modified = files.count { it.changeType == GitChangeType.Modified },
            added = files.count { it.changeType == GitChangeType.Added },
            removed = files.count { it.changeType == GitChangeType.Removed }
        )
    }

    ScreenTemplate(
        title = "Status",
        modifier = modifier,
        actions = {
            FilledTonalIconButton(
                onClick = { statusViewModel.refreshWorkspace() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(18.dp)
                )
            }
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
                    title = "Workspace",
                    subtitle = "Unstaged changes",
                    files = workspace,
                    modifier = Modifier.weight(1f),
                    accentColor = colors.primary,
                    onFileAction = { file ->
                        statusViewModel.stageFile(file.path)
                    },
                    onDiscard = { file ->
                        statusViewModel.requestDiscardChanges(file.path)
                    },
                    emptyMessage = "Working directory clean"
                )

                FileSectionCard(
                    title = "Staged",
                    subtitle = "Ready to commit",
                    files = staged,
                    modifier = Modifier.weight(1f),
                    accentColor = colors.tertiary,
                    onFileAction = { file -> statusViewModel.unstageFile(file.path) },
                    emptyMessage = "Nothing to commit"
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

    // 鍗遍櫓鎿嶄綔鍙岀‘璁ゅ璇濇
    val pendingOperation = uiState.pendingOperation
    val pendingTarget = uiState.pendingOperationTarget
    if (pendingOperation != null && pendingTarget != null) {
        when (pendingOperation) {
            DangerousOperationType.DISCARD_CHANGES -> {
                TwoStepConfirmDialog(
                    operationType = DangerousOperationType.DISCARD_CHANGES,
                    targetName = pendingTarget,
                    description = "You are about to discard all changes to:",
                    warningMessage = "Any unstaged modifications will be permanently lost. This action cannot be undone.",
                    confirmText = "DISCARD",
                    onConfirm = { statusViewModel.confirmDiscardChanges() },
                    onDismiss = { statusViewModel.cancelPendingOperation() }
                )
            }
            else -> {}
        }
    }

    // 鎿嶄綔缁撴灉鍙嶉瀵硅瘽妗?
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
