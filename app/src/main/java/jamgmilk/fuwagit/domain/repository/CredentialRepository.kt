package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.data.local.credential.HttpsCredential
import jamgmilk.fuwagit.data.local.credential.SshKey
import jamgmilk.fuwagit.core.result.AppResult
import javax.crypto.SecretKey

interface CredentialRepository {

    suspend fun setupMasterPassword(password: String, hint: String?): AppResult<Unit>

    suspend fun unlockWithPassword(password: String): AppResult<Unit>

    suspend fun unlockWithBiometric(): AppResult<Unit>

    fun isMasterPasswordSet(): Boolean

    fun isBiometricEnabled(): Boolean

    fun getMasterPasswordHint(): String?

    fun isUnlocked(): Boolean

    fun lock()

    fun getCachedMasterKey(): SecretKey?

    fun setMasterKey(key: SecretKey)

    suspend fun getAllHttpsCredentials(): AppResult<List<HttpsCredential>>

    suspend fun addHttpsCredential(host: String, username: String, password: String): AppResult<String>

    suspend fun updateHttpsCredential(uuid: String, host: String?, username: String?, password: String?): AppResult<Unit>

    suspend fun deleteHttpsCredential(uuid: String): AppResult<Unit>

    suspend fun getHttpsPassword(uuid: String): AppResult<String>

    suspend fun getAllSshKeys(): AppResult<List<SshKey>>

    suspend fun addSshKey(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String?,
        fingerprint: String
    ): AppResult<String>

    suspend fun deleteSshKey(uuid: String): AppResult<Unit>

    suspend fun getSshPrivateKey(uuid: String): AppResult<String>

    suspend fun getSshPassphrase(uuid: String): AppResult<String?>

    suspend fun exportCredentials(): AppResult<String>

    suspend fun importCredentials(jsonData: String): AppResult<Unit>

    suspend fun enableBiometric(): AppResult<Unit>

    suspend fun disableBiometric(): AppResult<Unit>
}
