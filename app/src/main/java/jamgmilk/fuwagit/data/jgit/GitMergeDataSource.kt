package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.CleanResult
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.git.GitConflict

/**
 * Interface for Git merge, rebase, conflict resolution, and clean operations.
 */
interface GitMergeDataSource {
    fun mergeBranch(repoPath: String, branchName: String): Result<ConflictResult>

    fun rebaseBranch(repoPath: String, branchName: String): Result<ConflictResult>

    fun getConflictStatus(repoPath: String): Result<ConflictResult>

    fun markConflictResolved(repoPath: String, filePath: String): Result<Unit>

    fun abortRebase(repoPath: String): Result<String>

    fun clean(repoPath: String, dryRun: Boolean): Result<CleanResult>
}
