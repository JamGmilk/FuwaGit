package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.domain.model.repo.RepoData
import jamgmilk.fuwagit.domain.repository.RepoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepoRepositoryImpl @Inject constructor(
    private val repoDataStore: RepoDataStore
) : RepoRepository {

    override fun getSavedReposFlow(): Flow<List<RepoData>> {
        return repoDataStore.getSavedReposFlow()
    }

    override suspend fun getAllRepos(): List<RepoData> {
        return repoDataStore.getSavedRepos()
    }

    override suspend fun setCurrentRepo(path: String?) {
        repoDataStore.setCurrentRepo(path)
    }

    override suspend fun getCurrentRepoPath(): String? {
        return repoDataStore.getCurrentRepoPath()
    }

    override suspend fun addRepo(repo: RepoData): Boolean {
        return repoDataStore.addRepo(repo)
    }

    override suspend fun removeRepo(path: String): Boolean {
        return repoDataStore.removeRepo(path)
    }

    override suspend fun updateLastAccessed(path: String) {
        repoDataStore.updateLastAccessed(path)
    }

    override suspend fun toggleFavorite(path: String) {
        repoDataStore.toggleFavorite(path)
    }
}
