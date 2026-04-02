package jamgmilk.fuwagit.ui.screen.status

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.ui.components.OperationResultDialog
import jamgmilk.fuwagit.ui.components.TwoStepConfirmDialog
import jamgmilk.fuwagit.ui.components.CleanPreviewDialog
import jamgmilk.fuwagit.ui.theme.AppColors
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import jamgmilk.fuwagit.ui.theme.Sakura50
import jamgmilk.fuwagit.ui.theme.Sakura80
import jamgmilk.fuwagit.ui.theme.Sakura90

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
    val uiState by statusViewModel.uiState.collectAsState()
    val files = uiState.workspaceFiles
    val staged = remember(files) { files.filter { it.isStaged } }
    val workspace = remember(files) { files.filter { !it.isStaged } }
    var commitMessage by remember { mutableStateOf("") }

    val isRepo = uiState.isGitRepo
    val terminalLogs = uiState.terminalOutput
    val currentBranch = uiState.currentBranch

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Clean 按钮
                if (statusStats.untracked > 0) {
                    FilledTonalIconButton(
                        onClick = { statusViewModel.requestCleanUntracked() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clean untracked files",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
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
                    httpsCredentials = uiState.httpsCredentials,
                    sshKeys = uiState.sshKeys,
                    selectedCredentialUuid = uiState.selectedCredentialUuid,
                    selectedSshKeyUuid = uiState.selectedSshKeyUuid,
                    onSelectHttpsCredential = { uuid -> statusViewModel.selectHttpsCredential(uuid) },
                    onSelectSshKey = { uuid -> statusViewModel.selectSshKey(uuid) },
                    onLoadCredentials = { statusViewModel.loadCredentials() },
                    modifier = Modifier.fillMaxWidth()
                )

                ChangesOverviewCard(
                    stats = statusStats,
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
                    accentColor = Sakura80,
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
                    accentColor = AppColors.GitGreen,
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

    // 危险操作双确认对话框
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

    // Clean 预览对话框
    val untrackedFiles = uiState.untrackedFilesForClean
    if (untrackedFiles.isNotEmpty()) {
        CleanPreviewDialog(
            untrackedFiles = untrackedFiles,
            onConfirm = { statusViewModel.confirmCleanUntracked() },
            onDismiss = { statusViewModel.clearCleanPreview() }
        )
    }

    // 操作结果反馈对话框
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
