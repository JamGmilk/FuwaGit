package jamgmilk.fuwagit.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.usecase.git.GetCommitHistoryUseCase
import jamgmilk.fuwagit.domain.state.RepoState
import jamgmilk.fuwagit.domain.state.RepoStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val repoPath: String? = null,
    val commits: List<GitCommit> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val currentRepoManager: RepoStateManager,
    private val getCommitHistoryUseCase: GetCommitHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var currentRepoPath: String? = null

    init {
        viewModelScope.launch {
            currentRepoManager.repoInfo.collectLatest { info ->
                currentRepoPath = info.repoPath
                _uiState.update { it.copy(repoPath = info.repoPath) }
                if (info.state == RepoState.REPO_VALID && info.repoPath != null) {
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
                .onFailure { e ->
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
}
