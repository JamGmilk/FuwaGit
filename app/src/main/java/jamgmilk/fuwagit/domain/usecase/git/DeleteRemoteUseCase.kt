package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class DeleteRemoteUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, remoteName: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (remoteName.isBlank()) {
            return Result.failure(IllegalArgumentException("Remote name cannot be empty"))
        }
        return repository.deleteRemote(repoPath, remoteName)
    }
}
