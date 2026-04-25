package jamgmilk.fuwagit.ui.screen.branches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.domain.usecase.git.BranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.MergeUseCase
import jamgmilk.fuwagit.ui.state.RepoStateManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.Stable

sealed class BranchUiEvent {
    data class DeleteSuccess(val branchName: String) : BranchUiEvent()
    data class DeleteError(val reason: String, val suggestion: String? = null) : BranchUiEvent()
    data class MergeSuccess(val branchName: String) : BranchUiEvent()
    data class MergeConflict(val hint: String) : BranchUiEvent()
    data class MergeError(val suggestion: String) : BranchUiEvent()
    data class RebaseSuccess(val branchName: String) : BranchUiEvent()
    data class RebaseConflict(val hint: String) : BranchUiEvent()
    data class RebaseError(val suggestion: String) : BranchUiEvent()
    data class CheckoutSuccess(val branchName: String) : BranchUiEvent()
    data class CreateBranchSuccess(val branchName: String) : BranchUiEvent()
    data class RenameSuccess(val newName: String) : BranchUiEvent()
    data class ConflictResolved(val message: String) : BranchUiEvent()
    data class AbortRebaseSuccess(val message: String) : BranchUiEvent()
    data class ContinueRebaseSuccess(val message: String) : BranchUiEvent()
    data class Error(val message: String) : BranchUiEvent()
}

@Stable
data class BranchesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val repoPath: String? = null,
    val localBranches: List<GitBranch> = emptyList(),
    val remoteBranches: List<GitBranch> = emptyList(),
    val currentBranch: GitBranch? = null,
    val pendingOperation: DangerousOperationType? = null,
    val pendingOperationTarget: String? = null,
    val conflictResult: ConflictResult? = null,
    val isResolvingConflict: Boolean = false
)

