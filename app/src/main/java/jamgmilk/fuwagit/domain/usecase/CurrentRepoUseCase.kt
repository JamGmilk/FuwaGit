package jamgmilk.fuwagit.domain.usecase

import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.domain.state.RepoInfo
import jamgmilk.fuwagit.domain.state.RepoState
import jamgmilk.fuwagit.domain.state.RepoStateManager
import jamgmilk.fuwagit.domain.usecase.git.HasGitDirUseCase
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentRepoUseCase @Inject constructor(
    private val repoDataStore: RepoDataStore,
    private val hasGitDirUseCase: HasGitDirUseCase,
    private val repoStateManager: RepoStateManager
) {
    suspend fun validateAndSetCurrentRepo(path: String?) {
        if (path == null) {
            repoStateManager.clearRepo()
            repoDataStore.setCurrentRepo(null)
            return
        }

        val file = File(path)
        val name = file.name
        
        when {
            !file.exists() -> {
                repoStateManager.updateRepoInfo(
                    RepoInfo(
                        state = RepoState.REPO_PATH_INVALID,
                        repoPath = path,
                        repoName = name,
                        errorMessage = "Path does not exist"
                    )
                )
                repoDataStore.setCurrentRepo(null)
            }
            !hasGitDirUseCase(path) -> {
                repoStateManager.updateRepoInfo(
                    RepoInfo(
                        state = RepoState.REPO_NOT_GIT,
                        repoPath = path,
                        repoName = name,
                        errorMessage = "Not a git repository"
                    )
                )
                // Do not set as current repo in store if it's not a valid git repo
                repoDataStore.setCurrentRepo(null)
            }
            else -> {
                repoStateManager.updateRepoInfo(
                    RepoInfo(
                        state = RepoState.REPO_VALID,
                        repoPath = path,
                        repoName = name
                    )
                )
                repoDataStore.setCurrentRepo(path)
                repoDataStore.updateLastAccessed(path)
            }
        }
    }

    suspend fun clearCurrentRepo() {
        repoStateManager.clearRepo()
        repoDataStore.setCurrentRepo(null)
    }

    suspend fun initializeFromStorage(): String? {
        val savedPath = repoDataStore.getCurrentRepoPath()
        validateAndSetCurrentRepo(savedPath)
        return savedPath
    }
}