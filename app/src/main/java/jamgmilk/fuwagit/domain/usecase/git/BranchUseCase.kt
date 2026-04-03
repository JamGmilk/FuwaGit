package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

/**
 * Facade for Git branch operations.
 * Aggregates branch operations to reduce UseCase count.
 */
class BranchUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend fun list(repoPath: String): Result<List<GitBranch>> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        return repository.getBranches(repoPath)
    }

    suspend fun create(repoPath: String, branchName: String): Result<Unit> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        if (branchName.isBlank()) return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        return repository.createBranch(repoPath, branchName)
    }

    suspend fun delete(repoPath: String, branchName: String, force: Boolean = false): Result<Unit> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        if (branchName.isBlank()) return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        return repository.deleteBranch(repoPath, branchName, force)
    }

    suspend fun rename(repoPath: String, oldName: String, newName: String): Result<String> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        if (oldName.isBlank() || newName.isBlank()) return Result.failure(IllegalArgumentException("Branch names cannot be empty"))
        return repository.renameBranch(repoPath, oldName, newName)
    }

    suspend fun checkout(repoPath: String, branchName: String): Result<Unit> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        if (branchName.isBlank()) return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        return repository.checkoutBranch(repoPath, branchName)
    }
}
