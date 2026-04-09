package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.git.CleanResult
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.data.jgit.ConflictFileInfo
import jamgmilk.fuwagit.data.jgit.PrePullCheckResult
import jamgmilk.fuwagit.data.jgit.PrePushCheckResult

interface MergeRepository {
    suspend fun mergeBranch(repoPath: String, branchName: String): AppResult<ConflictResult>
    suspend fun rebaseBranch(repoPath: String, branchName: String): AppResult<ConflictResult>
    suspend fun continueRebase(repoPath: String): AppResult<String>
    suspend fun getConflictStatus(repoPath: String): AppResult<ConflictResult>
    suspend fun markConflictResolved(repoPath: String, filePath: String): AppResult<Unit>
    suspend fun abortRebase(repoPath: String): AppResult<String>
    suspend fun clean(repoPath: String, dryRun: Boolean): AppResult<CleanResult>
    suspend fun checkPrePullStatus(repoPath: String): Result<PrePullCheckResult>
    suspend fun checkPrePushStatus(repoPath: String): Result<PrePushCheckResult>
    suspend fun isRepositoryLocked(repoPath: String): Boolean
    suspend fun getConflictDetails(repoPath: String): Result<List<ConflictFileInfo>>
}