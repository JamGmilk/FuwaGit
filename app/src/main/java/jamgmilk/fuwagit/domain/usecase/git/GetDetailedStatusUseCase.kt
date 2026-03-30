package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class GetDetailedStatusUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<List<GitFileStatus>> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.getDetailedStatus(repoPath)
    }
}
