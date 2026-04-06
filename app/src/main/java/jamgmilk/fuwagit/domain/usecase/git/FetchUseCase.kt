package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class FetchUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, credentials: CloneCredential? = null): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.fetch(repoPath, credentials)
    }
}
