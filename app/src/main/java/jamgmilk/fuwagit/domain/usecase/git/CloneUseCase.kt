package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.CloneCredential
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class CloneUseCase @Inject constructor(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(
        uri: String,
        localPath: String,
        branch: String? = null,
        credentials: CloneCredential? = null
    ): Result<String> {
        if (uri.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository URL cannot be empty"))
        }
        if (localPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Local path cannot be empty"))
        }
        return gitRepository.cloneRepository(uri, localPath, branch, credentials)
    }
}
