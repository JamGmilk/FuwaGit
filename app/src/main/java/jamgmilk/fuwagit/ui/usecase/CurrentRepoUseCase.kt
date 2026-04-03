package jamgmilk.fuwagit.ui.usecase

import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.ui.state.RepoInfo
import jamgmilk.fuwagit.ui.state.RepoStateManager
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
                        repoPath = path,
                        repoName = name,
                        error = "Path does not exist"
                    )
                )
                repoDataStore.setCurrentRepo(null)
            }
            !hasGitDirUseCase(path) -> {
                repoStateManager.updateRepoInfo(
                    RepoInfo(
                        repoPath = path,
                        repoName = name,
                        error = "Not a git repository"
                    )
                )
                repoDataStore.setCurrentRepo(null)
            }
            else -> {
                repoStateManager.updateRepoInfo(
                    RepoInfo(
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