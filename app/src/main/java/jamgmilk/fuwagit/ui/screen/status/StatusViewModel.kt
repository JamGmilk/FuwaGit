package jamgmilk.fuwagit.ui.screen.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.data.source.RepoPathUtils
import jamgmilk.fuwagit.domain.model.GitBranch
import jamgmilk.fuwagit.domain.model.GitFileStatus
import jamgmilk.fuwagit.domain.model.GitRemote
import jamgmilk.fuwagit.domain.model.GitStash
import jamgmilk.fuwagit.domain.model.GitTag
import jamgmilk.fuwagit.domain.usecase.git.ApplyStashUseCase
import jamgmilk.fuwagit.domain.usecase.git.CheckRepoStatusUseCase
import jamgmilk.fuwagit.domain.usecase.git.CleanUseCase
import jamgmilk.fuwagit.domain.usecase.git.CommitChangesUseCase
import jamgmilk.fuwagit.domain.usecase.git.CreateTagUseCase
import jamgmilk.fuwagit.domain.usecase.git.DeleteRemoteUseCase
import jamgmilk.fuwagit.domain.usecase.git.DeleteTagUseCase
import jamgmilk.fuwagit.domain.usecase.git.DiscardChangesUseCase
import jamgmilk.fuwagit.domain.usecase.git.DropStashUseCase
import jamgmilk.fuwagit.domain.usecase.git.FetchUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetBranchesUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetRemotesUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetTagsUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetWorkspaceStatusUseCase
import jamgmilk.fuwagit.domain.usecase.git.InitRepoUseCase
import jamgmilk.fuwagit.domain.usecase.git.PullUseCase
import jamgmilk.fuwagit.domain.usecase.git.PushUseCase
import jamgmilk.fuwagit.domain.usecase.git.RenameBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.StageAllUseCase
import jamgmilk.fuwagit.domain.usecase.git.StageFileUseCase
import jamgmilk.fuwagit.domain.usecase.git.StashChangesUseCase
import jamgmilk.fuwagit.domain.usecase.git.StashListUseCase
import jamgmilk.fuwagit.domain.usecase.git.UnstageAllUseCase
import jamgmilk.fuwagit.domain.usecase.git.UnstageFileUseCase
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
    private val getWorkspaceStatusUseCase: GetWorkspaceStatusUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val stageAllUseCase: StageAllUseCase,
    private val unstageAllUseCase: UnstageAllUseCase,
    private val stageFileUseCase: StageFileUseCase,
    private val unstageFileUseCase: UnstageFileUseCase,
    private val commitChangesUseCase: CommitChangesUseCase,
    private val pullUseCase: PullUseCase,
    private val pushUseCase: PushUseCase,
    private val fetchUseCase: FetchUseCase,
    private val initRepoUseCase: InitRepoUseCase,
    private val discardChangesUseCase: DiscardChangesUseCase,
    private val checkRepoStatusUseCase: CheckRepoStatusUseCase,
    private val stashListUseCase: StashListUseCase,
    private val stashChangesUseCase: StashChangesUseCase,
    private val applyStashUseCase: ApplyStashUseCase,
    private val dropStashUseCase: DropStashUseCase,
    private val getTagsUseCase: GetTagsUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
    private val getRemotesUseCase: GetRemotesUseCase,
    private val deleteRemoteUseCase: DeleteRemoteUseCase,
    private val renameBranchUseCase: RenameBranchUseCase,
    private val cleanUseCase: CleanUseCase
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
            withContext(Dispatchers.IO) {
                checkRepoStatusUseCase(path)
            }.fold(
                onSuccess = { status ->
                    _uiState.update { 
                        it.copy(
                            isGitRepo = status.isGitRepo,
                            statusMessage = status.message
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { 
                        it.copy(
                            isGitRepo = false,
                            statusMessage = "Path: ${RepoPathUtils.shortDisplayPath(path)}\nError: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun refreshWorkspace() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val filesResult = withContext(Dispatchers.IO) { getWorkspaceStatusUseCase(path) }
            val branchesResult = withContext(Dispatchers.IO) { getBranchesUseCase(path) }
            
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
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] > $command\n$result"
        _uiState.update { it.copy(terminalOutput = it.terminalOutput + line) }
    }

    fun initRepo() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            initRepoUseCase(path).fold(
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
                stashListUseCase(path)
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
                stashChangesUseCase(path, message)
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
                applyStashUseCase(path, stashIndex, dropAfterApply)
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
                dropStashUseCase(path, stashIndex)
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
                getTagsUseCase(path)
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
                createTagUseCase(path, tagName, message)
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
                deleteTagUseCase(path, tagName)
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
                getRemotesUseCase(path)
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
                deleteRemoteUseCase(path, remoteName)
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
                renameBranchUseCase(path, oldName, newName)
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
                cleanUseCase(path, dryRun)
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
