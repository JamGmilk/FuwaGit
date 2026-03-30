package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.GitRepoStatus
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class GetWorkspaceStatusUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<GitRepoStatus> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.getStatus(repoPath)
    }
}
