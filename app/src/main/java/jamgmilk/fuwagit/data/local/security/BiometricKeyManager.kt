package jamgmilk.fuwagit.data.local.security

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class BiometricKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_NAME = "fuwa_git_biometric_key"
        const val GCM_TAG_LENGTH = 128
        const val GCM_IV_LENGTH = 12
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    fun canAuthenticate(): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }

    suspend fun createBiometricKey(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (keyStore.containsAlias(KEY_NAME)) {
                keyStore.deleteEntry(KEY_NAME)
            }

            val keyGenerator = KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            val builder = android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_NAME,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .setUserAuthenticationValidityDurationSeconds(-1)

            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
            Unit
        }
    }

    suspend fun encryptMasterKey(
        activity: FragmentActivity,
        masterKey: SecretKey,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val cipher = result.cryptoObject?.cipher
                    if (cipher != null) {
                        val encrypted = cipher.doFinal(masterKey.encoded)
                        val iv = cipher.iv
                        val combined = iv + encrypted
                        val encoded = Base64.encodeToString(combined, Base64.NO_WRAP)
                        continuation.resume(Result.success(encoded))
                    } else {
                        continuation.resumeWithException(Exception("Cipher not available"))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                continuation.resumeWithException(BiometricError(errorCode, errString.toString()))
            }

            override fun onAuthenticationFailed() {
                // Don't resume - let user retry
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val cipher = try {
            val key = keyStore.getKey(KEY_NAME, null) as? SecretKey
                ?: throw Exception("Key not found")
            val cipherInstance = Cipher.getInstance("AES/GCM/NoPadding")
            cipherInstance.init(Cipher.ENCRYPT_MODE, key)
            cipherInstance
        } catch (e: Exception) {
            continuation.resumeWithException(e)
            return@suspendCancellableCoroutine
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))

        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
    }

    suspend fun decryptMasterKey(
        activity: FragmentActivity,
        encryptedMasterKey: String,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): Result<SecretKey> = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)

        val combined = Base64.decode(encryptedMasterKey, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                try {
                    val cipher = result.cryptoObject?.cipher
                    if (cipher != null) {
                        val decrypted = cipher.doFinal(encrypted)
                        val secretKey = javax.crypto.spec.SecretKeySpec(decrypted, "AES")
                        continuation.resume(Result.success(secretKey))
                    } else {
                        continuation.resumeWithException(Exception("Cipher not available"))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                continuation.resumeWithException(BiometricError(errorCode, errString.toString()))
            }

            override fun onAuthenticationFailed() {
                // Don't resume - let user retry
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val cipher = try {
            val key = keyStore.getKey(KEY_NAME, null) as? SecretKey
                ?: throw Exception("Key not found")
            val cipherInstance = Cipher.getInstance("AES/GCM/NoPadding")
            cipherInstance.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            cipherInstance
        } catch (e: Exception) {
            continuation.resumeWithException(e)
            return@suspendCancellableCoroutine
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))

        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }
    }

    fun getKey(): SecretKey? {
        return try {
            keyStore.getKey(KEY_NAME, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }

    fun hasBiometricKey(): Boolean {
        return try {
            keyStore.containsAlias(KEY_NAME)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteBiometricKey() = withContext(Dispatchers.IO) {
        try {
            if (keyStore.containsAlias(KEY_NAME)) {
                keyStore.deleteEntry(KEY_NAME)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}

class BiometricError(val errorCode: Int, override val message: String) : Exception(message)
