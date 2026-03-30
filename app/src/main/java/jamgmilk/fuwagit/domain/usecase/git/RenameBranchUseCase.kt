package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class RenameBranchUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, oldName: String, newName: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (oldName.isBlank() || newName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch names cannot be empty"))
        }
        return repository.renameBranch(repoPath, oldName, newName)
    }
}
