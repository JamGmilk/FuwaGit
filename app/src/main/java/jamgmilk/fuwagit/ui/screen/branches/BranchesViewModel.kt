package jamgmilk.fuwagit.ui.screen.branches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.UiMessage
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.ui.components.OperationResult
import jamgmilk.fuwagit.domain.usecase.git.BranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.GitSyncFacade
import jamgmilk.fuwagit.domain.usecase.git.MergeUseCase
import jamgmilk.fuwagit.ui.state.RepoStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.Stable

@Stable
data class BranchesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val repoPath: String? = null,
    val localBranches: List<GitBranch> = emptyList(),
    val remoteBranches: List<GitBranch> = emptyList(),
    val currentBranch: GitBranch? = null,
    // 危险操作相关状态
    val pendingOperation: DangerousOperationType? = null,
    val pendingOperationTarget: String? = null,
    val operationResult: OperationResult? = null,
    // 冲突处理相关状态
    val conflictResult: ConflictResult? = null,
    val isResolvingConflict: Boolean = false
)

@HiltViewModel
class BranchesViewModel @Inject constructor(
    private val currentRepoManager: RepoStateManager,
    private val branchUseCase: BranchUseCase,
    private val mergeUseCase: MergeUseCase,
    private val gitSync: GitSyncFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchesUiState())
    val uiState: StateFlow<BranchesUiState> = _uiState.asStateFlow()

    private var currentRepoPath: String? = null

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

    // ============ 危险操作：请求确认 ============

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

    // ============ 危险操作：执行 ============

    fun confirmDeleteBranch(force: Boolean = false) {
        val path = currentRepoPath ?: return
        val branchName = _uiState.value.pendingOperationTarget ?: return

        viewModelScope.launch {
            branchUseCase.delete(path, branchName, force)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Success(UiMessage.Branch.Deleted(branchName)),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                    loadBranches()
                }
                .onError { e ->
                    val errorMsg = e.message ?: "Unknown error"
                    val causeMsg = e.cause?.message ?: errorMsg

                    val (userMessage, suggestion) = when {
                        errorMsg.contains("not fully merged", ignoreCase = true) ||
                        causeMsg.contains("not fully merged", ignoreCase = true) -> {
                            UiMessage.Branch.NotFullyMerged to UiMessage.Branch.NotFullyMerged
                        }
                        errorMsg.contains("cannot delete current branch", ignoreCase = true) ||
                        causeMsg.contains("cannot delete current branch", ignoreCase = true) -> {
                            UiMessage.Branch.CannotDeleteCurrentBranch to UiMessage.Branch.CannotDeleteCurrentBranch
                        }
                        else -> {
                            UiMessage.Generic(errorMsg) to UiMessage.Generic("Please try again or check the git logs for more details.")
                        }
                    }
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(userMessage, suggestion),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
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
                        // 有冲突，显示冲突解决 UI
                        _uiState.update {
                            it.copy(
                                conflictResult = result,
                                isResolvingConflict = true,
                                pendingOperation = null,
                                pendingOperationTarget = null
                            )
                        }
                    } else {
                        loadBranches()
                        _uiState.update {
                            it.copy(
                                operationResult = OperationResult.Success(UiMessage.Merge.Success(branchName)),
                                pendingOperation = null,
                                pendingOperationTarget = null,
                                conflictResult = null
                            )
                        }
                    }
                }
                .onError { e ->
                    val suggestion = when {
                        e.message?.contains("not fully merged") == true ->
                            UiMessage.Merge.NotFullyMerged
                        else -> UiMessage.Generic("Please try again or check the git logs for more details.")
                    }
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(UiMessage.Merge.ConflictHint, suggestion),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
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
                    } else {
                        loadBranches()
                        _uiState.update {
                            it.copy(
                                operationResult = OperationResult.Success(UiMessage.Rebase.Success(branchName)),
                                pendingOperation = null,
                                pendingOperationTarget = null,
                                conflictResult = null
                            )
                        }
                    }
                }
                .onError { e ->
                    val suggestion = when {
                        e.message?.contains("up to date") == true ->
                            UiMessage.Rebase.UpToDate
                        else -> UiMessage.Rebase.AbortHint
                    }
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(UiMessage.Rebase.ConflictHint, suggestion),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                }
        }
    }

    // ============ 常规操作 ============

    fun checkoutBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            branchUseCase.checkout(path, name)
                .onSuccess {
                    _uiState.update {
                        it.copy(operationResult = OperationResult.Success(UiMessage.Checkout.CheckoutSuccess(name)))
                    }
                    loadBranches()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun createBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            branchUseCase.create(path, name)
                .onSuccess {
                    _uiState.update {
                        it.copy(operationResult = OperationResult.Success(UiMessage.Create.BranchCreated(name)))
                    }
                    loadBranches()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun renameBranch(oldName: String, newName: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            branchUseCase.rename(path, oldName, newName)
                .onSuccess {
                    _uiState.update {
                        it.copy(operationResult = OperationResult.Success(UiMessage.Rename.RenameSuccess(newName)))
                    }
                    loadBranches()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    // ============ 冲突处理方法 ============

    /**
     * 检查当前是否有未解决的冲突
     */
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

    /**
     * 标记冲突文件为已解决
     */
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

    /**
     * 完成冲突解决（所有冲突都已解决后继续操作）
     */
    fun finishConflictResolution() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            // 检查是否所有冲突都已解决
            mergeUseCase.getConflicts(path)
                .onSuccess { result ->
                    if (result.allResolved || result.allStaged) {
                        _uiState.update {
                            it.copy(
                                isResolvingConflict = false,
                                conflictResult = null,
                                operationResult = OperationResult.Success(UiMessage.Conflict.Resolved)
                            )
                        }
                        loadBranches()
                    }
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    /**
     * 取消冲突解决
     */
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
                            conflictResult = null,
                            operationResult = OperationResult.Success(UiMessage.Generic(result))
                        )
                    }
                    loadBranches()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
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
                            conflictResult = null,
                            operationResult = OperationResult.Success(UiMessage.Generic(result))
                        )
                    }
                    loadBranches()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    // ============ 状态清理 ============

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
}
