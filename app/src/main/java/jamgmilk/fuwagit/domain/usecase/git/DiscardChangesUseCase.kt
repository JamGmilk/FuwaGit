package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class DiscardChangesUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, filePath: String): AppResult<Unit> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.discardChanges(repoPath, filePath)
    }
}
