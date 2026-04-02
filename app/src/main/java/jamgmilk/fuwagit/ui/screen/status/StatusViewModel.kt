package jamgmilk.fuwagit.ui.screen.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.state.RepoStateManager
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.ui.components.OperationResult
import jamgmilk.fuwagit.domain.usecase.git.CommitUseCase
import jamgmilk.fuwagit.domain.usecase.git.DiscardChangesUseCase
import jamgmilk.fuwagit.domain.usecase.git.FetchUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetDetailedStatusUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetBranchesUseCase
import jamgmilk.fuwagit.domain.usecase.git.HasGitDirUseCase
import jamgmilk.fuwagit.domain.usecase.git.InitRepoUseCase
import jamgmilk.fuwagit.domain.usecase.git.PullUseCase
import jamgmilk.fuwagit.domain.usecase.git.PushUseCase
import jamgmilk.fuwagit.domain.usecase.git.StageAllUseCase
import jamgmilk.fuwagit.domain.usecase.git.StageFileUseCase
import jamgmilk.fuwagit.domain.usecase.git.UnstageAllUseCase
import jamgmilk.fuwagit.domain.usecase.git.UnstageFileUseCase
import jamgmilk.fuwagit.domain.usecase.git.CleanUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshKeysUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshPrivateKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshPassphraseUseCase
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

