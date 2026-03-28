package jamgmilk.obsigit.domain.usecase.credential

import jamgmilk.obsigit.credential.store.HttpsCredential
import jamgmilk.obsigit.domain.model.AppResult
import jamgmilk.obsigit.domain.repository.CredentialRepository

class GetHttpsCredentialsUseCase(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(): AppResult<List<HttpsCredential>> {
        return repository.getAllHttpsCredentials()
    }
}
