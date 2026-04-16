package jamgmilk.fuwagit.data.biometric

import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthManager @Inject constructor(
) {
    companion object {
        private const val TAG = "BiometricAuthManager"
    }
    sealed class AuthResult {
        data object Cancelled : AuthResult()
        data class Error(val code: Int, val message: String) : AuthResult()
        data class SuccessWithCrypto(val result: BiometricPrompt.AuthenticationResult) : AuthResult()
    }

    enum class AuthAction {
        ENABLE,
        UNLOCK
    }

    fun authenticateWithCrypto(
        activity: FragmentActivity,
        action: AuthAction,
        cryptoObject: BiometricPrompt.CryptoObject?,
        onResult: (AuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(AuthResult.SuccessWithCrypto(result))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when {
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onResult(AuthResult.Cancelled)
                    }
                    else -> {
                        onResult(AuthResult.Error(errorCode, errString.toString()))
                    }
                }
            }

            override fun onAuthenticationFailed() {
                Log.w(TAG, "Biometric authentication failed")
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = when (action) {
            AuthAction.ENABLE -> BiometricPrompt.PromptInfo.Builder()
                .setTitle("Enable Biometric Unlock")
                .setSubtitle("Use your fingerprint to quickly access credentials")
                .setNegativeButtonText("Cancel")
                .build()

            AuthAction.UNLOCK -> BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Credentials")
                .setSubtitle("Use your fingerprint to access credentials")
                .setNegativeButtonText("Use Password")
                .build()
        }

        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
