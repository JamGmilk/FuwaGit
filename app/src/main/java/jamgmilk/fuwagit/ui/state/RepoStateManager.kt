package jamgmilk.fuwagit.ui.state

import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.domain.usecase.repo.ValidateRepoUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class RepoInfo(
    val repoPath: String? = null,
    val repoName: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isValidGit: Boolean get() = repoPath != null && error == null && !isLoading
    val isNotGit: Boolean get() = repoPath != null && error == "Not a git repository"
    val isPathInvalid: Boolean get() = error == "Path does not exist"
}

@Singleton
class RepoStateManager @Inject constructor(
    private val repoDataStore: RepoDataStore,
    private val validateRepoUseCase: ValidateRepoUseCase
) {
    private val _repoInfo = MutableStateFlow(RepoInfo())
    val repoInfo: StateFlow<RepoInfo> = _repoInfo.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            initializeFromStorage()
        }
    }

    private suspend fun initializeFromStorage() {
        val savedPath = repoDataStore.getCurrentRepoPath()
        if (savedPath != null) {
            validateRepo(savedPath)
        }
    }

    fun updateRepoInfo(info: RepoInfo) {
        _repoInfo.value = info
    }

    fun getRepoPath(): String? = _repoInfo.value.repoPath

    fun isRepoReady(): Boolean = _repoInfo.value.isValidGit

    fun clearRepo() {
        _repoInfo.value = RepoInfo()
        scope.launch {
            validateRepoUseCase(null)
        }
    }

    suspend fun setRepoPath(path: String?) {
        if (path == null) {
            clearRepo()
            return
        }
        validateRepo(path)
    }

    suspend fun validateRepo(path: String) {
        val file = File(path)
        val name = file.name

        _repoInfo.value = RepoInfo(
            repoPath = path,
            repoName = name,
            isLoading = true
        )

        val result = validateRepoUseCase(path)

        _repoInfo.value = when (result) {
            is ValidateRepoUseCase.ValidationResult.Success -> RepoInfo(
                repoPath = result.path,
                repoName = result.name
            )
            is ValidateRepoUseCase.ValidationResult.Error -> RepoInfo(
                repoPath = path,
                repoName = name,
                error = result.message
            )
            ValidateRepoUseCase.ValidationResult.Cleared -> RepoInfo()
        }
    }
}