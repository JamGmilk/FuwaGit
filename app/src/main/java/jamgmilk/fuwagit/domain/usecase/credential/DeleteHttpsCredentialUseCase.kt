package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.domain.model.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository

class DeleteHttpsCredentialUseCase(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(uuid: String): AppResult<Unit> {
        return repository.deleteHttpsCredential(uuid)
    }
}
