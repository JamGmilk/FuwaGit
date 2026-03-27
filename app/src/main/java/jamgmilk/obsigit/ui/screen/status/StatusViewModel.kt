package jamgmilk.obsigit.ui.screen.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jamgmilk.obsigit.domain.usecase.git.GetRepoStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StatusViewModel(
    private val getRepoStatusUseCase: GetRepoStatusUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StatusUiState())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()
    
    fun loadStatus(repoPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getRepoStatusUseCase(repoPath)
                .onSuccess { status ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            repoPath = repoPath,
                            branch = status.branch,
                            hasUncommittedChanges = status.hasUncommittedChanges,
                            untrackedCount = status.untrackedCount,
                            statusMessage = status.message
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
        }
    }
}
