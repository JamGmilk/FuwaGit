package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.credential.store.HttpsCredential
import jamgmilk.fuwagit.domain.model.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository

class GetHttpsCredentialsUseCase(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(): AppResult<List<HttpsCredential>> {
        return repository.getAllHttpsCredentials()
    }
}
