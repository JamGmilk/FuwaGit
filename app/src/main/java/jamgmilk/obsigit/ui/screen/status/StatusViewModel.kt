package jamgmilk.obsigit.ui.screen.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jamgmilk.obsigit.data.source.RepoPathUtils
import jamgmilk.obsigit.domain.model.GitBranch
import jamgmilk.obsigit.domain.model.GitFileStatus
import jamgmilk.obsigit.domain.repository.GitRepository
import jamgmilk.obsigit.domain.usecase.git.CommitChangesUseCase
import jamgmilk.obsigit.domain.usecase.git.GetBranchesUseCase
import jamgmilk.obsigit.domain.usecase.git.GetWorkspaceStatusUseCase
import jamgmilk.obsigit.domain.usecase.git.PullUseCase
import jamgmilk.obsigit.domain.usecase.git.PushUseCase
import jamgmilk.obsigit.domain.usecase.git.StageAllUseCase
import jamgmilk.obsigit.domain.usecase.git.StageFileUseCase
import jamgmilk.obsigit.domain.usecase.git.UnstageAllUseCase
import jamgmilk.obsigit.domain.usecase.git.UnstageFileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StatusUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val repoPath: String? = null,
    val isGitRepo: Boolean = false,
    val statusMessage: String = "Select a target repo",
    val branch: String = "",
    val currentBranch: GitBranch? = null,
    val branches: List<GitBranch> = emptyList(),
    val workspaceFiles: List<GitFileStatus> = emptyList(),
    val stagedFiles: List<GitFileStatus> = emptyList(),
    val unstagedFiles: List<GitFileStatus> = emptyList(),
    val terminalOutput: List<String> = emptyList()
)

class StatusViewModel(
    private val getWorkspaceStatusUseCase: GetWorkspaceStatusUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val stageAllUseCase: StageAllUseCase,
    private val unstageAllUseCase: UnstageAllUseCase,
    private val stageFileUseCase: StageFileUseCase,
    private val unstageFileUseCase: UnstageFileUseCase,
    private val commitChangesUseCase: CommitChangesUseCase,
    private val pullUseCase: PullUseCase,
    private val pushUseCase: PushUseCase,
    private val gitRepository: GitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    private var currentRepoPath: String? = null

    fun setRepoPath(path: String?) {
        currentRepoPath = path
        _uiState.update { it.copy(repoPath = path) }
        if (path != null) {
            refreshAll()
        } else {
            _uiState.update { 
                it.copy(
                    isGitRepo = false,
                    statusMessage = "Select a target repo",
                    workspaceFiles = emptyList(),
                    branches = emptyList()
                )
            }
        }
    }

    fun refreshAll() {
        checkRepoStatus()
        refreshWorkspace()
    }

    private fun checkRepoStatus() {
        val path = currentRepoPath
        if (path == null) {
            _uiState.update { 
                it.copy(
                    isGitRepo = false,
                    statusMessage = "Select a target repo path"
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            gitRepository.getStatus(path)
                .onSuccess { status ->
                    _uiState.update { 
                        it.copy(
                            isGitRepo = status.isGitRepo,
                            statusMessage = status.message
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isGitRepo = false,
                            statusMessage = "Path: ${RepoPathUtils.shortDisplayPath(path)}\nError: ${e.message}"
                        )
                    }
                }
        }
    }

    fun refreshWorkspace() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val filesResult = getWorkspaceStatusUseCase(path)
            val branchesResult = getBranchesUseCase(path)
            
            filesResult.fold(
                onSuccess = { files ->
                    _uiState.update {
                        it.copy(
                            workspaceFiles = files,
                            stagedFiles = files.filter { file -> file.isStaged },
                            unstagedFiles = files.filter { file -> !file.isStaged },
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
                onFailure = { }
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

    fun discardChanges(filePath: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch(Dispatchers.IO) {
            gitRepository.discardChanges(path, filePath)
                .onSuccess {
                    refreshWorkspace()
                }
                .onFailure { e ->
                    appendTerminalLog("git checkout -- $filePath", "Error: ${e.message}")
                }
        }
    }

    fun commitChanges(message: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            commitChangesUseCase(path, message).fold(
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
            pullUseCase(path).fold(
                onSuccess = { result ->
                    appendTerminalLog("git pull", result)
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
            pushUseCase(path).fold(
                onSuccess = { result ->
                    appendTerminalLog("git push", result)
                },
                onFailure = { e ->
                    appendTerminalLog("git push", "Error: ${e.message}")
                }
            )
        }
    }

    private fun appendTerminalLog(command: String, result: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] > $command\n$result"
        _uiState.update { it.copy(terminalOutput = it.terminalOutput + line) }
    }

    fun initRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch(Dispatchers.IO) {
            gitRepository.initRepo(path)
                .onSuccess { result ->
                    appendTerminalLog("git init", result)
                    checkRepoStatus()
                }
                .onFailure { e ->
                    appendTerminalLog("git init", "Error: ${e.message}")
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
