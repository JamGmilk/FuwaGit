package jamgmilk.fuwagit.ui.biometric

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class BiometricActivity : FragmentActivity() {
    companion object {
        private const val TAG = "BiometricActivity"
        const val EXTRA_ACTION = "action"
        const val EXTRA_MASTER_KEY = "master_key"
        const val ACTION_ENABLE = "enable"
        const val ACTION_UNLOCK = "unlock"

        const val RESULT_SUCCESS = Activity.RESULT_FIRST_USER + 1
        const val RESULT_ERROR = Activity.RESULT_FIRST_USER + 2
        const val RESULT_CANCELED = Activity.RESULT_FIRST_USER + 3

        const val EXTRA_ERROR_MESSAGE = "error_message"

        fun createIntent(activity: Activity, action: String, masterKeyBase64: String? = null): Intent {
            return Intent(activity, BiometricActivity::class.java).apply {
                putExtra(EXTRA_ACTION, action)
                putExtra(EXTRA_MASTER_KEY, masterKeyBase64)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "BiometricActivity created, action: ${intent.getStringExtra(EXTRA_ACTION)}")
        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        val action = intent.getStringExtra(EXTRA_ACTION) ?: run {
            finish()
            return
        }

        val masterKeyBase64 = intent.getStringExtra(EXTRA_MASTER_KEY)
        Log.d(TAG, "masterKeyBase64: ${masterKeyBase64 != null}")

        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.d(TAG, "Biometric authentication succeeded")
                setResult(RESULT_SUCCESS)
                finish()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.d(TAG, "Biometric error: $errorCode - $errString")
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    val data = Intent().apply {
                        putExtra(EXTRA_ERROR_MESSAGE, errString.toString())
                    }
                    setResult(RESULT_ERROR, data)
                } else {
                    setResult(RESULT_CANCELED)
                }
                finish()
            }

            override fun onAuthenticationFailed() {
                Log.d(TAG, "Biometric authentication failed")
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)

        val promptInfo = when (action) {
            ACTION_ENABLE -> BiometricPrompt.PromptInfo.Builder()
                .setTitle("Enable Biometric Unlock")
                .setSubtitle("Use your fingerprint to quickly access credentials")
                .setNegativeButtonText("Cancel")
                .build()

            ACTION_UNLOCK -> BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Credentials")
                .setSubtitle("Use your fingerprint to access credentials")
                .setNegativeButtonText("Use Password")
                .build()

            else -> {
                finish()
                return
            }
        }

        biometricPrompt.authenticate(promptInfo)
    }
}
