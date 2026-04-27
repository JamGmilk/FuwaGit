package jamgmilk.fuwagit.data.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthManager @Inject constructor() {

    sealed class AuthAvailability {
        data object Available : AuthAvailability()
        data object NotAvailable : AuthAvailability()
        data object NotEnrolled : AuthAvailability()
    }

    fun canAuthenticate(context: Context): AuthAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> AuthAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> AuthAvailability.NotEnrolled
            else -> AuthAvailability.NotAvailable
        }
    }
}
