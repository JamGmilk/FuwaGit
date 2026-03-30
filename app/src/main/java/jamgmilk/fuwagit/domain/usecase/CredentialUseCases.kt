package jamgmilk.fuwagit.domain.usecase

import jamgmilk.fuwagit.data.local.credential.HttpsCredential
import jamgmilk.fuwagit.data.local.credential.SshKey
import jamgmilk.fuwagit.domain.model.AppException
import jamgmilk.fuwagit.domain.model.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialUseCases @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend fun setupMasterPassword(password: String, confirmPassword: String, hint: String?): AppResult<Unit> {
        if (password != confirmPassword) {
            return AppResult.Error(AppException.PasswordMismatch())
        }
        if (password.length < 6) {
            return AppResult.Error(AppException.Unknown("Password must be at least 6 characters"))
        }
        return repository.setupMasterPassword(password, hint)
    }

    suspend fun unlockWithPassword(password: String): AppResult<Unit> {
        return repository.unlockWithPassword(password)
    }

    suspend fun getHttpsCredentials(): AppResult<List<HttpsCredential>> {
        return repository.getAllHttpsCredentials()
    }

    suspend fun addHttpsCredential(host: String, username: String, password: String): AppResult<String> {
        return repository.addHttpsCredential(host, username, password)
    }

    suspend fun updateHttpsCredential(
        uuid: String,
        host: String? = null,
        username: String? = null,
        password: String? = null
    ): AppResult<Unit> {
        return repository.updateHttpsCredential(uuid, host, username, password)
    }

    suspend fun deleteHttpsCredential(uuid: String): AppResult<Unit> {
        return repository.deleteHttpsCredential(uuid)
    }

    suspend fun getHttpsPassword(uuid: String): AppResult<String> {
        return repository.getHttpsPassword(uuid)
    }

    suspend fun getSshKeys(): AppResult<List<SshKey>> {
        return repository.getAllSshKeys()
    }

    suspend fun addSshKey(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String?,
        fingerprint: String
    ): AppResult<String> {
        return repository.addSshKey(name, type, publicKey, privateKey, passphrase, fingerprint)
    }

    suspend fun deleteSshKey(uuid: String): AppResult<Unit> {
        return repository.deleteSshKey(uuid)
    }

    suspend fun getSshPrivateKey(uuid: String): AppResult<String> {
        return repository.getSshPrivateKey(uuid)
    }

    fun isMasterPasswordSet(): Boolean = repository.isMasterPasswordSet()

    fun isBiometricEnabled(): Boolean = repository.isBiometricEnabled()

    fun getMasterPasswordHint(): String? = repository.getMasterPasswordHint()

    fun isUnlocked(): Boolean = repository.isUnlocked()

    fun lock() = repository.lock()

    suspend fun exportCredentials(): AppResult<String> {
        return repository.exportCredentials()
    }

    suspend fun importCredentials(jsonData: String): AppResult<Unit> {
        return repository.importCredentials(jsonData)
    }

    suspend fun enableBiometric(): AppResult<Unit> {
        return repository.enableBiometric()
    }

    suspend fun disableBiometric(): AppResult<Unit> {
        return repository.disableBiometric()
    }
}
