package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.CloneOptions
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class CloneRepositoryUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(
        uri: String,
        localPath: String,
        credentials: CloneCredential? = null,
        options: CloneOptions = CloneOptions()
    ): Result<String> {
        if (uri.isBlank()) {
            return Result.failure(IllegalArgumentException("URI cannot be empty"))
        }
        if (localPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Local path cannot be empty"))
        }
        return repository.cloneRepository(uri, localPath, credentials, options)
    }
}