data class StatusUiState(
    val isLoading: Boolean = false,
    val isCheckingRepo: Boolean = false,
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
    val untrackedFilesForClean: List<String> = emptyList(),
    // 凭据相关状态
    val selectedCredentialUuid: String? = null,
    val selectedSshKeyUuid: String? = null,
    val httpsCredentials: List<HttpsCredential> = emptyList(),
    val sshKeys: List<SshKey> = emptyList()
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val currentRepoManager: RepoStateManager,
    private val hasGitDirUseCase: HasGitDirUseCase,
    private val initRepoUseCase: InitRepoUseCase,
    private val getDetailedStatusUseCase: GetDetailedStatusUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val stageAllUseCase: StageAllUseCase,
    private val unstageAllUseCase: UnstageAllUseCase,
    private val stageFileUseCase: StageFileUseCase,
    private val unstageFileUseCase: UnstageFileUseCase,
    private val discardChangesUseCase: DiscardChangesUseCase,
    private val cleanUseCase: CleanUseCase,
    private val commitUseCase: CommitUseCase,
    private val pullUseCase: PullUseCase,
    private val pushUseCase: PushUseCase,
    private val fetchUseCase: FetchUseCase,
    // 凭据相关
    private val getHttpsCredentialsUseCase: GetHttpsCredentialsUseCase,
    private val getHttpsPasswordUseCase: GetHttpsPasswordUseCase,
    private val getSshKeysUseCase: GetSshKeysUseCase,
    private val getSshPrivateKeyUseCase: GetSshPrivateKeyUseCase,
    private val getSshPassphraseUseCase: GetSshPassphraseUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    private var currentRepoPath: String? = null

    init {
        viewModelScope.launch {
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
    }

    fun initRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            initRepoUseCase(path).fold(
                onSuccess = { result ->
                    appendTerminalLog("git init", result)
                    refreshAll()
                },
                onFailure = { e ->
                    appendTerminalLog("git init", "Error: ${e.message}")
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
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
            val isGitRepo = hasGitDirUseCase(path)
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

            val filesResult = withContext(Dispatchers.IO) { getDetailedStatusUseCase(path) }
            val branchesResult = withContext(Dispatchers.IO) { getBranchesUseCase(path) }

            filesResult.fold(
                onSuccess = { files ->
                    _uiState.update {
                        it.copy(
                            workspaceFiles = files,
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            )

            branchesResult.fold(
                onSuccess = { branches ->
                    val currentBranch = branches.find { it.isCurrent }
                    _uiState.update { it.copy(branches = branches, currentBranch = currentBranch) }
                },
                onFailure = { e ->
                    appendTerminalLog("git branch", "Error: ${e.message}")
                }
            )
        }
    }

    fun stageAll() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            stageAllUseCase(path).fold(
                onSuccess = { result ->
                    appendTerminalLog("git add -A", result)
                    refreshWorkspace()
                },
                onFailure = { e ->
                    appendTerminalLog("git add -A", "Error: ${e.message}")
                }
            )
        }
    }

    fun unstageAll() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            unstageAllUseCase(path).fold(
                onSuccess = { result ->
                    appendTerminalLog("git reset", result)
                    refreshWorkspace()
                },
                onFailure = { e ->
                    appendTerminalLog("git reset", "Error: ${e.message}")
                }
            )
        }
    }

    fun stageFile(filePath: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            stageFileUseCase(path, filePath).fold(
                onSuccess = { refreshWorkspace() },
                onFailure = { e ->
                    appendTerminalLog("git add $filePath", "Error: ${e.message}")
                }
            )
        }
    }

    fun unstageFile(filePath: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            unstageFileUseCase(path, filePath).fold(
                onSuccess = { refreshWorkspace() },
                onFailure = { e ->
                    appendTerminalLog("git reset $filePath", "Error: ${e.message}")
                }
            )
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

    fun requestCleanUntracked() {
        val path = currentRepoPath ?: return
        val untrackedFiles = _uiState.value.workspaceFiles
            .filter { it.changeType == jamgmilk.fuwagit.domain.model.git.GitChangeType.Untracked }
            .map { it.path }

        if (untrackedFiles.isEmpty()) {
            _uiState.update {
                it.copy(
                    operationResult = OperationResult.Failure("No untracked files to clean", "")
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                untrackedFilesForClean = untrackedFiles
            )
        }
    }

    // ============ 危险操作：执行 ============

    fun confirmDiscardChanges() {
        val path = currentRepoPath ?: return
        val filePath = _uiState.value.pendingOperationTarget ?: return

        viewModelScope.launch {
            discardChangesUseCase(path, filePath).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Success("Changes to '$filePath' have been discarded"),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                    refreshWorkspace()
                },
                onFailure = { e ->
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
            )
        }
    }

    fun confirmCleanUntracked() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            cleanUseCase(path, dryRun = false).fold(
                onSuccess = {
                    val count = _uiState.value.untrackedFilesForClean.size
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Success("$count untracked file(s) cleaned"),
                            pendingOperation = null,
                            pendingOperationTarget = null,
                            untrackedFilesForClean = emptyList()
                        )
                    }
                    refreshWorkspace()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(
                                e.message ?: "Unknown error",
                                "Some files may be locked or in use. Close any open files and try again."
                            ),
                            pendingOperation = null,
                            pendingOperationTarget = null,
                            untrackedFilesForClean = emptyList()
                        )
                    }
                }
            )
        }
    }

    fun commitChanges(message: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            commitUseCase(path, message).fold(
                onSuccess = { result ->
                    appendTerminalLog("git commit -m \"${message.trim()}\"", result)
                    refreshWorkspace()
                },
                onFailure = { e ->
                    appendTerminalLog("git commit", "Error: ${e.message}")
                }
            )
        }
    }

    fun pullRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            appendTerminalLog("git pull", "Attempting pull. Remote auth may be required")
            val credentials = loadSelectedCredentials()
            pullUseCase(path, credentials).fold(
                onSuccess = { result ->
                    appendTerminalLog("git pull", result.toString())
                    refreshWorkspace()
                },
                onFailure = { e ->
                    appendTerminalLog("git pull", "Error: ${e.message}")
                }
            )
        }
    }

    fun pushRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            appendTerminalLog("git push", "Attempting push. Remote auth may be required")
            val credentials = loadSelectedCredentials()
            pushUseCase(path, credentials).fold(
                onSuccess = { result ->
                    appendTerminalLog("git push", result)
                },
                onFailure = { e ->
                    appendTerminalLog("git push", "Error: ${e.message}")
                }
            )
        }
    }

    fun fetchRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            appendTerminalLog("git fetch", "Fetching from remote...")
            val credentials = loadSelectedCredentials()
            fetchUseCase(path, credentials).fold(
                onSuccess = { result ->
                    appendTerminalLog("git fetch", result)
                    refreshAll()
                },
                onFailure = { e ->
                    appendTerminalLog("git fetch", "Error: ${e.message}")
                }
            )
        }
    }

    // ============ 凭据管理 ============

    fun loadCredentials() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val httpsCreds = getHttpsCredentialsUseCase().getOrNull() ?: emptyList()
                val sshKeyList = getSshKeysUseCase().getOrNull() ?: emptyList()
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

    private suspend fun loadSelectedCredentials(): CloneCredential? {
        val state = _uiState.value
        return when {
            state.selectedCredentialUuid != null -> {
                val uuid = state.selectedCredentialUuid!!
                val cred = state.httpsCredentials.find { it.uuid == uuid } ?: return null
                val password = getHttpsPasswordUseCase(uuid).getOrNull() ?: return null
                CloneCredential.Https(cred.username, password)
            }
            state.selectedSshKeyUuid != null -> {
                val uuid = state.selectedSshKeyUuid!!
                val privateKey = getSshPrivateKeyUseCase(uuid).getOrNull() ?: return null
                val passphrase = getSshPassphraseUseCase(uuid).getOrNull()
                CloneCredential.Ssh(privateKey, passphrase)
            }
            else -> {
                // 自动选择第一个可用的凭据
                if (state.httpsCredentials.isNotEmpty()) {
                    val cred = state.httpsCredentials.first()
                    val password = getHttpsPasswordUseCase(cred.uuid).getOrNull() ?: return null
                    CloneCredential.Https(cred.username, password)
                } else if (state.sshKeys.isNotEmpty()) {
                    val key = state.sshKeys.first()
                    val privateKey = getSshPrivateKeyUseCase(key.uuid).getOrNull() ?: return null
                    val passphrase = getSshPassphraseUseCase(key.uuid).getOrNull()
                    CloneCredential.Ssh(privateKey, passphrase)
                } else {
                    null
                }
            }
        }
    }

    private fun appendTerminalLog(command: String, result: String) {
        val time = SimpleDateFormat("HH:mm:ss").format(Date())
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

    fun clearCleanPreview() {
        _uiState.update {
            it.copy(untrackedFilesForClean = emptyList())
        }
    }
}