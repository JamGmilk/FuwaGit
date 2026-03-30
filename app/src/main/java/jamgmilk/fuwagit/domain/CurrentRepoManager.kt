package jamgmilk.fuwagit.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class CurrentRepoState {
    NO_REPO_SELECTED,
    CHECKING,
    REPO_PATH_INVALID,
    REPO_NOT_GIT,
    REPO_VALID
}

data class CurrentRepoInfo(
    val state: CurrentRepoState = CurrentRepoState.NO_REPO_SELECTED,
    val repoPath: String? = null,
    val repoName: String? = null,
    val errorMessage: String? = null
)

@Singleton
class CurrentRepoManager @Inject constructor() {

    private val _currentRepoInfo = MutableStateFlow(CurrentRepoInfo())
    val currentRepoInfo: StateFlow<CurrentRepoInfo> = _currentRepoInfo.asStateFlow()

    private val _validationRequest = MutableSharedFlow<String?>()
    val validationRequest: SharedFlow<String?> = _validationRequest.asSharedFlow()

    fun updateRepoInfo(info: CurrentRepoInfo) {
        _currentRepoInfo.value = info
    }

    fun getCurrentRepoPath(): String? = _currentRepoInfo.value.repoPath

    fun isRepoReady(): Boolean = _currentRepoInfo.value.state == CurrentRepoState.REPO_VALID

    fun clearCurrentRepo() {
        _currentRepoInfo.value = CurrentRepoInfo(
            state = CurrentRepoState.NO_REPO_SELECTED,
            repoPath = null,
            repoName = null
        )
    }

    suspend fun setCurrentRepoPath(path: String?) {
        _currentRepoInfo.value = CurrentRepoInfo(
            state = CurrentRepoState.CHECKING,
            repoPath = path,
            repoName = path?.substringAfterLast("/")
        )
        _validationRequest.emit(path)
    }
}