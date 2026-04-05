package jamgmilk.fuwagit.data.repository

import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.data.local.security.MasterKeyManager
import jamgmilk.fuwagit.domain.repository.BiometricRepository
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricRepositoryImpl @Inject constructor(
    private val masterKeyManager: MasterKeyManager
) : BiometricRepository {

    override fun enableBiometric(
        activity: FragmentActivity,
        masterKey: SecretKey,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        masterKeyManager.enableBiometric(
            activity = activity,
            masterKey = masterKey,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    override fun unlockWithBiometric(
        activity: FragmentActivity,
        onSuccess: (SecretKey) -> Unit,
        onError: (String) -> Unit
    ) {
        masterKeyManager.unlockWithBiometric(
            activity = activity,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    override fun isBiometricEnabled(): Boolean {
        return masterKeyManager.isBiometricEnabled()
    }

    override fun disableBiometric(): AppResult<Unit> {
        return AppResult.catching {
            masterKeyManager.disableBiometric()
            Unit
        }
    }

    override fun isMasterPasswordSet(): Boolean {
        return masterKeyManager.isMasterPasswordSet()
    }

    override fun getMasterPasswordHint(): String? {
        return masterKeyManager.getPasswordHint()
    }
}
