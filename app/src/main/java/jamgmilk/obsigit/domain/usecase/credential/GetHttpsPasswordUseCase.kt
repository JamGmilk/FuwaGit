package jamgmilk.obsigit.domain.usecase.credential

import jamgmilk.obsigit.domain.model.AppResult
import jamgmilk.obsigit.domain.repository.CredentialRepository

class GetHttpsPasswordUseCase(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(uuid: String): AppResult<String> {
        return repository.getHttpsPassword(uuid)
    }
}
