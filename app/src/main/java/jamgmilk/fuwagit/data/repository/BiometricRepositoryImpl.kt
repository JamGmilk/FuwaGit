package jamgmilk.fuwagit.data.repository

import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.data.local.security.BiometricKeyManager
import jamgmilk.fuwagit.data.local.security.MasterKeyManager
import jamgmilk.fuwagit.domain.repository.BiometricRepository
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricRepositoryImpl @Inject constructor(
    private val masterKeyManager: MasterKeyManager,
    private val biometricKeyManager: BiometricKeyManager
) : BiometricRepository {

    override fun canAuthenticate(): Boolean {
        return biometricKeyManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun enableBiometric(
        activity: FragmentActivity,
        masterKey: SecretKey,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): AppResult<Unit> {
        return AppResult.catching {
            biometricKeyManager.createBiometricKey().getOrThrow()

            val encryptedKey = biometricKeyManager.encryptMasterKey(
                activity = activity,
                masterKey = masterKey,
                title = title,
                subtitle = subtitle,
                negativeButtonText = negativeButtonText
            ).getOrThrow()

            masterKeyManager.saveEncryptedMasterKey(encryptedKey)
            masterKeyManager.setBiometricEnabledInternal(true)
        }
    }

    override suspend fun unlockWithBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): AppResult<SecretKey> {
        val encryptedKey = masterKeyManager.getEncryptedMasterKey()
            ?: return AppResult.Error(jamgmilk.fuwagit.core.result.AppException.BiometricError("No encrypted key found"))

        return AppResult.catching {
            biometricKeyManager.decryptMasterKey(
                activity = activity,
                encryptedMasterKey = encryptedKey,
                title = title,
                subtitle = subtitle,
                negativeButtonText = negativeButtonText
            ).getOrThrow()
        }
    }

    override fun isBiometricEnabled(): Boolean {
        return masterKeyManager.isBiometricEnabled()
    }

    override suspend fun disableBiometric(): AppResult<Unit> {
        return AppResult.catching {
            biometricKeyManager.deleteBiometricKey()
            masterKeyManager.clearEncryptedMasterKey()
            masterKeyManager.setBiometricEnabledInternal(false)
        }
    }

    override fun isMasterPasswordSet(): Boolean {
        return masterKeyManager.isMasterPasswordSet()
    }

    override fun getMasterPasswordHint(): String? {
        return masterKeyManager.getPasswordHint()
    }
}
