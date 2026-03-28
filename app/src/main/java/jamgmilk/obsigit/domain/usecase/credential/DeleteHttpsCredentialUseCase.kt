package jamgmilk.obsigit.domain.usecase.credential

import jamgmilk.obsigit.domain.model.AppResult
import jamgmilk.obsigit.domain.repository.CredentialRepository

class DeleteHttpsCredentialUseCase(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(uuid: String): AppResult<Unit> {
        return repository.deleteHttpsCredential(uuid)
    }
}
