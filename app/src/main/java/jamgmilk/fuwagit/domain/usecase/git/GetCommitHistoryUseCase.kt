package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.repository.CommitRepository
import javax.inject.Inject

class GetCommitHistoryUseCase @Inject constructor(
    private val repository: CommitRepository
) {
    suspend operator fun invoke(repoPath: String, maxCount: Int = 50, skip: Int = 0): AppResult<List<GitCommit>> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.getCommitHistory(repoPath, maxCount, skip)
    }
}
