package jamgmilk.fuwagit.data.biometric

import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
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

    fun authenticate(
        activity: FragmentActivity,
        action: AuthAction,
        onResult: (AuthResult) -> Unit
    ) {
        authenticateWithCrypto(activity, action, null, onResult)
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

@AndroidEntryPoint
class BiometricActivityResult : FragmentActivity() {

    @Inject
    lateinit var manager: BiometricAuthManager

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.getStringExtra(EXTRA_ACTION) ?: run {
            finish()
            return
        }

        val authAction = when (action) {
            ACTION_ENABLE -> BiometricAuthManager.AuthAction.ENABLE
            ACTION_UNLOCK -> BiometricAuthManager.AuthAction.UNLOCK
            else -> {
                finish()
                return
            }
        }

        manager.authenticate(this, authAction) { result ->
            when (result) {
                is BiometricAuthManager.AuthResult.Error -> {
                    setResult(RESULT_ERROR, intent.apply {
                        putExtra(EXTRA_ERROR_CODE, result.code)
                        putExtra(EXTRA_ERROR_MESSAGE, result.message)
                    })
                }
                is BiometricAuthManager.AuthResult.Cancelled -> {
                    setResult(RESULT_CANCELED)
                }
                is BiometricAuthManager.AuthResult.SuccessWithCrypto -> {
                    setResult(RESULT_SUCCESS)
                }
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_ACTION = "biometric_action"
        const val EXTRA_ERROR_MESSAGE = "biometric_error_message"
        const val EXTRA_ERROR_CODE = "biometric_error_code"

        const val ACTION_ENABLE = "enable"
        const val ACTION_UNLOCK = "unlock"

        const val RESULT_SUCCESS = 1001
        const val RESULT_ERROR = 1002
        const val RESULT_CANCELED = 1003
    }
}
