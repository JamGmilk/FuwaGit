package jamgmilk.obsigit.domain.usecase.credential

import jamgmilk.obsigit.domain.model.HttpsCredential
import jamgmilk.obsigit.domain.repository.CredentialRepository

class SaveCredentialUseCase(
    private val credentialRepository: CredentialRepository
) {
    suspend operator fun invoke(credential: HttpsCredential): Result<Unit> {
        return credentialRepository.saveHttpsCredential(credential)
    }
    
    suspend fun update(credential: HttpsCredential): Result<Unit> {
        return credentialRepository.updateHttpsCredential(credential)
    }
}
