package jamgmilk.fuwagit.ui.screen.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.usecase.GitOperationUseCases
import jamgmilk.fuwagit.domain.usecase.GitQueryUseCases
import kotlinx.coroutines.Dispatchers
import jamgmilk.fuwagit.domain.CurrentRepoManager
import jamgmilk.fuwagit.domain.CurrentRepoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StatusUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val repoPath: String? = null,
    val repoName: String? = null,
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

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val gitQueryUseCases: GitQueryUseCases,
    private val gitOperationUseCases: GitOperationUseCases,
    private val currentRepoManager: CurrentRepoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    private var currentRepoPath: String? = null

    init {
        viewModelScope.launch {
            currentRepoManager.currentRepoInfo.collectLatest { info ->
                currentRepoPath = info.repoPath
                val repoName = info.repoPath?.substringAfterLast("/")
                _uiState.update { it.copy(repoPath = info.repoPath, repoName = repoName) }
                
                if (info.state == CurrentRepoState.REPO_VALID && info.repoPath != null) {
                    refreshAll()
                } else if (info.state == CurrentRepoState.NO_REPO_SELECTED) {
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

        viewModelScope.launch {
            gitQueryUseCases.hasGitDir(path)
            val isGitRepo = gitQueryUseCases.hasGitDir(path)
            _uiState.update {
                it.copy(
                    isGitRepo = isGitRepo,
                    statusMessage = if (isGitRepo) "Git repository" else "Not a git repository"
                )
            }
        }
    }

    fun refreshWorkspace() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val filesResult = withContext(Dispatchers.IO) { gitQueryUseCases.getDetailedStatus(path) }
            val branchesResult = withContext(Dispatchers.IO) { gitQueryUseCases.getBranches(path) }

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
            gitOperationUseCases.stageAll(path).fold(
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
            gitOperationUseCases.unstageAll(path).fold(
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
            gitOperationUseCases.stageFile(path, filePath).fold(
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
            gitOperationUseCases.unstageFile(path, filePath).fold(
                onSuccess = { refreshWorkspace() },
                onFailure = { e ->
                    appendTerminalLog("git reset $filePath", "Error: ${e.message}")
                }
            )
        }
    }

    fun discardChanges(filePath: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            gitOperationUseCases.discardChanges(path, filePath).fold(
                onSuccess = { refreshWorkspace() },
                onFailure = { e ->
                    appendTerminalLog("git checkout -- $filePath", "Error: ${e.message}")
                }
            )
        }
    }

    fun commitChanges(message: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            gitOperationUseCases.commit(path, message).fold(
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
            gitOperationUseCases.pull(path).fold(
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
            gitOperationUseCases.push(path).fold(
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
            gitOperationUseCases.fetch(path).fold(
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

    private fun appendTerminalLog(command: String, result: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] > $command\n$result"
        _uiState.update { it.copy(terminalOutput = it.terminalOutput + line) }
    }

    fun initRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            gitOperationUseCases.initRepo(path).fold(
                onSuccess = { result ->
                    appendTerminalLog("git init", result)
                    checkRepoStatus()
                },
                onFailure = { e ->
                    appendTerminalLog("git init", "Error: ${e.message}")
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
