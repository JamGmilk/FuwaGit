package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.repository.RemoteRepository
import javax.inject.Inject

class ConfigureRemoteUseCase @Inject constructor(
    private val repository: RemoteRepository
) {
    suspend operator fun invoke(localPath: String, name: String, url: String): AppResult<String> {
        if (localPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Local path cannot be empty"))
        }
        if (name.isBlank()) {
            return AppResult.Error(AppException.Validation("Remote name cannot be empty"))
        }
        if (url.isBlank()) {
            return AppResult.Error(AppException.Validation("Remote URL cannot be empty"))
        }
        return repository.configureRemote(localPath, name, url)
    }
}
