package jamgmilk.obsigit.data.repository

import android.content.Context
import jamgmilk.obsigit.credential.CredentialManager
import jamgmilk.obsigit.credential.SshKeyManager
import jamgmilk.obsigit.domain.model.HttpsCredential
import jamgmilk.obsigit.credential.SshKeyType
import jamgmilk.obsigit.credential.SshKeyInfo
import jamgmilk.obsigit.domain.repository.CredentialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CredentialRepositoryImpl(
    private val context: Context
) : CredentialRepository {
    
    private val credentialManager: CredentialManager by lazy { CredentialManager(context) }
    private val sshKeyManager: SshKeyManager by lazy { SshKeyManager(context) }
    
    // HTTPS Credentials
    override suspend fun saveHttpsCredential(credential: HttpsCredential): Result<Unit> = withContext(Dispatchers.IO) {
        credentialManager.saveCredential(
            jamgmilk.obsigit.credential.HttpsCredential(
                id = credential.id,
                host = credential.host,
                username = credential.username,
                password = credential.password,
                createdAt = credential.createdAt
            )
        )
    }
    
    override suspend fun getHttpsCredentials(): Result<List<HttpsCredential>> = withContext(Dispatchers.IO) {
        credentialManager.getAllCredentials().map { credentials ->
            credentials.map { cred ->
                HttpsCredential(
                    id = cred.id,
                    host = cred.host,
                    username = cred.username,
                    password = cred.password,
                    createdAt = cred.createdAt
                )
            }
        }
    }
    
    override suspend fun getHttpsCredentialById(id: String): Result<HttpsCredential?> = withContext(Dispatchers.IO) {
        credentialManager.getCredential(id).map { cred ->
            cred?.let {
                HttpsCredential(
                    id = it.id,
                    host = it.host,
                    username = it.username,
                    password = it.password,
                    createdAt = it.createdAt
                )
            }
        }
    }
    
    override suspend fun getHttpsCredentialByHost(host: String): Result<HttpsCredential?> = withContext(Dispatchers.IO) {
        credentialManager.getCredentialByHost(host).map { cred ->
            cred?.let {
                HttpsCredential(
                    id = it.id,
                    host = it.host,
                    username = it.username,
                    password = it.password,
                    createdAt = it.createdAt
                )
            }
        }
    }
    
    override suspend fun updateHttpsCredential(credential: HttpsCredential): Result<Unit> = withContext(Dispatchers.IO) {
        credentialManager.updateCredential(
            jamgmilk.obsigit.credential.HttpsCredential(
                id = credential.id,
                host = credential.host,
                username = credential.username,
                password = credential.password,
                createdAt = credential.createdAt
            )
        )
    }
    
    override suspend fun deleteHttpsCredential(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        credentialManager.deleteCredential(id)
    }
    
    // SSH Keys
    override suspend fun generateSshKey(name: String, type: SshKeyType, comment: String): Result<SshKeyInfo> = withContext(Dispatchers.IO) {
        sshKeyManager.generateKeyPair(name, type, comment)
    }
    
    override suspend fun importSshKey(
        name: String,
        privateKey: String,
        publicKey: String?,
        passphrase: String?
    ): Result<SshKeyInfo> = withContext(Dispatchers.IO) {
        sshKeyManager.importKeyPair(name, privateKey, publicKey, passphrase)
    }
    
    override suspend fun importPublicKey(name: String, publicKey: String): Result<SshKeyInfo> = withContext(Dispatchers.IO) {
        sshKeyManager.importPublicKey(name, publicKey)
    }
    
    override suspend fun getSshKeys(): Result<List<SshKeyInfo>> = withContext(Dispatchers.IO) {
        sshKeyManager.getAllKeys()
    }
    
    override suspend fun getSshKeyById(id: String): Result<SshKeyInfo?> = withContext(Dispatchers.IO) {
        sshKeyManager.getKey(id)
    }
    
    override suspend fun deleteSshKey(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        sshKeyManager.deleteKey(id)
    }
    
    override suspend fun exportPublicKey(id: String): Result<String> = withContext(Dispatchers.IO) {
        sshKeyManager.exportPublicKey(id)
    }
    
    override suspend fun exportPrivateKey(id: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        sshKeyManager.exportPrivateKey(id)
    }
    
    // KeyStore
    override fun isKeyStoreAvailable(): Boolean {
        return credentialManager.isKeyStoreAvailable()
    }
}
