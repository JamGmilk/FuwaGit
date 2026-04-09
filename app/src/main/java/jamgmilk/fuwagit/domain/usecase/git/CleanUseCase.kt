package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.CleanResult
import jamgmilk.fuwagit.domain.repository.MergeRepository
import javax.inject.Inject

class CleanUseCase @Inject constructor(
    private val repository: MergeRepository
) {
    suspend operator fun invoke(repoPath: String, dryRun: Boolean = false): AppResult<CleanResult> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.clean(repoPath, dryRun)
    }
}
