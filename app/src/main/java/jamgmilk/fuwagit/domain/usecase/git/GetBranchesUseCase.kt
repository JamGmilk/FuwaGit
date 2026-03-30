package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class GetBranchesUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<List<GitBranch>> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.getBranches(repoPath)
    }
}
