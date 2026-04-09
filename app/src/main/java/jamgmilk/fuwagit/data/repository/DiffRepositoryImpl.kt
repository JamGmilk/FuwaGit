package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.GitDiffDataSource
import jamgmilk.fuwagit.domain.model.git.FileDiff
import jamgmilk.fuwagit.domain.repository.DiffRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DiffRepositoryImpl @Inject constructor(
    private val diffDataSource: GitDiffDataSource
) : DiffRepository {

    override suspend fun getWorkingTreeDiff(repoPath: String, filePath: String): AppResult<FileDiff> =
        withContext(Dispatchers.IO) {
            diffDataSource.getWorkingTreeDiff(repoPath, filePath).toAppResult()
        }

    override suspend fun getStagedDiff(repoPath: String, filePath: String): AppResult<FileDiff> =
        withContext(Dispatchers.IO) {
            diffDataSource.getStagedDiff(repoPath, filePath).toAppResult()
        }

    override suspend fun getCommitFileDiff(
        repoPath: String,
        filePath: String,
        oldCommit: String,
        newCommit: String
    ): AppResult<FileDiff> =
        withContext(Dispatchers.IO) {
            diffDataSource.getCommitFileDiff(repoPath, filePath, oldCommit, newCommit).toAppResult()
        }

    override suspend fun getCommitDiff(
        repoPath: String,
        oldCommit: String,
        newCommit: String
    ): AppResult<List<FileDiff>> =
        withContext(Dispatchers.IO) {
            diffDataSource.getCommitDiff(repoPath, oldCommit, newCommit).toAppResult()
        }

    override suspend fun getFileContent(
        repoPath: String,
        filePath: String,
        commitHash: String?
    ): AppResult<String> =
        withContext(Dispatchers.IO) {
            diffDataSource.getFileContent(repoPath, filePath, commitHash).toAppResult()
        }
}