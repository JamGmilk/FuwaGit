package jamgmilk.fuwagit.ui.screen.branches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.ui.components.OperationResult
import jamgmilk.fuwagit.domain.usecase.git.BranchUseCase
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
    // 鍗遍櫓鎿嶄綔鐩稿叧鐘舵€?
    val pendingOperation: DangerousOperationType? = null,
    val pendingOperationTarget: String? = null,
    val operationResult: OperationResult? = null,
    // 鍐茬獊澶勭悊鐩稿叧鐘舵€?
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

    // ============ 鍗遍櫓鎿嶄綔锛氳姹傜‘璁?============

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

    // ============ 鍗遍櫓鎿嶄綔锛氭墽琛?============

    fun confirmDeleteBranch(force: Boolean = false) {
        val path = currentRepoPath ?: return
        val branchName = _uiState.value.pendingOperationTarget ?: return

        viewModelScope.launch {
            branchUseCase.delete(path, branchName, force)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Success("Branch '$branchName' deleted successfully"),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                    loadBranches()
                }
                .onError { e ->
                    val suggestion = when {
                        e.message?.contains("not fully merged") == true ->
                            "The branch contains commits that haven't been merged. Use force delete to remove it anyway."
                        e.message?.contains("currently checked out") == true ->
                            "Cannot delete the currently checked out branch. Switch to another branch first."
                        else -> "Please try again or check the git logs for more details."
                    }
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(e.message ?: "Unknown error", suggestion),
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
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Success("Successfully merged '$branchName' into current branch"),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                    loadBranches()
                }
                .onError { e ->
                    val suggestion = when {
                        e.message?.contains("conflict") == true ->
                            "Resolve the conflicts manually in the Status screen, then commit the changes."
                        e.message?.contains("not fully merged") == true ->
                            "The branch contains unmerged commits. This is expected for a merge operation."
                        else -> "Check if the current branch is up to date and try again."
                    }
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(e.message ?: "Unknown error", suggestion),
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
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Success("Successfully rebased onto '$branchName'"),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                    loadBranches()
                }
                .onError { e ->
                    val suggestion = when {
                        e.message?.contains("conflict") == true ->
                            "Resolve conflicts and run 'git rebase --continue', or 'git rebase --abort' to cancel."
                        e.message?.contains("up to date") == true ->
                            "The current branch is already up to date with the target branch."
                        else -> "Run 'git rebase --abort' to cancel the rebase operation."
                    }
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(e.message ?: "Unknown error", suggestion),
                            pendingOperation = null,
                            pendingOperationTarget = null
                        )
                    }
                }
        }
    }

    // ============ 甯歌鎿嶄綔 ============

    fun checkoutBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            branchUseCase.checkout(path, name)
                .onSuccess {
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
                    loadBranches()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun deleteBranch(name: String, force: Boolean = false) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            branchUseCase.delete(path, name, force)
                .onSuccess {
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
                    loadBranches()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun mergeBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            mergeUseCase.merge(path, name)
                .onSuccess { result ->
                    if (result.isConflicting) {
                        // 鏈夊啿绐侊紝鏄剧ず鍐茬獊瑙ｅ喅 UI
                        _uiState.update {
                            it.copy(
                                conflictResult = result,
                                isResolvingConflict = true
                            )
                        }
                    } else {
                        // 鍚堝苟鎴愬姛
                        loadBranches()
                        _uiState.update {
                            it.copy(
                                operationResult = OperationResult.Success(result.message),
                                conflictResult = null
                            )
                        }
                    }
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun rebaseBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            mergeUseCase.rebase(path, name)
                .onSuccess { result ->
                    if (result.isConflicting) {
                        // 鏈夊啿绐侊紝鏄剧ず鍐茬獊瑙ｅ喅 UI
                        _uiState.update {
                            it.copy(
                                conflictResult = result,
                                isResolvingConflict = true
                            )
                        }
                    } else {
                        // Rebase 鎴愬姛
                        loadBranches()
                        _uiState.update {
                            it.copy(
                                operationResult = OperationResult.Success(result.message),
                                conflictResult = null
                            )
                        }
                    }
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    // ============ 鍐茬獊澶勭悊鏂规硶 ============

    /**
     * 妫€鏌ュ綋鍓嶆槸鍚︽湁鏈В鍐崇殑鍐茬獊
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
     * 鏍囪鍐茬獊鏂囦欢涓哄凡瑙ｅ喅
     */
    fun markConflictResolved(filePath: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            mergeUseCase.resolveConflict(path, filePath)
                .onSuccess {
                    // 閲嶆柊鑾峰彇鍐茬獊鐘舵€?
                    checkConflictStatus()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    /**
     * 瀹屾垚鍐茬獊瑙ｅ喅锛堟墍鏈夊啿绐侀兘宸茶В鍐冲悗缁х画鎿嶄綔锛?
     */
    fun finishConflictResolution() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            // 妫€鏌ユ槸鍚︽墍鏈夊啿绐侀兘宸茶В鍐?
            mergeUseCase.getConflicts(path)
                .onSuccess { result ->
                    if (result.allResolved || result.allStaged) {
                        // 鎵€鏈夊啿绐佸凡瑙ｅ喅锛屽彲浠ョ户缁?
                        _uiState.update {
                            it.copy(
                                isResolvingConflict = false,
                                conflictResult = null,
                                operationResult = OperationResult.Success("Conflicts resolved")
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
     * 鍙栨秷鍐茬獊瑙ｅ喅
     */
    fun cancelConflictResolution() {
        _uiState.update {
            it.copy(
                isResolvingConflict = false,
                conflictResult = null
            )
        }
    }

    /**
     * 鍙栨秷 Rebase 鎿嶄綔
     */
    fun abortRebase() {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            mergeUseCase.abortRebase(path)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isResolvingConflict = false,
                            conflictResult = null,
                            operationResult = OperationResult.Success(result)
                        )
                    }
                    loadBranches()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    // ============ 鐘舵€佹竻鐞?============

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
}
