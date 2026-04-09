package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.repository.StatusRepository
import javax.inject.Inject

class GetDetailedStatusUseCase @Inject constructor(
    private val repository: StatusRepository
) {
    suspend operator fun invoke(repoPath: String): AppResult<List<GitFileStatus>> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.getDetailedStatus(repoPath)
    }
}
