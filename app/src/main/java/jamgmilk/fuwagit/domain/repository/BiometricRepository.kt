package jamgmilk.fuwagit.domain.repository

import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.result.AppResult
import javax.crypto.SecretKey

/**
 * Domain interface for biometric authentication operations.
 * Extracted to prevent Domain → Data layer dependency violations.
 */
interface BiometricRepository {
    /**
     * Enable biometric authentication by encrypting the master key.
     */
    fun enableBiometric(
        activity: FragmentActivity,
        masterKey: SecretKey,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Unlock using biometric authentication, returning the decrypted master key.
     */
    fun unlockWithBiometric(
        activity: FragmentActivity,
        onSuccess: (SecretKey) -> Unit,
        onError: (String) -> Unit
    )

    /**
     * Check if biometric authentication is enabled.
     */
    fun isBiometricEnabled(): Boolean

    /**
     * Disable biometric authentication.
     */
    fun disableBiometric(): AppResult<Unit>

    /**
     * Check if master password is set.
     */
    fun isMasterPasswordSet(): Boolean

    /**
     * Get master password hint.
     */
    fun getMasterPasswordHint(): String?
}
