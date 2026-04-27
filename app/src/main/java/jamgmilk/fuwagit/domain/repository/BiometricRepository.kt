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
     * Check if biometric authentication can be used.
     */
    fun canAuthenticate(): Boolean

    /**
     * Enable biometric authentication by encrypting and storing the master key.
     * Uses BiometricPrompt to secure the encryption.
     */
    suspend fun enableBiometric(
        activity: FragmentActivity,
        masterKey: SecretKey,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): AppResult<Unit>

    /**
     * Unlock using biometric authentication, returning the decrypted master key.
     * Uses BiometricPrompt to secure the decryption.
     */
    suspend fun unlockWithBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): AppResult<SecretKey>

    /**
     * Check if biometric authentication is enabled.
     */
    fun isBiometricEnabled(): Boolean

    /**
     * Disable biometric authentication.
     */
    suspend fun disableBiometric(): AppResult<Unit>

    /**
     * Check if master password is set.
     */
    fun isMasterPasswordSet(): Boolean

    /**
     * Get master password hint.
     */
    fun getMasterPasswordHint(): String?
}
