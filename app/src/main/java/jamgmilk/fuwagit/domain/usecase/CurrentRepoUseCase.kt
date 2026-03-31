package jamgmilk.fuwagit.domain.usecase

import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.domain.CurrentRepoInfo
import jamgmilk.fuwagit.domain.CurrentRepoManager
import jamgmilk.fuwagit.domain.CurrentRepoState
import jamgmilk.fuwagit.domain.usecase.git.HasGitDirUseCase
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentRepoUseCase @Inject constructor(
    private val repoDataStore: RepoDataStore,
    private val hasGitDirUseCase: HasGitDirUseCase,
    private val currentRepoManager: CurrentRepoManager
) {
    suspend fun validateAndSetCurrentRepo(path: String?) {
        if (path == null) {
            currentRepoManager.updateRepoInfo(
                CurrentRepoInfo(
                    state = CurrentRepoState.NO_REPO_SELECTED,
                    repoPath = null,
                    repoName = null
                )
            )
            repoDataStore.setCurrentRepo(null)
            return
        }

        val file = File(path)
        when {
            !file.exists() -> {
                currentRepoManager.updateRepoInfo(
                    CurrentRepoInfo(
                        state = CurrentRepoState.REPO_PATH_INVALID,
                        repoPath = path,
                        repoName = path.substringAfterLast("/"),
                        errorMessage = "Path does not exist"
                    )
                )
                repoDataStore.setCurrentRepo(null)
            }
            !hasGitDirUseCase(path) -> {
                currentRepoManager.updateRepoInfo(
                    CurrentRepoInfo(
                        state = CurrentRepoState.REPO_NOT_GIT,
                        repoPath = path,
                        repoName = path.substringAfterLast("/"),
                        errorMessage = "Not a git repository"
                    )
                )
                repoDataStore.setCurrentRepo(path)
            }
            else -> {
                currentRepoManager.updateRepoInfo(
                    CurrentRepoInfo(
                        state = CurrentRepoState.REPO_VALID,
                        repoPath = path,
                        repoName = path.substringAfterLast("/")
                    )
                )
                repoDataStore.setCurrentRepo(path)
                repoDataStore.updateLastAccessed(path)
            }
        }
    }

    suspend fun clearCurrentRepo() {
        currentRepoManager.clearCurrentRepo()
        repoDataStore.setCurrentRepo(null)
    }

    suspend fun initializeFromStorage(): String? {
        val savedPath = repoDataStore.getCurrentRepoPath()
        validateAndSetCurrentRepo(savedPath)
        return savedPath
    }
}