@HiltViewModel
class BranchesViewModel @Inject constructor(
    private val currentRepoManager: RepoStateManager,
    private val branchUseCase: BranchUseCase,
    private val mergeUseCase: MergeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchesUiState())
    val uiState: StateFlow<BranchesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BranchUiEvent>()
    val events: SharedFlow<BranchUiEvent> = _events.asSharedFlow()

    private var currentRepoPath: String? = null

    fun cancelPendingOperation() {
        _uiState.update {
            it.copy(
                pendingOperation = null,
                pendingOperationTarget = null
            )
        }
    }

    init {
        viewModelScope.launch {
            currentRepoManager.repoInfo.collectLatest { info ->
                currentRepoPath = info.repoPath
                _uiState.update { it.copy(repoPath = info.repoPath) }
                if (info.isValidGit) {
                    loadBranches()
                } else {
                    _uiState.update {
                        it.copy(
                            localBranches = emptyList(),
                            remoteBranches = emptyList(),
                            currentBranch = null
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            currentRepoManager.refreshEvents.collectLatest {
                loadBranches()
            }
        }
    }

    fun loadBranches() {
        val path = currentRepoPath
        if (path == null) {
            _uiState.update {
                it.copy(
                    localBranches = emptyList(),
                    remoteBranches = emptyList()
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            branchUseCase.list(path)
                .onSuccess { branches ->
                    val local = branches.filter { !it.isRemote }
                    val remote = branches.filter { it.isRemote }
                    val current = branches.find { it.isCurrent }

                    _uiState.update {
                        it.copy(
                            localBranches = local,
                            remoteBranches = remote,
                            currentBranch = current,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun requestDeleteBranch(name: String) {
        _uiState.update {
            it.copy(
                pendingOperation = DangerousOperationType.DELETE_BRANCH,
                pendingOperationTarget = name
            )
        }
    }

    fun requestMergeBranch(name: String) {
        _uiState.update {
            it.copy(
                pendingOperation = DangerousOperationType.MERGE,
                pendingOperationTarget = name
            )
        }
    }

    fun requestRebaseBranch(name: String) {
        _uiState.update {
            it.copy(
                pendingOperation = DangerousOperationType.REBASE,
                pendingOperationTarget = name
            )
        }
    }

    fun confirmDeleteBranch(force: Boolean = false) {
        val path = currentRepoPath ?: return
        val branchName = _uiState.value.pendingOperationTarget ?: return

        viewModelScope.launch {
            branchUseCase.delete(path, branchName, force)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                    _events.emit(BranchUiEvent.DeleteSuccess(branchName))
                    loadBranches()
                }
                .onError { e ->
                    val errorMsg = e.message ?: "Unknown error"
                    val causeMsg = e.cause?.message ?: errorMsg

                    val (userMessage, suggestion) = when {
                        errorMsg.contains("not fully merged", ignoreCase = true) ||
                        causeMsg.contains("not fully merged", ignoreCase = true) -> {
                            "The branch contains commits that haven't been merged" to "Use force delete to remove it anyway"
                        }
                        errorMsg.contains("cannot delete current branch", ignoreCase = true) ||
                        causeMsg.contains("cannot delete current branch", ignoreCase = true) -> {
                            "Cannot delete the currently checked out branch" to "Switch to another branch first"
                        }
                        else -> {
                            errorMsg to "Please try again or check the git logs for more details."
                        }
                    }
                    _uiState.update {
                        it.copy(
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                    _events.emit(BranchUiEvent.DeleteError(userMessage, suggestion))
                }
        }
    }

    fun confirmMergeBranch() {
        val path = currentRepoPath ?: return
        val branchName = _uiState.value.pendingOperationTarget ?: return

        viewModelScope.launch {
            mergeUseCase.merge(path, branchName)
                .onSuccess { result ->
                    if (result.isConflicting) {
                        _uiState.update {
                            it.copy(
                                conflictResult = result,
                                isResolvingConflict = true,
                                pendingOperation = null,
                                pendingOperationTarget = null
                            )
                        }
                        _events.emit(BranchUiEvent.MergeConflict("Resolve the conflicts manually in the Status screen, then commit the changes."))
                    } else {
                        loadBranches()
                        _uiState.update {
                            it.copy(
                                pendingOperation = null,
                                pendingOperationTarget = null,
                                conflictResult = null
                            )
                        }
                        _events.emit(BranchUiEvent.MergeSuccess(branchName))
                    }
                }
                .onError { e ->
                    val suggestion = when {
                        e.message?.contains("not fully merged") == true ->
                            "The branch contains unmerged commits. This is expected for a merge operation."
                        else -> "Please try again or check the git logs for more details."
                    }
                    _uiState.update {
                        it.copy(
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                    _events.emit(BranchUiEvent.MergeError(suggestion))
                }
        }
    }

    fun confirmRebaseBranch() {
        val path = currentRepoPath ?: return
        val branchName = _uiState.value.pendingOperationTarget ?: return

        viewModelScope.launch {
            mergeUseCase.rebase(path, branchName)
                .onSuccess { result ->
                    if (result.isConflicting) {
                        _uiState.update {
                            it.copy(
                                conflictResult = result,
                                isResolvingConflict = true,
                                pendingOperation = null,
                                pendingOperationTarget = null
                            )
                        }
                        _events.emit(BranchUiEvent.RebaseConflict("Resolve conflicts and run 'git rebase --continue', or 'git rebase --abort' to cancel."))
                    } else {
                        loadBranches()
                        _uiState.update {
                            it.copy(
                                pendingOperation = null,
                                pendingOperationTarget = null,
                                conflictResult = null
                            )
                        }
                        _events.emit(BranchUiEvent.RebaseSuccess(branchName))
                    }
                }
                .onError { e ->
                    val suggestion = when {
                        e.message?.contains("up to date") == true ->
                            "The current branch is already up to date with the target branch."
                        else -> "Run 'git rebase --abort' to cancel the rebase operation."
                    }
                    _uiState.update {
                        it.copy(
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                    _events.emit(BranchUiEvent.RebaseError(suggestion))
                }
        }
    }

    fun checkoutBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            branchUseCase.checkout(path, name)
                .onSuccess {
                    _events.emit(BranchUiEvent.CheckoutSuccess(name))
                    loadBranches()
                }
                .onError { e ->
                    _events.emit(BranchUiEvent.Error(e.message ?: "Failed to checkout branch"))
                }
        }
    }

    fun createBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            branchUseCase.create(path, name)
                .onSuccess {
                    _events.emit(BranchUiEvent.CreateBranchSuccess(name))
                    loadBranches()
                }
                .onError { e ->
                    _events.emit(BranchUiEvent.Error(e.message ?: "Failed to create branch"))
                }
        }
    }

    fun renameBranch(oldName: String, newName: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            branchUseCase.rename(path, oldName, newName)
                .onSuccess {
                    _events.emit(BranchUiEvent.RenameSuccess(newName))
                    loadBranches()
                }
                .onError { e ->
                    _events.emit(BranchUiEvent.Error(e.message ?: "Failed to rename branch"))
                }
        }
    }

    fun checkConflictStatus() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            mergeUseCase.getConflicts(path)
                .onSuccess { result ->
                    if (result.isConflicting) {
                        _uiState.update {
                            it.copy(
                                conflictResult = result,
                                isResolvingConflict = true
                            )
                        }
                    }
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun markConflictResolved(filePath: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            mergeUseCase.resolveConflict(path, filePath)
                .onSuccess {
                    // 重新获取冲突状态
                    checkConflictStatus()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun finishConflictResolution() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            mergeUseCase.getConflicts(path)
                .onSuccess { result ->
                    if (result.allResolved || result.allStaged) {
                        _uiState.update {
                            it.copy(
                                isResolvingConflict = false,
                                conflictResult = null
                            )
                        }
                        _events.emit(BranchUiEvent.ConflictResolved("Conflicts resolved"))
                        loadBranches()
                    }
                }
                .onError { e ->
                    _events.emit(BranchUiEvent.Error(e.message ?: "Failed to check conflict status"))
                }
        }
    }

    fun cancelConflictResolution() {
        _uiState.update {
            it.copy(
                isResolvingConflict = false,
                conflictResult = null
            )
        }
    }

    fun abortRebase() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            mergeUseCase.abortRebase(path)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isResolvingConflict = false,
                            conflictResult = null
                        )
                    }
                    _events.emit(BranchUiEvent.AbortRebaseSuccess(result))
                    loadBranches()
                }
                .onError { e ->
                    _events.emit(BranchUiEvent.Error(e.message ?: "Failed to abort rebase"))
                }
        }
    }

    fun continueRebase() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            mergeUseCase.continueRebase(path)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isResolvingConflict = false,
                            conflictResult = null
                        )
                    }
                    _events.emit(BranchUiEvent.ContinueRebaseSuccess(result))
                    loadBranches()
                }
                .onError { e ->
                    _events.emit(BranchUiEvent.Error(e.message ?: "Failed to continue rebase"))
                }
        }
    }
}
