package jamgmilk.fuwagit.ui.screen.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.state.RepoStateManager
import jamgmilk.fuwagit.domain.state.RepoState
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.usecase.git.CommitUseCase
import jamgmilk.fuwagit.domain.usecase.git.DiscardChangesUseCase
import jamgmilk.fuwagit.domain.usecase.git.FetchUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetDetailedStatusUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetBranchesUseCase
import jamgmilk.fuwagit.domain.usecase.git.HasGitDirUseCase
import jamgmilk.fuwagit.domain.usecase.git.PullUseCase
import jamgmilk.fuwagit.domain.usecase.git.PushUseCase
import jamgmilk.fuwagit.domain.usecase.git.StageAllUseCase
import jamgmilk.fuwagit.domain.usecase.git.StageFileUseCase
import jamgmilk.fuwagit.domain.usecase.git.UnstageAllUseCase
import jamgmilk.fuwagit.domain.usecase.git.UnstageFileUseCase
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
    val error: String? = null,
    val repoPath: String? = null,
    val repoName: String? = null,
    val repoState: RepoState = RepoState.NO_REPO_SELECTED,
    val isGitRepo: Boolean = false,
    val statusMessage: String = "Select a target repo",
    val currentBranch: GitBranch? = null,
    val branches: List<GitBranch> = emptyList(),
    val workspaceFiles: List<GitFileStatus> = emptyList(),
    val terminalOutput: List<String> = emptyList()
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val currentRepoManager: RepoStateManager,
    private val hasGitDirUseCase: HasGitDirUseCase,
    private val getDetailedStatusUseCase: GetDetailedStatusUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val stageAllUseCase: StageAllUseCase,
    private val unstageAllUseCase: UnstageAllUseCase,
    private val stageFileUseCase: StageFileUseCase,
    private val unstageFileUseCase: UnstageFileUseCase,
    private val discardChangesUseCase: DiscardChangesUseCase,
    private val commitUseCase: CommitUseCase,
    private val pullUseCase: PullUseCase,
    private val pushUseCase: PushUseCase,
    private val fetchUseCase: FetchUseCase
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
                        repoName = info.repoName,
                        repoState = info.state
                    )
                }

                if (info.state == RepoState.REPO_VALID && info.repoPath != null) {
                    refreshAll()
                } else if (info.state == RepoState.NO_REPO_SELECTED) {
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
            val isGitRepo = hasGitDirUseCase(path)
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

    fun discardChanges(filePath: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            discardChangesUseCase(path, filePath).fold(
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
            pullUseCase(path).fold(
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

    fun fetchRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            appendTerminalLog("git fetch", "Fetching from remote...")
            fetchUseCase(path).fold(
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
        val time = SimpleDateFormat("HH:mm:ss").format(Date())
        val line = "[$time] > $command\n$result"
        _uiState.update { it.copy(terminalOutput = it.terminalOutput + line) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
