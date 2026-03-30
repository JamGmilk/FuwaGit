package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class DeleteBranchUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, branchName: String, force: Boolean = false): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (branchName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }
        return repository.deleteBranch(repoPath, branchName, force)
    }
}
