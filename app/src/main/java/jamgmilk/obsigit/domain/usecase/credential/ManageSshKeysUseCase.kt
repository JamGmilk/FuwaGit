package jamgmilk.obsigit.domain.usecase.credential

import jamgmilk.obsigit.credential.SshKeyInfo
import jamgmilk.obsigit.credential.SshKeyType
import jamgmilk.obsigit.domain.repository.CredentialRepository

class ManageSshKeysUseCase(
    private val credentialRepository: CredentialRepository
) {
    suspend fun generate(name: String, type: SshKeyType, comment: String): Result<SshKeyInfo> {
        return credentialRepository.generateSshKey(name, type, comment)
    }
    
    suspend fun import(
        name: String,
        privateKey: String,
        publicKey: String?,
        passphrase: String?
    ): Result<SshKeyInfo> {
        return credentialRepository.importSshKey(name, privateKey, publicKey, passphrase)
    }
    
    suspend fun importPublicKey(name: String, publicKey: String): Result<SshKeyInfo> {
        return credentialRepository.importPublicKey(name, publicKey)
    }
    
    suspend fun getAll(): Result<List<SshKeyInfo>> {
        return credentialRepository.getSshKeys()
    }
    
    suspend fun getById(id: String): Result<SshKeyInfo?> {
        return credentialRepository.getSshKeyById(id)
    }
    
    suspend fun delete(id: String): Result<Unit> {
        return credentialRepository.deleteSshKey(id)
    }
    
    suspend fun exportPublicKey(id: String): Result<String> {
        return credentialRepository.exportPublicKey(id)
    }
    
    suspend fun exportPrivateKey(id: String): Result<ByteArray> {
        return credentialRepository.exportPrivateKey(id)
    }
}
