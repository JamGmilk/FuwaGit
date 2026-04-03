package jamgmilk.fuwagit.ui.state

import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
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
    private val repoDataStore: RepoDataStore
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
            repoPath = path,
            repoName = name,
            isLoading = true
        )

        val error = when {
            !file.exists() -> "Path does not exist"
            !hasGitDir(path) -> "Not a git repository"
            else -> null
        }

        _repoInfo.value = RepoInfo(
            repoPath = path,
            repoName = name,
            isLoading = false,
            error = error
        )

        if (error == null) {
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