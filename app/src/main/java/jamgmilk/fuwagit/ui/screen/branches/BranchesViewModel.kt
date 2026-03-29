package jamgmilk.fuwagit.ui.screen.branches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.GitBranch
import jamgmilk.fuwagit.domain.usecase.git.CheckoutBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.CreateBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.DeleteBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetBranchesUseCase
import jamgmilk.fuwagit.domain.usecase.git.MergeBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.RebaseBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.RenameBranchUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BranchesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val repoPath: String? = null,
    val branches: List<GitBranch> = emptyList(),
    val localBranches: List<GitBranch> = emptyList(),
    val remoteBranches: List<GitBranch> = emptyList(),
    val currentBranch: GitBranch? = null,
    val searchQuery: String = "",
    val selectedBranch: GitBranch? = null
)

@HiltViewModel
class BranchesViewModel @Inject constructor(
    private val getBranchesUseCase: GetBranchesUseCase,
    private val checkoutBranchUseCase: CheckoutBranchUseCase,
    private val createBranchUseCase: CreateBranchUseCase,
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val rebaseBranchUseCase: RebaseBranchUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val renameBranchUseCase: RenameBranchUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchesUiState())
    val uiState: StateFlow<BranchesUiState> = _uiState.asStateFlow()

    private var currentRepoPath: String? = null

    fun setRepoPath(path: String?) {
        currentRepoPath = path
        _uiState.update { it.copy(repoPath = path) }
        if (path != null) {
            loadBranches()
        } else {
            _uiState.update { 
                it.copy(
                    branches = emptyList(),
                    localBranches = emptyList(),
                    remoteBranches = emptyList(),
                    currentBranch = null
                )
            }
        }
    }

    fun loadBranches() {
        val path = currentRepoPath
        if (path == null) {
            _uiState.update { it.copy(branches = emptyList()) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            getBranchesUseCase(path)
                .onSuccess { branches ->
                    val local = branches.filter { !it.isRemote }
                    val remote = branches.filter { it.isRemote }
                    val current = branches.find { it.isCurrent }

                    _uiState.update {
                        it.copy(
                            branches = branches,
                            localBranches = local,
                            remoteBranches = remote,
                            currentBranch = current,
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

    fun checkoutBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            checkoutBranchUseCase(path, name)
                .onSuccess {
                    loadBranches()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun createBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            createBranchUseCase(path, name)
                .onSuccess {
                    loadBranches()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun deleteBranch(name: String, force: Boolean = false) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            deleteBranchUseCase(path, name, force)
                .onSuccess {
                    loadBranches()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun mergeBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            mergeBranchUseCase(path, name)
                .onSuccess {
                    loadBranches()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun rebaseBranch(name: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            rebaseBranchUseCase(path, name)
                .onSuccess {
                    loadBranches()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun renameBranch(oldName: String, newName: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            renameBranchUseCase(path, oldName, newName)
                .onSuccess {
                    loadBranches()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectBranch(branch: GitBranch?) {
        _uiState.update { it.copy(selectedBranch = branch) }
    }

    fun getFilteredBranches(): List<GitBranch> {
        val query = _uiState.value.searchQuery.trim().lowercase()
        if (query.isEmpty()) return _uiState.value.branches
        
        return _uiState.value.branches.filter { branch ->
            branch.name.lowercase().contains(query)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
