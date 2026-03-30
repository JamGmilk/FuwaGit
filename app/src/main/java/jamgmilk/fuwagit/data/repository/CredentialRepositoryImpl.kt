package jamgmilk.fuwagit.data.repository

import android.content.Context
import jamgmilk.fuwagit.data.local.credential.HttpsCredential
import jamgmilk.fuwagit.data.local.credential.MasterKeyManager
import jamgmilk.fuwagit.data.local.credential.SecureCredentialStore
import jamgmilk.fuwagit.data.local.credential.SshKey
import jamgmilk.fuwagit.domain.model.AppException
import jamgmilk.fuwagit.domain.model.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.crypto.SecretKey

class CredentialRepositoryImpl(context: Context) : CredentialRepository {

    private val secureStore = SecureCredentialStore(context)
    private val masterKeyManager = MasterKeyManager(context)
    private var cachedMasterKey: SecretKey? = null

    override suspend fun setupMasterPassword(password: String, hint: String?): AppResult<Unit> {
        return AppResult.catching {
            val result = masterKeyManager.setupMasterPassword(password)
            if (result.isSuccess) {
                val key = result.getOrThrow()
                cachedMasterKey = key
                secureStore.cacheMasterKey(key)
                if (hint != null) {
                    secureStore.setMasterPasswordHint(hint)
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
        return secureStore.getMasterPasswordHint()
    }

    override fun isUnlocked(): Boolean {
        return cachedMasterKey != null || secureStore.getCachedMasterKey() != null
    }

    override fun lock() {
        cachedMasterKey = null
        secureStore.clearCachedMasterKey()
    }

    private fun getMasterKey(): SecretKey {
        return cachedMasterKey ?: secureStore.getCachedMasterKey()
            ?: throw AppException.MasterKeyNotUnlocked()
    }

    override suspend fun getAllHttpsCredentials(): AppResult<List<HttpsCredential>> {
        return AppResult.catching {
            secureStore.getPublicCredentials()
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
            secureStore.getHttpsPassword(uuid, key)
                ?: throw AppException.CredentialNotFound(uuid)
        }
    }

    override suspend fun getAllSshKeys(): AppResult<List<SshKey>> {
        return AppResult.catching {
            secureStore.getPublicSshKeys()
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
            secureStore.getSshPrivateKey(uuid, key)
                ?: throw AppException.CredentialNotFound(uuid)
        }
    }

    override suspend fun getSshPassphrase(uuid: String): AppResult<String?> {
        return AppResult.catching {
            val key = getMasterKey()
            secureStore.getSshPassphrase(uuid, key)
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
