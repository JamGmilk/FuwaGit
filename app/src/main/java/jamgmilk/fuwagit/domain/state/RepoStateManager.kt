package jamgmilk.fuwagit.domain.state

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
class RepoStateManager @Inject constructor() {

    private val _repoInfo = MutableStateFlow(RepoInfo())
    val repoInfo: StateFlow<RepoInfo> = _repoInfo.asStateFlow()

    private val _validationRequest = MutableSharedFlow<String?>()
    val validationRequest: SharedFlow<String?> = _validationRequest.asSharedFlow()

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
        _repoInfo.value = RepoInfo(
            state = RepoState.CHECKING,
            repoPath = path,
            repoName = path?.substringAfterLast("/")
        )
        _validationRequest.emit(path)
    }
}