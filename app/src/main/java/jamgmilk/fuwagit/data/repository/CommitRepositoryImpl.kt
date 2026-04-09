package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.GitCommitDataSource
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import jamgmilk.fuwagit.domain.model.git.GitResetMode
import jamgmilk.fuwagit.domain.repository.CommitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CommitRepositoryImpl @Inject constructor(
    private val commitDataSource: GitCommitDataSource
) : CommitRepository {

    override suspend fun getCommitHistory(repoPath: String, maxCount: Int): AppResult<List<GitCommit>> =
        withContext(Dispatchers.IO) {
            commitDataSource.getLog(repoPath, maxCount).toAppResult()
        }

    override suspend fun getCommitFileChanges(repoPath: String, commitHash: String): AppResult<GitCommitDetail> =
        withContext(Dispatchers.IO) {
            commitDataSource.getCommitFileChanges(repoPath, commitHash).toAppResult()
        }

    override suspend fun commit(repoPath: String, message: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            commitDataSource.commit(repoPath, message).toAppResult()
        }

    override suspend fun reset(repoPath: String, commitHash: String, mode: GitResetMode): AppResult<String> =
        withContext(Dispatchers.IO) {
            commitDataSource.reset(repoPath, commitHash, mode).toAppResult()
        }
}