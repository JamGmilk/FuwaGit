package jamgmilk.fuwagit.ui.state

import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
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

private sealed interface ValidationResult {
    data class Success(val path: String, val name: String) : ValidationResult
    data class Error(val message: String) : ValidationResult
}

@Singleton
class RepoStateManager @Inject constructor(
    private val repoDataStore: RepoDataStore
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

    private fun initializeFromStorage() {
        repoDataStore.getCurrentRepoPath()?.let { validateRepo(it) }
    }

    fun getRepoPath(): String? = _repoInfo.value.repoPath

    fun clearRepo() {
        _repoInfo.value = RepoInfo()
        repoDataStore.setCurrentRepo(null)
    }

    fun setRepoPath(path: String?) {
        if (path == null) {
            clearRepo()
        } else {
            validateRepo(path)
        }
    }

    fun validateRepo(path: String) {
        val file = File(path)
        val name = file.name

        _repoInfo.value = RepoInfo(
            repoPath = path,
            repoName = name,
            isLoading = true
        )

        scope.launch(Dispatchers.IO) {
            val result = when {
                !file.exists() -> {
                    repoDataStore.setCurrentRepo(null)
                    ValidationResult.Error("Path does not exist")
                }
                !File(file, ".git").exists() -> {
                    repoDataStore.setCurrentRepo(null)
                    ValidationResult.Error("Not a git repository")
                }
                else -> {
                    repoDataStore.setCurrentRepo(path)
                    repoDataStore.updateLastAccessed(path)
                    ValidationResult.Success(path, name)
                }
            }

            _repoInfo.value = when (result) {
                is ValidationResult.Success -> RepoInfo(
                    repoPath = result.path,
                    repoName = result.name
                )
                is ValidationResult.Error -> RepoInfo(
                    error = result.message
                )
            }
        }
    }

    suspend fun notifyRefresh() {
        _refreshEvents.emit(Unit)
    }
}