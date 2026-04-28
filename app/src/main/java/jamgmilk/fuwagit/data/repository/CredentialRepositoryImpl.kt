package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
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

    override suspend fun setupMasterPassword(password: String, hint: String?): AppResult<Unit> {
        return AppResult.catching {
            val result = masterKeyManager.setupMasterPassword(password)
            if (result.isSuccess) {
                val key = result.getOrThrow()
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
                secureStore.cacheMasterKey(key)
            } else {
                throw AppException.InvalidPassword()
            }
        }
    }

    override fun isMasterPasswordSet(): Boolean = masterKeyManager.isMasterPasswordSet()

    override fun isBiometricEnabled(): Boolean = masterKeyManager.isBiometricEnabled()

    override fun getMasterPasswordHint(): String? = masterKeyManager.getPasswordHint()

    override suspend fun isUnlocked(): Boolean = getCachedMasterKey() != null

    override fun lock() = secureStore.clearCachedMasterKey()

    override suspend fun getCachedMasterKey(): SecretKey? = secureStore.getCachedMasterKey()

    override fun setMasterKey(key: SecretKey) = secureStore.cacheMasterKey(key)

    override fun setMasterKeyFromBiometric(key: SecretKey) = secureStore.cacheMasterKeyFromBiometric(key)

    private fun getMasterKey(): SecretKey =
        secureStore.getCachedMasterKey() ?: throw AppException.MasterKeyNotUnlocked()

    private suspend fun <T> getCredentialOrThrow(
        credentialId: String,
        fetchOperation: suspend () -> T?,
        existsCheck: suspend () -> Boolean
    ): T {
        val result = fetchOperation()
        if (result != null) return result
        throw if (existsCheck()) AppException.DecryptionFailed()
               else AppException.CredentialNotFound(credentialId)
    }

    override suspend fun getAllHttpsCredentials(): AppResult<List<HttpsCredential>> {
        return AppResult.catching {
            secureStore.getPublicCredentials().map { it.toDomain() }
        }
    }

    override suspend fun addHttpsCredential(host: String, username: String, password: String): AppResult<String> {
        return AppResult.catching {
            val key = getMasterKey()
            secureStore.addHttpsCredential(host, username, password, key)
        }
    }

    override suspend fun updateHttpsCredential(
        uuid: String, host: String?, username: String?, password: String?
    ): AppResult<Unit> {
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
            getCredentialOrThrow(
                credentialId = uuid,
                fetchOperation = { getMasterKey().let { key -> secureStore.getHttpsPassword(uuid, key) } },
                existsCheck = { secureStore.getPublicCredentials().any { it.uuid == uuid } }
            )
        }
    }

    override suspend fun getAllSshKeys(): AppResult<List<SshKey>> {
        return AppResult.catching {
            secureStore.getPublicSshKeys().map { it.toDomain() }
        }
    }

    override suspend fun addSshKey(
        name: String, type: String, publicKey: String, privateKey: String,
        passphrase: String?, fingerprint: String
    ): AppResult<String> {
        return AppResult.catching {
            val key = getMasterKey()
            secureStore.addSshKey(name, type, publicKey, privateKey, passphrase, fingerprint, key)
        }
    }

    override suspend fun deleteSshKey(uuid: String): AppResult<Unit> {
        return AppResult.catching {
            secureStore.deleteSshKey(uuid)
        }
    }

    override suspend fun getSshPrivateKey(uuid: String): AppResult<String> {
        return AppResult.catching {
            getCredentialOrThrow(
                credentialId = uuid,
                fetchOperation = { getMasterKey().let { key -> secureStore.getSshPrivateKey(uuid, key) } },
                existsCheck = { secureStore.getPublicSshKeys().any { it.uuid == uuid } }
            )
        }
    }

    override suspend fun getSshPassphrase(uuid: String): AppResult<String?> {
        return AppResult.catching {
            getMasterKey().let { key ->
                val result = secureStore.getSshPassphrase(uuid, key)
                if (result == null) {
                    val keyData = secureStore.getPublicSshKeys().find { it.uuid == uuid }
                    if (keyData?.passphrase != null) {
                        throw AppException.DecryptionFailed()
                    }
                }
                result
            }
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

    override suspend fun changeMasterPassword(oldPassword: String, newPassword: String, hint: String?): AppResult<Unit> {
        return AppResult.catching {
            masterKeyManager.changeMasterPassword(oldPassword, newPassword).getOrThrow()
            if (hint != null) {
                masterKeyManager.setPasswordHint(hint)
            }
        }
    }
}
