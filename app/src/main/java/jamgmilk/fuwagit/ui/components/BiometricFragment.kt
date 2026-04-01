package jamgmilk.fuwagit.ui.components

import android.os.Bundle
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

object BiometricHelper {
    private const val TAG = "BiometricHelper"
    const val KEY_ACTION = "action"
    const val ACTION_ENABLE = "enable"
    const val ACTION_UNLOCK = "unlock"

    fun show(
        activity: FragmentActivity,
        action: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val fragment = BiometricFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_ACTION, action)
            }
            this.onBiometricSuccess = onSuccess
            this.onBiometricError = onError
        }

        activity.supportFragmentManager.beginTransaction()
            .add(fragment, "biometric_fragment")
            .commit()
    }
}

class BiometricFragment : Fragment() {
    internal var onBiometricSuccess: (() -> Unit)? = null
    internal var onBiometricError: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BiometricFragment", "onCreate")
        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        val activity = requireActivity()

        val executor = ContextCompat.getMainExecutor(requireContext())

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Log.d("BiometricFragment", "Authentication succeeded")
                onBiometricSuccess?.invoke()
                dismiss()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.d("BiometricFragment", "Authentication error: $errorCode - $errString")
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onBiometricError?.invoke(errString.toString())
                }
                dismiss()
            }

            override fun onAuthenticationFailed() {
                Log.d("BiometricFragment", "Authentication failed")
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Authenticate to enable biometric unlock")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun dismiss() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }
}
