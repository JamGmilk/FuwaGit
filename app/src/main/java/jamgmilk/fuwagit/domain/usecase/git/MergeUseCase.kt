package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

/**
 * Facade for Git merge/rebase operations.
 * Aggregates merge/rebase/conflict operations to reduce UseCase count.
 */
class MergeUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend fun merge(repoPath: String, branchName: String): Result<ConflictResult> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        if (branchName.isBlank()) return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        return repository.mergeBranch(repoPath, branchName)
    }

    suspend fun rebase(repoPath: String, branchName: String): Result<ConflictResult> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        if (branchName.isBlank()) return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        return repository.rebaseBranch(repoPath, branchName)
    }

    suspend fun getConflicts(repoPath: String): Result<ConflictResult> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        return repository.getConflictStatus(repoPath)
    }

    suspend fun resolveConflict(repoPath: String, filePath: String): Result<Unit> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        if (filePath.isBlank()) return Result.failure(IllegalArgumentException("File path cannot be empty"))
        return repository.markConflictResolved(repoPath, filePath)
    }

    suspend fun abortRebase(repoPath: String): Result<String> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        return repository.abortRebase(repoPath)
    }
}
