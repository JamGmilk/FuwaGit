package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
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
    suspend fun merge(repoPath: String, branchName: String): AppResult<ConflictResult> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        if (branchName.isBlank()) return AppResult.Error(AppException.Validation("Branch name cannot be empty"))
        return repository.mergeBranch(repoPath, branchName)
    }

    suspend fun rebase(repoPath: String, branchName: String): AppResult<ConflictResult> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        if (branchName.isBlank()) return AppResult.Error(AppException.Validation("Branch name cannot be empty"))
        return repository.rebaseBranch(repoPath, branchName)
    }

    suspend fun continueRebase(repoPath: String): AppResult<String> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        return repository.continueRebase(repoPath)
    }

    suspend fun getConflicts(repoPath: String): AppResult<ConflictResult> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        return repository.getConflictStatus(repoPath)
    }

    suspend fun resolveConflict(repoPath: String, filePath: String): AppResult<Unit> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        if (filePath.isBlank()) return AppResult.Error(AppException.Validation("File path cannot be empty"))
        return repository.markConflictResolved(repoPath, filePath)
    }

    suspend fun abortRebase(repoPath: String): AppResult<String> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        return repository.abortRebase(repoPath)
    }
}
