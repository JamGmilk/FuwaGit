package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class DeleteRemoteUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, remoteName: String): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (remoteName.isBlank()) {
            return AppResult.Error(AppException.Validation("Remote name cannot be empty"))
        }
        return repository.deleteRemote(repoPath, remoteName)
    }
}
