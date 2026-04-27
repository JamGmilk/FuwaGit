package jamgmilk.fuwagit.ui.components.biometric

import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.AuthenticationResultCallback
import androidx.biometric.BiometricManager
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jamgmilk.fuwagit.R

private object BiometricErrorCodes {
    const val USER_CANCELED = 10
    const val NO_BIOMETRICS = 7
    const val LOCKOUT = 5
}

@Composable
fun BiometricAuthenticator(
    title: String,
    subtitle: String,
    negativeButtonText: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
    isEnabled: Boolean = true
) {
    var authState by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val resultCallback = AuthenticationResultCallback { result ->
        when {
            result is AuthenticationResult.Success -> onSuccess()
            result is AuthenticationResult.Error -> {
                when (result.errorCode) {
                    BiometricErrorCodes.USER_CANCELED,
                    BiometricErrorCodes.NO_BIOMETRICS,
                    BiometricErrorCodes.LOCKOUT -> onCancelled()
                    else -> onError(result.errString.toString())
                }
            }
            else -> { }
        }
    }

    val authLauncher = rememberAuthenticationLauncher(resultCallback)

    LaunchedEffect(isEnabled, authState) {
        if (isEnabled && !authState) {
            val biometricManager = BiometricManager.from(context)
            val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                authState = true
                val request = AuthenticationRequest.Biometric.Builder(
                    title = title,
                    authFallbacks = arrayOf()
                ).build()
                authLauncher.launch(request)
            } else {
                onError(context.getString(R.string.biometric_not_available))
            }
        }
    }
}

@Composable
fun BiometricEnableAuthenticator(
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
    isEnabled: Boolean = true
) {
    BiometricAuthenticator(
        title = stringResource(R.string.biometric_enable_title),
        subtitle = stringResource(R.string.biometric_enable_subtitle),
        negativeButtonText = stringResource(R.string.settings_biometric_cancel),
        onSuccess = onSuccess,
        onError = onError,
        onCancelled = onCancelled,
        isEnabled = isEnabled
    )
}

@Composable
fun BiometricUnlockAuthenticator(
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit,
    isEnabled: Boolean = true
) {
    BiometricAuthenticator(
        title = stringResource(R.string.biometric_unlock_title),
        subtitle = stringResource(R.string.credentials_unlock_biometric_subtitle),
        negativeButtonText = stringResource(R.string.credentials_use_password),
        onSuccess = onSuccess,
        onError = onError,
        onCancelled = onCancelled,
        isEnabled = isEnabled
    )
}
