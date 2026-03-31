package jamgmilk.fuwagit.domain.state

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

enum class RepoState {
    NO_REPO_SELECTED,
    CHECKING,
    REPO_PATH_INVALID,
    REPO_NOT_GIT,
    REPO_VALID
}

data class RepoInfo(
    val state: RepoState = RepoState.NO_REPO_SELECTED,
    val repoPath: String? = null,
    val repoName: String? = null,
    val errorMessage: String? = null
)

@Singleton
class RepoStateManager @Inject constructor(
    private val repoDataStore: RepoDataStore
) {
    private val _repoInfo = MutableStateFlow(RepoInfo())
    val repoInfo: StateFlow<RepoInfo> = _repoInfo.asStateFlow()

    private val _validationRequest = MutableSharedFlow<String?>()
    val validationRequest: SharedFlow<String?> = _validationRequest.asSharedFlow()

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

    fun isRepoReady(): Boolean = _repoInfo.value.state == RepoState.REPO_VALID

    fun clearRepo() {
        _repoInfo.value = RepoInfo(
            state = RepoState.NO_REPO_SELECTED,
            repoPath = null,
            repoName = null
        )
    }

    suspend fun setRepoPath(path: String?) {
        if (path == null) {
            clearRepo()
            repoDataStore.setCurrentRepo(null)
            return
        }
        validateRepo(path)
    }

    suspend fun validateRepo(path: String) {
        val file = File(path)
        val name = file.name

        _repoInfo.value = RepoInfo(
            state = RepoState.CHECKING,
            repoPath = path,
            repoName = name
        )

        val isValidGit = file.exists() && hasGitDir(path)
        val state = when {
            !file.exists() -> RepoState.REPO_PATH_INVALID
            !isValidGit -> RepoState.REPO_NOT_GIT
            else -> RepoState.REPO_VALID
        }

        val errorMessage = when (state) {
            RepoState.REPO_PATH_INVALID -> "Path does not exist"
            RepoState.REPO_NOT_GIT -> "Not a git repository"
            else -> null
        }

        _repoInfo.value = RepoInfo(
            state = state,
            repoPath = path,
            repoName = name,
            errorMessage = errorMessage
        )

        if (state == RepoState.REPO_VALID) {
            repoDataStore.setCurrentRepo(path)
            repoDataStore.updateLastAccessed(path)
        } else {
            repoDataStore.setCurrentRepo(null)
        }
    }

    private fun hasGitDir(path: String): Boolean {
        return File(path, ".git").exists()
    }
}
