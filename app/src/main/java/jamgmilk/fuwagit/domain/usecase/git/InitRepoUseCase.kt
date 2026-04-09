package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.repository.CoreRepository
import javax.inject.Inject

class InitRepoUseCase @Inject constructor(
    private val repository: CoreRepository
) {
    suspend operator fun invoke(repoPath: String): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Unknown("Repository path cannot be empty"))
        }
        return repository.initRepo(repoPath)
    }
}
