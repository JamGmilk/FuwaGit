package jamgmilk.fuwagit.ui.state

import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.domain.usecase.repo.ValidateRepoUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
}

@Singleton
class RepoStateManager @Inject constructor(
    private val repoDataStore: RepoDataStore,
    private val validateRepoUseCase: ValidateRepoUseCase
) {
    private val _repoInfo = MutableStateFlow(RepoInfo())
    val repoInfo: StateFlow<RepoInfo> = _repoInfo.asStateFlow()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _refreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshEvents: SharedFlow<Unit> = _refreshEvents.asSharedFlow()

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

    fun getRepoPath(): String? = _repoInfo.value.repoPath

    suspend fun clearRepo() {
        _repoInfo.value = RepoInfo()
        validateRepoUseCase(null)
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
                error = result.message
            )
            ValidateRepoUseCase.ValidationResult.Cleared -> RepoInfo()
        }
    }

    suspend fun notifyRefresh() {
        _refreshEvents.emit(Unit)
    }
}