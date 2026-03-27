package jamgmilk.obsigit.domain.usecase.credential

import jamgmilk.obsigit.domain.model.HttpsCredential
import jamgmilk.obsigit.domain.repository.CredentialRepository

class GetCredentialsUseCase(
    private val credentialRepository: CredentialRepository
) {
    suspend fun getAll(): Result<List<HttpsCredential>> {
        return credentialRepository.getHttpsCredentials()
    }
    
    suspend fun getById(id: String): Result<HttpsCredential?> {
        return credentialRepository.getHttpsCredentialById(id)
    }
    
    suspend fun getByHost(host: String): Result<HttpsCredential?> {
        return credentialRepository.getHttpsCredentialByHost(host)
    }
    
    suspend fun delete(id: String): Result<Unit> {
        return credentialRepository.deleteHttpsCredential(id)
    }
    
    fun isKeyStoreAvailable(): Boolean {
        return credentialRepository.isKeyStoreAvailable()
    }
}
