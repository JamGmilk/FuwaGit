package jamgmilk.fuwagit.ui.screen.status

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.UiMessage
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.usecase.credential.CredentialFacade
import jamgmilk.fuwagit.domain.usecase.git.GitStatusFacade
import jamgmilk.fuwagit.domain.usecase.git.GitSyncFacade
import jamgmilk.fuwagit.domain.usecase.git.MergeUseCase
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.ui.components.OperationResult
import jamgmilk.fuwagit.ui.state.RepoStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class StatusEvent {
    data class PushSuccess(val message: String) : StatusEvent()
    data class PushError(val message: String) : StatusEvent()
    data class PullSuccess(val message: String) : StatusEvent()
    data class PullError(val message: String) : StatusEvent()
    data class FetchSuccess(val message: String) : StatusEvent()
    data class FetchError(val message: String) : StatusEvent()
    data object CredentialUnlockRequired : StatusEvent()
}

@Stable
data class StatusUiState(
    val isLoading: Boolean = false,
    val isCheckingRepo: Boolean = true,
    val error: UiMessage? = null,
    val repoPath: String? = null,
    val repoName: String? = null,
    val isGitRepo: Boolean = false,
    val statusMessage: String = "Select a target repo",
    val currentBranch: GitBranch? = null,
    val branches: List<GitBranch> = emptyList(),
    val workspaceFiles: List<GitFileStatus> = emptyList(),
    val terminalOutput: List<String> = emptyList(),
    // 危险操作相关状态
    val pendingOperation: DangerousOperationType? = null,
    val pendingOperationTarget: String? = null,
    val operationResult: OperationResult? = null,
    // 冲突解决相关状态
    val conflictResult: ConflictResult? = null,
    val isResolvingConflict: Boolean = false,
    // 凭证相关状态
    val selectedCredentialUuid: String? = null,
    val selectedSshKeyUuid: String? = null,
    val httpsCredentials: List<HttpsCredential> = emptyList(),
    val sshKeys: List<SshKey> = emptyList(),
    // 解锁状态
    val isCredentialUnlocked: Boolean = false,
    val pendingCredentialOperation: suspend () -> Unit = {}
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val currentRepoManager: RepoStateManager,
    private val gitStatus: GitStatusFacade,
    private val gitSync: GitSyncFacade,
    private val mergeUseCase: MergeUseCase,
    private val credential: CredentialFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<StatusEvent>()
    val events: SharedFlow<StatusEvent> = _events.asSharedFlow()

    private var currentRepoPath: String? = null

    init {
        viewModelScope.launch {
            loadCredentials()
            refreshUnlockState()
            currentRepoManager.repoInfo.collectLatest { info ->
                currentRepoPath = info.repoPath
                _uiState.update {
                    it.copy(
                        repoPath = info.repoPath,
                        repoName = info.repoName
                    )
                }

                if (info.isValidGit) {
                    refreshAll()
                } else if (info.repoPath == null) {
                    _uiState.update {
                        it.copy(
                            isGitRepo = false,
                            statusMessage = "Select a target repo",
                            workspaceFiles = emptyList(),
                            branches = emptyList(),
                            currentBranch = null
                        )
                    }
                }
            }
        }
    }

    fun refreshAll() {
        checkRepoStatus()
        refreshWorkspace()
        loadCredentials()
    }

    fun initRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            gitStatus.initRepo(path)
                .onSuccess { result ->
                    appendTerminalLog("git init", result)
                    refreshAll()
                }
                .onError { e ->
                    appendTerminalLog("git init", "Error: ${e.message}")
                    _uiState.update { it.copy(isLoading = false, error = UiMessage.Generic(e.message ?: "Unknown error")) }
                }
        }
    }

    private fun checkRepoStatus() {
        val path = currentRepoPath
        if (path == null) {
            _uiState.update {
                it.copy(
                    isGitRepo = false,
                    isCheckingRepo = false,
                    statusMessage = "Select a target repo path"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingRepo = true) }
            val isGitRepo = gitStatus.hasGitDir(path)
            _uiState.update {
                it.copy(
                    isGitRepo = isGitRepo,
                    isCheckingRepo = false,
                    statusMessage = if (isGitRepo) "Git repository" else "Not a git repository"
                )
            }
        }
    }

    fun refreshWorkspace() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val filesResult = withContext(Dispatchers.IO) { gitStatus.getDetailedStatus(path) }
            val branchesResult = withContext(Dispatchers.IO) { gitStatus.getBranches(path) }

            filesResult
                .onSuccess { files ->
                    _uiState.update {
                        it.copy(
                            workspaceFiles = files,
                            isLoading = false
                        )
                    }
                }
                .onError { e ->
                    _uiState.update { it.copy(error = UiMessage.Generic(e.message ?: "Unknown error"), isLoading = false) }
                }

            branchesResult
                .onSuccess { branches ->
                    val currentBranch = branches.find { it.isCurrent }
                    _uiState.update { it.copy(branches = branches, currentBranch = currentBranch) }
                }
                .onError { e ->
                    appendTerminalLog("git branch", "Error: ${e.message}")
                }
        }
    }

    fun stageAll() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            gitStatus.stageAll(path)
                .onSuccess { result ->
                    appendTerminalLog("git add -A", result)
                    refreshWorkspace()
                }
                .onError { e ->
                    appendTerminalLog("git add -A", "Error: ${e.message}")
                }
        }
    }

    fun unstageAll() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            gitStatus.unstageAll(path)
                .onSuccess { result ->
                    appendTerminalLog("git reset", result)
                    refreshWorkspace()
                }
                .onError { e ->
                    appendTerminalLog("git reset", "Error: ${e.message}")
                }
        }
    }

    fun stageFile(filePath: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            gitStatus.stageFile(path, filePath)
                .onSuccess { refreshWorkspace() }
                .onError { e ->
                    appendTerminalLog("git add $filePath", "Error: ${e.message}")
                }
        }
    }

    fun unstageFile(filePath: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            gitStatus.unstageFile(path, filePath)
                .onSuccess { refreshWorkspace() }
                .onError { e ->
                    appendTerminalLog("git reset $filePath", "Error: ${e.message}")
                }
        }
    }

    // ============ 危险操作：请求确认 ============

    fun requestDiscardChanges(filePath: String) {
        _uiState.update {
            it.copy(
                pendingOperation = DangerousOperationType.DISCARD_CHANGES,
                pendingOperationTarget = filePath
            )
        }
    }

    // ============ 危险操作：执行 ============

    fun confirmDiscardChanges() {
        val path = currentRepoPath ?: return
        val filePath = _uiState.value.pendingOperationTarget ?: return

        viewModelScope.launch {
            gitStatus.discardChanges(path, filePath)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Success(UiMessage.Discard.ChangesDiscarded(filePath)),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                    refreshWorkspace()
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(
                                UiMessage.Generic(e.message ?: "Unknown error"),
                                UiMessage.Discard.NotStagedOrLocked
                            ),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                }
        }
    }

    fun commitChanges(message: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            gitStatus.commit(path, message)
                .onSuccess { result ->
                    appendTerminalLog("git commit -m \"${message.trim()}\"", result)
                    refreshWorkspace()
                    currentRepoManager.notifyRefresh()
                }
                .onError { e ->
                    appendTerminalLog("git commit", "Error: ${e.message}")
                }
        }
    }

    fun pullRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            if (!credential.isUnlocked()) {
                _uiState.update {
                    it.copy(
                        pendingCredentialOperation = { executePull(path) }
                    )
                }
                _events.emit(StatusEvent.CredentialUnlockRequired)
                return@launch
            }
            executePull(path)
        }
    }

    private suspend fun executePull(path: String) {
        appendTerminalLog("git pull", "Attempting pull. Remote auth may be required")
        val remoteUrl = gitStatus.getRemoteUrl(path)
        val credentials = loadSelectedCredentials(remoteUrl)
        gitSync.pull(path, credentials)
            .onSuccess { result ->
                appendTerminalLog("git pull", result.toString())
                if (result.hasConflicts) {
                    val conflictResult = mergeUseCase.getConflicts(path)
                    conflictResult.onSuccess { conflicts ->
                        _uiState.update {
                            it.copy(
                                conflictResult = conflicts,
                                isResolvingConflict = true
                            )
                        }
                    }
                } else {
                    _events.emit(StatusEvent.PullSuccess(result.message))
                    refreshWorkspace()
                }
            }
            .onError { e ->
                appendTerminalLog("git pull", "Error: ${e.message}")
                _events.emit(StatusEvent.PullError(e.message ?: "Pull failed"))
            }
    }

    fun pushRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            if (!credential.isUnlocked()) {
                _uiState.update {
                    it.copy(
                        pendingCredentialOperation = { executePush(path) }
                    )
                }
                _events.emit(StatusEvent.CredentialUnlockRequired)
                return@launch
            }
            executePush(path)
        }
    }

    private suspend fun executePush(path: String) {
        appendTerminalLog("git push", "Attempting push. Remote auth may be required")
        val remoteUrl = gitStatus.getRemoteUrl(path)
        val credentials = loadSelectedCredentials(remoteUrl)
        gitSync.push(path, credentials)
            .onSuccess { result ->
                appendTerminalLog("git push", result)
                _events.emit(StatusEvent.PushSuccess(result))
            }
            .onError { e ->
                appendTerminalLog("git push", "Error: ${e.message}")
                _events.emit(StatusEvent.PushError(e.message ?: "Push failed"))
            }
    }

    fun fetchRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            if (!credential.isUnlocked()) {
                _uiState.update {
                    it.copy(
                        pendingCredentialOperation = { executeFetch(path) }
                    )
                }
                _events.emit(StatusEvent.CredentialUnlockRequired)
                return@launch
            }
            executeFetch(path)
        }
    }

    private suspend fun executeFetch(path: String) {
        appendTerminalLog("git fetch", "Fetching from remote...")
        val remoteUrl = gitStatus.getRemoteUrl(path)
        val credentials = loadSelectedCredentials(remoteUrl)
        gitSync.fetch(path, credentials)
            .onSuccess { result ->
                appendTerminalLog("git fetch", result)
                _events.emit(StatusEvent.FetchSuccess(result))
                refreshAll()
            }
            .onError { e ->
                appendTerminalLog("git fetch", "Error: ${e.message}")
                _events.emit(StatusEvent.FetchError(e.message ?: "Fetch failed"))
            }
    }

    // ============ 凭证管理 ============

    fun loadCredentials() {
        viewModelScope.launch {
            refreshCredentials()
        }
    }

    private suspend fun refreshCredentials() {
        withContext(Dispatchers.IO) {
            val httpsCredentials = credential.getHttpsCredentials().getOrNull() ?: emptyList()
            val sshKeyList = credential.getSshKeys().getOrNull() ?: emptyList()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        httpsCredentials = httpsCredentials,
                        sshKeys = sshKeyList
                    )
                }
            }
        }
    }

    private suspend fun loadSelectedCredentials(remoteUrl: String? = null): CloneCredential? {
        var state = _uiState.value
        // 如果当前列表为空，尝试同步刷新一下
        if (state.httpsCredentials.isEmpty() && state.sshKeys.isEmpty()) {
            refreshCredentials()
            state = _uiState.value
        }
        
        return credential.resolveCredentials(
            state.selectedCredentialUuid,
            state.selectedSshKeyUuid,
            state.httpsCredentials,
            state.sshKeys,
            remoteUrl
        )
    }

    private fun appendTerminalLog(command: String, result: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val line = "[$time] > $command\n$result"
        _uiState.update { it.copy(terminalOutput = it.terminalOutput + line) }
    }

    fun clearOperationResult() {
        _uiState.update {
            it.copy(
                operationResult = null,
                pendingOperation = null,
                pendingOperationTarget = null
            )
        }
    }

    fun cancelPendingOperation() {
        _uiState.update {
            it.copy(
                pendingOperation = null,
                pendingOperationTarget = null
            )
        }
    }

    // ============ 冲突处理方法 ============

    fun markConflictResolved(filePath: String) {
        val path = currentRepoPath ?: return
        viewModelScope.launch {
            mergeUseCase.resolveConflict(path, filePath)
                .onSuccess { checkConflictStatus() }
                .onError { e -> _uiState.update { it.copy(error = UiMessage.Generic(e.message ?: "Unknown error")) } }
        }
    }

    fun checkConflictStatus() {
        val path = currentRepoPath ?: return
        viewModelScope.launch {
            mergeUseCase.getConflicts(path)
                .onSuccess { result ->
                    if (result.isConflicting) {
                        _uiState.update { it.copy(conflictResult = result, isResolvingConflict = true) }
                    } else {
                        refreshWorkspace()
                        _uiState.update {
                            it.copy(isResolvingConflict = false, conflictResult = null,
                                operationResult = OperationResult.Success(UiMessage.Conflict.Resolved))
                        }
                    }
                }
                .onError { e -> _uiState.update { it.copy(error = UiMessage.Generic(e.message ?: "Unknown error")) } }
        }
    }

    fun finishConflictResolution() {
        val path = currentRepoPath ?: return
        viewModelScope.launch {
            mergeUseCase.getConflicts(path)
                .onSuccess { result ->
                    if (result.allResolved || result.allStaged) {
                        refreshWorkspace()
                        _uiState.update {
                            it.copy(isResolvingConflict = false, conflictResult = null,
                                operationResult = OperationResult.Success(UiMessage.Conflict.Resolved))
                        }
                    }
                }
                .onError { e -> _uiState.update { it.copy(error = UiMessage.Generic(e.message ?: "Unknown error")) } }
        }
    }

    fun cancelConflictResolution() {
        _uiState.update { it.copy(isResolvingConflict = false, conflictResult = null) }
    }

    fun abortRebase() {
        val path = currentRepoPath ?: return
        viewModelScope.launch {
            mergeUseCase.abortRebase(path)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(isResolvingConflict = false, conflictResult = null,
                            operationResult = OperationResult.Success(UiMessage.Generic(result)))
                    }
                    refreshWorkspace()
                }
                .onError { e -> _uiState.update { it.copy(error = UiMessage.Generic(e.message ?: "Unknown error")) } }
        }
    }

    fun continueRebase() {
        val path = currentRepoPath ?: return
        viewModelScope.launch {
            mergeUseCase.continueRebase(path)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(isResolvingConflict = false, conflictResult = null,
                            operationResult = OperationResult.Success(UiMessage.Generic(result)))
                    }
                    refreshWorkspace()
                }
                .onError { e -> _uiState.update { it.copy(error = UiMessage.Generic(e.message ?: "Unknown error")) } }
        }
    }

    fun onCredentialUnlocked() {
        viewModelScope.launch {
            val pendingOp = _uiState.value.pendingCredentialOperation
            if (pendingOp != {}) {
                pendingOp()
            }
            _uiState.update {
                it.copy(
                    pendingCredentialOperation = {},
                    isCredentialUnlocked = credential.isUnlocked()
                )
            }
        }
    }

    fun refreshUnlockState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCredentialUnlocked = credential.isUnlocked()) }
        }
    }
}