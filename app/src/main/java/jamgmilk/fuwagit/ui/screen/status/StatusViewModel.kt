package jamgmilk.fuwagit.ui.screen.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.ui.state.RepoStateManager
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.ui.components.OperationResult
import jamgmilk.fuwagit.domain.usecase.git.GitStatusFacade
import jamgmilk.fuwagit.domain.usecase.git.GitSyncFacade
import jamgmilk.fuwagit.domain.usecase.git.MergeUseCase
import jamgmilk.fuwagit.domain.usecase.credential.CredentialFacade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

import androidx.compose.runtime.Stable
import java.util.Locale

@Stable
data class StatusUiState(
    val isLoading: Boolean = false,
    val isCheckingRepo: Boolean = true,
    val error: String? = null,
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
    val sshKeys: List<SshKey> = emptyList()
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

    private var currentRepoPath: String? = null

    init {
        viewModelScope.launch {
            loadCredentials()
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
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
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
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
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
                            operationResult = OperationResult.Success("Changes to '$filePath' have been discarded"),
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
                                e.message ?: "Unknown error",
                                "Make sure the file is not staged or locked by another process."
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
            appendTerminalLog("git pull", "Attempting pull. Remote auth may be required")
            val remoteUrl = gitStatus.getRemoteUrl(path)
            val credentials = loadSelectedCredentials(remoteUrl)
            gitSync.pull(path, credentials)
                .onSuccess { result ->
                    appendTerminalLog("git pull", result.toString())
                    if (result.hasConflicts) {
                        // Pull 产生冲突，显示冲突解决 UI
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
                        refreshWorkspace()
                    }
                }
                .onError { e ->
                    appendTerminalLog("git pull", "Error: ${e.message}")
                }
        }
    }

    fun pushRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            appendTerminalLog("git push", "Attempting push. Remote auth may be required")
            val remoteUrl = gitStatus.getRemoteUrl(path)
            val credentials = loadSelectedCredentials(remoteUrl)
            gitSync.push(path, credentials)
                .onSuccess { result ->
                    appendTerminalLog("git push", result)
                }
                .onError { e ->
                    appendTerminalLog("git push", "Error: ${e.message}")
                }
        }
    }

    fun fetchRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            appendTerminalLog("git fetch", "Fetching from remote...")
            val remoteUrl = gitStatus.getRemoteUrl(path)
            val credentials = loadSelectedCredentials(remoteUrl)
            gitSync.fetch(path, credentials)
                .onSuccess { result ->
                    appendTerminalLog("git fetch", result)
                    refreshAll()
                }
                .onError { e ->
                    appendTerminalLog("git fetch", "Error: ${e.message}")
                }
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
            val httpsCreds = credential.getHttpsCredentials().getOrNull() ?: emptyList()
            val sshKeyList = credential.getSshKeys().getOrNull() ?: emptyList()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        httpsCredentials = httpsCreds,
                        sshKeys = sshKeyList
                    )
                }
            }
        }
    }

    fun selectHttpsCredential(uuid: String?) {
        _uiState.update {
            it.copy(
                selectedCredentialUuid = uuid,
                selectedSshKeyUuid = null
            )
        }
    }

    fun selectSshKey(uuid: String?) {
        _uiState.update {
            it.copy(
                selectedSshKeyUuid = uuid,
                selectedCredentialUuid = null
            )
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
                .onError { e -> _uiState.update { it.copy(error = e.message) } }
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
                                operationResult = OperationResult.Success("Conflicts resolved"))
                        }
                    }
                }
                .onError { e -> _uiState.update { it.copy(error = e.message) } }
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
                                operationResult = OperationResult.Success("Conflicts resolved"))
                        }
                    }
                }
                .onError { e -> _uiState.update { it.copy(error = e.message) } }
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
                            operationResult = OperationResult.Success(result))
                    }
                    refreshWorkspace()
                }
                .onError { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun continueRebase() {
        val path = currentRepoPath ?: return
        viewModelScope.launch {
            mergeUseCase.continueRebase(path)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(isResolvingConflict = false, conflictResult = null,
                            operationResult = OperationResult.Success(result))
                    }
                    refreshWorkspace()
                }
                .onError { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }
}