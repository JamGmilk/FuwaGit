package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class RebaseBranchUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, branchName: String): Result<ConflictResult> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (branchName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }
        return repository.rebaseBranch(repoPath, branchName)
    }
}
