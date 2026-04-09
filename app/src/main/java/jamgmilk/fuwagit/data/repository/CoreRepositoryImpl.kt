package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.GitCoreDataSource
import jamgmilk.fuwagit.domain.repository.CoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CoreRepositoryImpl @Inject constructor(
    private val coreDataSource: GitCoreDataSource
) : CoreRepository {

    override suspend fun initRepo(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            coreDataSource.initRepo(repoPath).toAppResult()
        }

    override suspend fun hasGitDir(path: String?): Boolean =
        withContext(Dispatchers.IO) {
            coreDataSource.hasGitDir(path)
        }

    override suspend fun getRepoInfo(localPath: String): Map<String, String> =
        withContext(Dispatchers.IO) {
            coreDataSource.getRepoInfo(localPath)
        }
}