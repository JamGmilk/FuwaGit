package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class RenameBranchUseCase @Inject constructor(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, oldName: String, newName: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (oldName.isBlank()) {
            return Result.failure(IllegalArgumentException("Old branch name cannot be empty"))
        }
        if (newName.isBlank()) {
            return Result.failure(IllegalArgumentException("New branch name cannot be empty"))
        }
        return gitRepository.renameBranch(repoPath, oldName, newName)
    }
}
