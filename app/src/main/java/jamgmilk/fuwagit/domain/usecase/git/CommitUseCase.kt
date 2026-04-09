package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.repository.CommitRepository
import javax.inject.Inject

class CommitUseCase @Inject constructor(
    private val repository: CommitRepository
) {
    suspend operator fun invoke(repoPath: String, message: String): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (message.isBlank()) {
            return AppResult.Error(AppException.Validation("Commit message cannot be empty"))
        }
        return repository.commit(repoPath, message)
    }
}
