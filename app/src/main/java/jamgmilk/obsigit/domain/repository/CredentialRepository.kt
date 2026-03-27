package jamgmilk.obsigit.domain.repository

import jamgmilk.obsigit.credential.SshKeyInfo
import jamgmilk.obsigit.credential.SshKeyType
import jamgmilk.obsigit.domain.model.HttpsCredential

interface CredentialRepository {
    
    // HTTPS Credentials
    suspend fun saveHttpsCredential(credential: HttpsCredential): Result<Unit>
    
    suspend fun getHttpsCredentials(): Result<List<HttpsCredential>>
    
    suspend fun getHttpsCredentialById(id: String): Result<HttpsCredential?>
    
    suspend fun getHttpsCredentialByHost(host: String): Result<HttpsCredential?>
    
    suspend fun updateHttpsCredential(credential: HttpsCredential): Result<Unit>
    
    suspend fun deleteHttpsCredential(id: String): Result<Unit>
    
    // SSH Keys
    suspend fun generateSshKey(name: String, type: SshKeyType, comment: String): Result<SshKeyInfo>
    
    suspend fun importSshKey(
        name: String,
        privateKey: String,
        publicKey: String?,
        passphrase: String?
    ): Result<SshKeyInfo>
    
    suspend fun importPublicKey(name: String, publicKey: String): Result<SshKeyInfo>
    
    suspend fun getSshKeys(): Result<List<SshKeyInfo>>
    
    suspend fun getSshKeyById(id: String): Result<SshKeyInfo?>
    
    suspend fun deleteSshKey(id: String): Result<Unit>
    
    suspend fun exportPublicKey(id: String): Result<String>
    
    suspend fun exportPrivateKey(id: String): Result<ByteArray>
    
    // KeyStore
    fun isKeyStoreAvailable(): Boolean
}
