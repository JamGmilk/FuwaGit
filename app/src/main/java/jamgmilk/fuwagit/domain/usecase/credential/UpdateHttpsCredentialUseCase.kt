package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.domain.model.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository

class UpdateHttpsCredentialUseCase(
    private val credentialRepository: CredentialRepository
) {
    suspend operator fun invoke(
        uuid: String,
        host: String? = null,
        username: String? = null,
        password: String? = null
    ): AppResult<Unit> {
        return credentialRepository.updateHttpsCredential(uuid, host, username, password)
    }
}
