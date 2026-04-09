package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.GitStatusDataSource
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.model.git.GitRepoStatus
import jamgmilk.fuwagit.domain.repository.StatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StatusRepositoryImpl @Inject constructor(
    private val statusDataSource: GitStatusDataSource
) : StatusRepository {

    override suspend fun getRepoStatus(repoPath: String): AppResult<GitRepoStatus> =
        withContext(Dispatchers.IO) {
            statusDataSource.readRepoStatus(repoPath).toAppResult()
        }

    override suspend fun getDetailedStatus(repoPath: String): AppResult<List<GitFileStatus>> =
        withContext(Dispatchers.IO) {
            statusDataSource.getDetailedStatus(repoPath).toAppResult()
        }

    override suspend fun stageAll(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            statusDataSource.stageAll(repoPath).toAppResult()
        }

    override suspend fun unstageAll(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            statusDataSource.unstageAll(repoPath).toAppResult()
        }

    override suspend fun stageFile(repoPath: String, path: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            statusDataSource.stageFile(repoPath, path).toAppResult()
        }

    override suspend fun unstageFile(repoPath: String, path: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            statusDataSource.unstageFile(repoPath, path).toAppResult()
        }

    override suspend fun discardChanges(repoPath: String, path: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            statusDataSource.discardChanges(repoPath, path).toAppResult()
        }
}