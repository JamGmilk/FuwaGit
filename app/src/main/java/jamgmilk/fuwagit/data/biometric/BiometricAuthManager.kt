package jamgmilk.fuwagit.data.biometric

import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BiometricAuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    sealed class AuthResult {
        data object Success : AuthResult()
        data class SuccessWithCrypto(val result: BiometricPrompt.AuthenticationResult) : AuthResult()
        data class Error(val code: Int, val message: String) : AuthResult()
        data object Cancelled : AuthResult()
    }

    enum class AuthAction {
        ENABLE,
        UNLOCK
    }

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
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
                // User can retry, don't complete the auth
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

    suspend fun authenticateSuspend(
        activity: FragmentActivity,
        action: AuthAction
    ): AuthResult = suspendCancellableCoroutine { continuation ->
        authenticate(activity, action) { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = "biometric_action"
        const val EXTRA_RESULT = "biometric_result"
        const val EXTRA_ERROR_MESSAGE = "biometric_error_message"
        const val EXTRA_ERROR_CODE = "biometric_error_code"

        const val ACTION_ENABLE = "enable"
        const val ACTION_UNLOCK = "unlock"

        const val RESULT_SUCCESS = 1001
        const val RESULT_ERROR = 1002
        const val RESULT_CANCELED = 1003

        fun createIntent(activity: Context, action: String): Intent {
            return Intent(activity, BiometricActivityResult::class.java).apply {
                putExtra(EXTRA_ACTION, action)
            }
        }
    }
}

@AndroidEntryPoint
class BiometricActivityResult : FragmentActivity() {
    @Inject
    lateinit var manager: BiometricAuthManager

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.getStringExtra(BiometricAuthManager.EXTRA_ACTION) ?: run {
            finish()
            return
        }

        val authAction = when (action) {
            BiometricAuthManager.ACTION_ENABLE -> BiometricAuthManager.AuthAction.ENABLE
            BiometricAuthManager.ACTION_UNLOCK -> BiometricAuthManager.AuthAction.UNLOCK
            else -> {
                finish()
                return
            }
        }

        manager.authenticate(this, authAction) { result ->
            val intent = Intent()
            when (result) {
                is BiometricAuthManager.AuthResult.Success,
                is BiometricAuthManager.AuthResult.SuccessWithCrypto -> {
                    setResult(BiometricAuthManager.RESULT_SUCCESS)
                }
                is BiometricAuthManager.AuthResult.Error -> {
                    intent.putExtra(BiometricAuthManager.EXTRA_ERROR_CODE, result.code)
                    intent.putExtra(BiometricAuthManager.EXTRA_ERROR_MESSAGE, result.message)
                    setResult(BiometricAuthManager.RESULT_ERROR, intent)
                }
                is BiometricAuthManager.AuthResult.Cancelled -> {
                    setResult(BiometricAuthManager.RESULT_CANCELED)
                }
            }
            finish()
        }
    }
}
