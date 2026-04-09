package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.data.local.credential.HttpsCredential as DataHttpsCredential
import jamgmilk.fuwagit.data.local.credential.SshKey as DataSshKey
import jamgmilk.fuwagit.data.local.security.MasterKeyManager
import jamgmilk.fuwagit.data.local.security.SecureCredentialStore
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialRepositoryImpl @Inject constructor(
    private val secureStore: SecureCredentialStore,
    private val masterKeyManager: MasterKeyManager
) : CredentialRepository {

    private var cachedMasterKey: SecretKey? = null

    override suspend fun setupMasterPassword(password: String, hint: String?): AppResult<Unit> {
        return AppResult.catching {
            val result = masterKeyManager.setupMasterPassword(password)
            if (result.isSuccess) {
                val key = result.getOrThrow()
                cachedMasterKey = key
                secureStore.cacheMasterKey(key)
                if (hint != null) {
                    masterKeyManager.setPasswordHint(hint)
                }
            } else {
                throw result.exceptionOrNull() ?: AppException.Unknown("Failed to setup password")
            }
        }
    }

    override suspend fun unlockWithPassword(password: String): AppResult<Unit> {
        return AppResult.catching {
            val result = masterKeyManager.unlockWithPassword(password)
            if (result.isSuccess) {
                val key = result.getOrThrow()
                cachedMasterKey = key
                secureStore.cacheMasterKey(key)
            } else {
                throw AppException.InvalidPassword()
            }
        }
    }

    override suspend fun unlockWithBiometric(): AppResult<Unit> {
        return AppResult.Error(AppException.BiometricNotEnabled())
    }

    override fun isMasterPasswordSet(): Boolean {
        return masterKeyManager.isMasterPasswordSet()
    }

    override fun isBiometricEnabled(): Boolean {
        return masterKeyManager.isBiometricEnabled()
    }

    override fun getMasterPasswordHint(): String? {
        return masterKeyManager.getPasswordHint()
    }

    override suspend fun isUnlocked(): Boolean {
        return cachedMasterKey != null || secureStore.getCachedMasterKey() != null
    }

    override fun lock() {
        cachedMasterKey = null
        secureStore.clearCachedMasterKey()
    }

    override suspend fun getCachedMasterKey(): SecretKey? {
        return cachedMasterKey ?: secureStore.getCachedMasterKey()
    }

    override fun setMasterKey(key: SecretKey) {
        cachedMasterKey = key
        secureStore.cacheMasterKey(key)
    }

    override fun setMasterKeyFromBiometric(key: SecretKey) {
        cachedMasterKey = key
        secureStore.cacheMasterKeyFromBiometric(key)
    }

    private suspend fun getMasterKey(): SecretKey {
        return cachedMasterKey ?: secureStore.getCachedMasterKey()
            ?: throw AppException.MasterKeyNotUnlocked()
    }

    override suspend fun getAllHttpsCredentials(): AppResult<List<HttpsCredential>> {
        return AppResult.catching {
            secureStore.getPublicCredentials().map { (it as DataHttpsCredential).toDomain() }
        }
    }

    override suspend fun addHttpsCredential(host: String, username: String, password: String): AppResult<String> {
        return AppResult.catching {
            val key = getMasterKey()
            secureStore.addHttpsCredential(host, username, password, key)
        }
    }

    override suspend fun updateHttpsCredential(uuid: String, host: String?, username: String?, password: String?): AppResult<Unit> {
        return AppResult.catching {
            val key = getMasterKey()
            secureStore.updateHttpsCredential(uuid, host, username, password, key)
        }
    }

    override suspend fun deleteHttpsCredential(uuid: String): AppResult<Unit> {
        return AppResult.catching {
            secureStore.deleteHttpsCredential(uuid)
        }
    }

    override suspend fun getHttpsPassword(uuid: String): AppResult<String> {
        return AppResult.catching {
            val key = getMasterKey()
            val result = secureStore.getHttpsPassword(uuid, key)
            if (result == null) {
                val exists = secureStore.getPublicCredentials().any { it.uuid == uuid }
                if (exists) throw AppException.DecryptionFailed()
                else throw AppException.CredentialNotFound(uuid)
            }
            result
        }
    }

    override suspend fun getAllSshKeys(): AppResult<List<SshKey>> {
        return AppResult.catching {
            secureStore.getPublicSshKeys().map { (it as DataSshKey).toDomain() }
        }
    }

    override suspend fun addSshKey(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String?,
        fingerprint: String
    ): AppResult<String> {
        return AppResult.catching {
            val key = getMasterKey()
            secureStore.addSshKey(
                name = name,
                type = type,
                publicKey = publicKey,
                privateKey = privateKey,
                passphrase = passphrase,
                fingerprint = fingerprint,
                masterKey = key
            )
        }
    }

    override suspend fun deleteSshKey(uuid: String): AppResult<Unit> {
        return AppResult.catching {
            secureStore.deleteSshKey(uuid)
        }
    }

    override suspend fun getSshPrivateKey(uuid: String): AppResult<String> {
        return AppResult.catching {
            val key = getMasterKey()
            val result = secureStore.getSshPrivateKey(uuid, key)
            if (result == null) {
                val exists = secureStore.getPublicSshKeys().any { it.uuid == uuid }
                if (exists) throw AppException.DecryptionFailed()
                else throw AppException.CredentialNotFound(uuid)
            }
            result
        }
    }

    override suspend fun getSshPassphrase(uuid: String): AppResult<String?> {
        return AppResult.catching {
            val key = getMasterKey()
            val result = secureStore.getSshPassphrase(uuid, key)
            if (result == null) {
                // If it's null, check if it was supposed to be there
                val keyData = secureStore.getPublicSshKeys().find { it.uuid == uuid }
                if (keyData?.passphrase != null) {
                    throw AppException.DecryptionFailed()
                }
            }
            result
        }
    }

    override suspend fun exportCredentials(): AppResult<String> {
        return AppResult.catching {
            val key = getMasterKey()
            secureStore.exportAllCredentials(key)
        }
    }

    override suspend fun importCredentials(jsonData: String): AppResult<Unit> {
        return AppResult.catching {
            val key = getMasterKey()
            secureStore.importAllCredentials(jsonData, key)
        }
    }

    override suspend fun enableBiometric(): AppResult<Unit> {
        return AppResult.Error(AppException.BiometricNotEnabled())
    }

    override suspend fun disableBiometric(): AppResult<Unit> {
        return AppResult.catching {
            masterKeyManager.disableBiometric()
        }
    }
}
