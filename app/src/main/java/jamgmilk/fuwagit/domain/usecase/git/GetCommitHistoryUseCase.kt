package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class GetCommitHistoryUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, maxCount: Int = 100): Result<List<GitCommit>> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.getCommitHistory(repoPath, maxCount)
    }
}
