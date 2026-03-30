package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class ConfigureRemoteUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(localPath: String, name: String, url: String): Result<String> {
        if (localPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Local path cannot be empty"))
        }
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Remote name cannot be empty"))
        }
        if (url.isBlank()) {
            return Result.failure(IllegalArgumentException("Remote URL cannot be empty"))
        }
        return repository.configureRemote(localPath, name, url)
    }
}
