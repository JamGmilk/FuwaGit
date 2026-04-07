package jamgmilk.fuwagit.ui.screen.filediff

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.git.FileDiff
import jamgmilk.fuwagit.domain.usecase.git.DiffUseCase
import jamgmilk.fuwagit.ui.state.RepoStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * File Diff UI 状态
 */
@Stable
data class FileDiffUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val fileDiff: FileDiff? = null,
    val repoPath: String? = null,
    val filePath: String? = null,
    val diffType: DiffType = DiffType.WORKING_TREE
)

/**
 * Diff 类型
 */
enum class DiffType {
    WORKING_TREE,  // 工作区 vs HEAD
    STAGED,        // 暂存区 vs HEAD
    COMMIT         // Commit vs Commit
}

@HiltViewModel
class FileDiffViewModel @Inject constructor(
    private val currentRepoManager: RepoStateManager,
    private val diffUseCase: DiffUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileDiffUiState())
    val uiState: StateFlow<FileDiffUiState> = _uiState.asStateFlow()

    private var currentRepoPath: String? = null

    init {
        // 从 SavedStateHandle 获取参数
        val filePath = savedStateHandle.get<String>("filePath")
        val diffTypeString = savedStateHandle.get<String>("diffType") ?: "WORKING_TREE"
        val oldCommit = savedStateHandle.get<String>("oldCommit")
        val newCommit = savedStateHandle.get<String>("newCommit")

        val diffType = DiffType.valueOf(diffTypeString)

        _uiState.update {
            it.copy(
                filePath = filePath,
                diffType = diffType
            )
        }

        viewModelScope.launch {
            currentRepoManager.repoInfo.collectLatest { info ->
                currentRepoPath = info.repoPath
                _uiState.update { it.copy(repoPath = info.repoPath) }
                val filePath = _uiState.value.filePath
                val diffType = _uiState.value.diffType
                val oldCommit = savedStateHandle.get<String>("oldCommit")
                val newCommit = savedStateHandle.get<String>("newCommit")
                if (info.isValidGit && filePath != null && info.repoPath != null) {
                    loadDiff(info.repoPath, filePath, diffType, oldCommit, newCommit)
                }
            }
        }
    }

    private fun loadDiff(
        repoPath: String,
        filePath: String,
        diffType: DiffType,
        oldCommit: String?,
        newCommit: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = when (diffType) {
                DiffType.WORKING_TREE -> diffUseCase.getWorkingTreeDiff(repoPath, filePath)
                DiffType.STAGED -> diffUseCase.getStagedDiff(repoPath, filePath)
                DiffType.COMMIT -> {
                    if (!oldCommit.isNullOrBlank() && !newCommit.isNullOrBlank()) {
                        diffUseCase.getCommitFileDiff(repoPath, filePath, oldCommit, newCommit)
                    } else {
                        jamgmilk.fuwagit.core.result.AppResult.Error(
                            jamgmilk.fuwagit.core.result.AppException.Validation("Commit hashes are required for commit diff")
                        )
                    }
                }
            }

            result
                .onSuccess { fileDiff ->
                    _uiState.update {
                        it.copy(
                            fileDiff = fileDiff,
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
}
