package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.ConflictFileInfo
import jamgmilk.fuwagit.data.jgit.GitMergeDataSource
import jamgmilk.fuwagit.data.jgit.GitOperationCheckDataSource
import jamgmilk.fuwagit.data.jgit.PrePullCheckResult
import jamgmilk.fuwagit.data.jgit.PrePushCheckResult
import jamgmilk.fuwagit.domain.model.git.CleanResult
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.repository.MergeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MergeRepositoryImpl @Inject constructor(
    private val mergeDataSource: GitMergeDataSource,
    private val operationCheckDataSource: GitOperationCheckDataSource
) : MergeRepository {

    override suspend fun mergeBranch(repoPath: String, branchName: String): AppResult<ConflictResult> =
        withContext(Dispatchers.IO) {
            mergeDataSource.mergeBranch(repoPath, branchName).toAppResult()
        }

    override suspend fun rebaseBranch(repoPath: String, branchName: String): AppResult<ConflictResult> =
        withContext(Dispatchers.IO) {
            mergeDataSource.rebaseBranch(repoPath, branchName).toAppResult()
        }

    override suspend fun continueRebase(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            mergeDataSource.continueRebase(repoPath).toAppResult()
        }

    override suspend fun getConflictStatus(repoPath: String): AppResult<ConflictResult> =
        withContext(Dispatchers.IO) {
            mergeDataSource.getConflictStatus(repoPath).toAppResult()
        }

    override suspend fun markConflictResolved(repoPath: String, filePath: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            mergeDataSource.markConflictResolved(repoPath, filePath).toAppResult()
        }

    override suspend fun abortRebase(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            mergeDataSource.abortRebase(repoPath).toAppResult()
        }

    override suspend fun abortMerge(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            mergeDataSource.abortMerge(repoPath).toAppResult()
        }

    override suspend fun clean(repoPath: String, dryRun: Boolean): AppResult<CleanResult> =
        withContext(Dispatchers.IO) {
            mergeDataSource.clean(repoPath, dryRun).toAppResult()
        }

    override suspend fun checkPrePullStatus(repoPath: String): Result<PrePullCheckResult> =
        withContext(Dispatchers.IO) {
            operationCheckDataSource.checkPrePullStatus(repoPath)
        }

    override suspend fun checkPrePushStatus(repoPath: String): Result<PrePushCheckResult> =
        withContext(Dispatchers.IO) {
            operationCheckDataSource.checkPrePushStatus(repoPath)
        }

    override suspend fun isRepositoryLocked(repoPath: String): Boolean =
        withContext(Dispatchers.IO) {
            operationCheckDataSource.isRepositoryLocked(repoPath)
        }

    override suspend fun getConflictDetails(repoPath: String): Result<List<ConflictFileInfo>> =
        withContext(Dispatchers.IO) {
            operationCheckDataSource.getConflictDetails(repoPath)
        }
}