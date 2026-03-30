package jamgmilk.fuwagit.ui.screen.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.data.source.RepoPathUtils
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.model.git.GitStash
import jamgmilk.fuwagit.domain.model.git.GitTag
import jamgmilk.fuwagit.domain.usecase.GitOperationUseCases
import jamgmilk.fuwagit.domain.usecase.GitQueryUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isGitRepo: Boolean = false,
    val statusMessage: String = "Select a target repo",
    val branch: String = "",
    val currentBranch: GitBranch? = null,
    val branches: List<GitBranch> = emptyList(),
    val workspaceFiles: List<GitFileStatus> = emptyList(),
    val stagedFiles: List<GitFileStatus> = emptyList(),
    val unstagedFiles: List<GitFileStatus> = emptyList(),
    val stashList: List<GitStash> = emptyList(),
    val tagList: List<GitTag> = emptyList(),
    val remoteList: List<GitRemote> = emptyList(),
    val terminalOutput: List<String> = emptyList()
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val gitQueryUseCases: GitQueryUseCases,
    private val gitOperationUseCases: GitOperationUseCases
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

    fun refreshStashList() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                gitQueryUseCases.getStashList(path)
            }.fold(
                onSuccess = { stashes ->
                    _uiState.update { it.copy(stashList = stashes) }
                },
                onFailure = { }
            )
        }
    }

    fun stashChanges(message: String? = null) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                gitOperationUseCases.stashChanges(path, message)
            }.fold(
                onSuccess = { result ->
                    appendTerminalLog("git stash", result)
                    refreshWorkspace()
                    refreshStashList()
                },
                onFailure = { e ->
                    appendTerminalLog("git stash", "Error: ${e.message}")
                    _uiState.update { it.copy(error = "Stash failed: ${e.message}") }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun applyStash(stashIndex: Int, dropAfterApply: Boolean = false) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                gitOperationUseCases.applyStash(path, stashIndex, dropAfterApply)
            }.fold(
                onSuccess = { result ->
                    appendTerminalLog("git stash pop", result)
                    refreshWorkspace()
                    refreshStashList()
                },
                onFailure = { e ->
                    appendTerminalLog("git stash pop", "Error: ${e.message}")
                    _uiState.update { it.copy(error = "Apply stash failed: ${e.message}") }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun dropStash(stashIndex: Int) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                gitOperationUseCases.dropStash(path, stashIndex)
            }.fold(
                onSuccess = { result ->
                    appendTerminalLog("git stash drop", result)
                    refreshStashList()
                },
                onFailure = { e ->
                    appendTerminalLog("git stash drop", "Error: ${e.message}")
                    _uiState.update { it.copy(error = "Drop stash failed: ${e.message}") }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refreshTagList() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                gitQueryUseCases.getTags(path)
            }.fold(
                onSuccess = { tags ->
                    _uiState.update { it.copy(tagList = tags) }
                },
                onFailure = { }
            )
        }
    }

    fun createTag(tagName: String, message: String? = null) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                gitOperationUseCases.createTag(path, tagName, message)
            }.fold(
                onSuccess = { result ->
                    appendTerminalLog("git tag", result)
                    refreshTagList()
                },
                onFailure = { e ->
                    appendTerminalLog("git tag", "Error: ${e.message}")
                    _uiState.update { it.copy(error = "Create tag failed: ${e.message}") }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun deleteTag(tagName: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                gitOperationUseCases.deleteTag(path, tagName)
            }.fold(
                onSuccess = { result ->
                    appendTerminalLog("git tag -d", result)
                    refreshTagList()
                },
                onFailure = { e ->
                    appendTerminalLog("git tag -d", "Error: ${e.message}")
                    _uiState.update { it.copy(error = "Delete tag failed: ${e.message}") }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refreshRemoteList() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                gitQueryUseCases.getRemotes(path)
            }.fold(
                onSuccess = { remotes ->
                    _uiState.update { it.copy(remoteList = remotes) }
                },
                onFailure = { }
            )
        }
    }

    fun deleteRemote(remoteName: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                gitOperationUseCases.deleteRemote(path, remoteName)
            }.fold(
                onSuccess = { result ->
                    appendTerminalLog("git remote remove", result)
                    refreshRemoteList()
                },
                onFailure = { e ->
                    appendTerminalLog("git remote remove", "Error: ${e.message}")
                    _uiState.update { it.copy(error = "Delete remote failed: ${e.message}") }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun renameBranch(oldName: String, newName: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                gitOperationUseCases.renameBranch(path, oldName, newName)
            }.fold(
                onSuccess = { result ->
                    appendTerminalLog("git branch -m", result)
                    refreshWorkspace()
                },
                onFailure = { e ->
                    appendTerminalLog("git branch -m", "Error: ${e.message}")
                    _uiState.update { it.copy(error = "Rename branch failed: ${e.message}") }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun cleanRepo(dryRun: Boolean = false) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                gitOperationUseCases.clean(path, dryRun)
            }.fold(
                onSuccess = { result ->
                    appendTerminalLog("git clean", result)
                    refreshWorkspace()
                },
                onFailure = { e ->
                    appendTerminalLog("git clean", "Error: ${e.message}")
                    _uiState.update { it.copy(error = "Clean failed: ${e.message}") }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
