package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.domain.model.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository

class GetHttpsPasswordUseCase(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(uuid: String): AppResult<String> {
        return repository.getHttpsPassword(uuid)
    }
}
