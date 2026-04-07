package jamgmilk.fuwagit.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import jamgmilk.fuwagit.domain.model.git.GitResetMode
import jamgmilk.fuwagit.ui.state.RepoStateManager
import jamgmilk.fuwagit.domain.usecase.git.GetCommitFileChangesUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetCommitHistoryUseCase
import jamgmilk.fuwagit.domain.usecase.git.ResetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.Stable

@Stable
data class HistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val repoPath: String? = null,
    val commits: List<GitCommit> = emptyList(),
    // Reset 鐩稿叧鐘舵€?
    val pendingResetCommit: GitCommit? = null,
    val pendingResetMode: GitResetMode? = null,
    val isResetting: Boolean = false,
    // Commit 璇︽儏鐩稿叧鐘舵€?
    val selectedCommitDetail: GitCommitDetail? = null,
    val isLoadingCommitDetail: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val currentRepoManager: RepoStateManager,
    private val getCommitHistoryUseCase: GetCommitHistoryUseCase,
    private val resetUseCase: ResetUseCase,
    private val getCommitFileChangesUseCase: GetCommitFileChangesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var currentRepoPath: String? = null

    init {
        viewModelScope.launch {
            currentRepoManager.repoInfo.collectLatest { info ->
                currentRepoPath = info.repoPath
                _uiState.update { it.copy(repoPath = info.repoPath) }
                if (info.isValidGit) {
                    loadCommitHistory()
                } else {
                    _uiState.update { it.copy(commits = emptyList()) }
                }
            }
        }
    }

    fun loadCommitHistory() {
        val path = currentRepoPath
        if (path == null) {
            _uiState.update { it.copy(commits = emptyList()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getCommitHistoryUseCase(path)
                .onSuccess { commits ->
                    _uiState.update {
                        it.copy(
                            commits = commits,
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 璇锋眰 Reset 鎿嶄綔锛氳缃緟澶勭悊鐨?commit 鍜屾ā寮?
     */
    fun requestReset(commit: GitCommit, mode: GitResetMode) {
        _uiState.update {
            it.copy(
                pendingResetCommit = commit,
                pendingResetMode = mode
            )
        }
    }

    /**
     * 纭鎵ц Reset 鎿嶄綔
     */
    fun confirmReset() {
        val path = currentRepoPath ?: return
        val commit = _uiState.value.pendingResetCommit ?: return
        val mode = _uiState.value.pendingResetMode ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isResetting = true) }
            resetUseCase(path, commit.hash, mode)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isResetting = false,
                            pendingResetCommit = null,
                            pendingResetMode = null,
                            error = null
                        )
                    }
                    loadCommitHistory()
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            isResetting = false,
                            pendingResetCommit = null,
                            pendingResetMode = null,
                            error = e.message
                        )
                    }
                }
        }
    }

    /**
     * 鍙栨秷 Reset 鎿嶄綔
     */
    fun cancelReset() {
        _uiState.update {
            it.copy(
                pendingResetCommit = null,
                pendingResetMode = null
            )
        }
    }

    /**
     * 鍔犺浇 commit 璇︽儏锛堝寘鍚枃浠跺彉鏇村垪琛級
     */
    fun loadCommitDetail(commit: GitCommit) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCommitDetail = true) }
            getCommitFileChangesUseCase(path, commit.hash)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            selectedCommitDetail = detail,
                            isLoadingCommitDetail = false
                        )
                    }
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingCommitDetail = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    /**
     * 娓呴櫎 commit 璇︽儏
     */
    fun clearCommitDetail() {
        _uiState.update {
            it.copy(
                selectedCommitDetail = null
            )
        }
    }
}
