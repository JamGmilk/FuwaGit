package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.domain.repository.GitRepository

class CreateBranchUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String, branchName: String): Result<Unit> {
        if (branchName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }
        return gitRepository.createBranch(repoPath, branchName.trim())
    }
}
