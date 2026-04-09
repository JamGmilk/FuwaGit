package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.PullResult
import jamgmilk.fuwagit.domain.repository.RemoteRepository
import javax.inject.Inject

class PullUseCase @Inject constructor(
    private val repository: RemoteRepository
) {
    suspend operator fun invoke(repoPath: String, credentials: CloneCredential? = null): AppResult<PullResult> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.pull(repoPath, credentials)
    }
}
