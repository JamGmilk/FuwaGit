package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.credential.store.HttpsCredential
import jamgmilk.fuwagit.credential.store.SshKey
import jamgmilk.fuwagit.domain.model.AppResult
import javax.crypto.SecretKey

interface CredentialRepository {

    suspend fun setupMasterPassword(password: String, hint: String?): AppResult<Unit>

    suspend fun unlockWithPassword(password: String): AppResult<SecretKey>

    suspend fun unlockWithBiometric(): AppResult<SecretKey>

    fun isMasterPasswordSet(): Boolean

    fun isBiometricEnabled(): Boolean

    fun getMasterPasswordHint(): String?

    fun isUnlocked(): Boolean

    fun lock()

    suspend fun getAllHttpsCredentials(): AppResult<List<HttpsCredential>>

    suspend fun addHttpsCredential(host: String, username: String, password: String): AppResult<String>

    suspend fun deleteHttpsCredential(uuid: String): AppResult<Unit>

    suspend fun getHttpsPassword(uuid: String): AppResult<String>

    suspend fun getAllSshKeys(): AppResult<List<SshKey>>

    suspend fun addSshKey(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String?,
        fingerprint: String,
        comment: String
    ): AppResult<String>

    suspend fun deleteSshKey(uuid: String): AppResult<Unit>

    suspend fun getSshPrivateKey(uuid: String): AppResult<String>

    suspend fun getSshPassphrase(uuid: String): AppResult<String?>
}
