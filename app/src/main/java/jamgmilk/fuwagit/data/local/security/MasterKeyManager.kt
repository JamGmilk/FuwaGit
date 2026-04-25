package jamgmilk.fuwagit.data.local.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import jamgmilk.fuwagit.BuildConfig
import jamgmilk.fuwagit.data.biometric.BiometricAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

private class SecureDerivedKey(private val spec: PBEKeySpec, private val keyBytes: ByteArray) : SecretKey {
    override fun getAlgorithm(): String = "AES"
    override fun getFormat(): String = "RAW"
    override fun getEncoded(): ByteArray = keyBytes
    fun secureZero() {
        spec.clearPassword()
        java.util.Arrays.fill(keyBytes, 0.toByte())
    }
}

@Singleton
class MasterKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val biometricAuthManager: BiometricAuthManager
) {

    companion object {
        private const val TAG = "MasterKeyManager"
        private const val KEYSTORE_BIOMETRIC_ALIAS = "fuwagit_biometric_key"
        private const val PREFS_NAME = "credential_key_store"
        private const val KEY_ENCRYPTED_MASTER = "encrypted_master_key"
        private const val KEY_SALT = "salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_BIOMETRIC_ENCRYPTED_MASTER = "biometric_encrypted_master"
        private const val KEY_BIOMETRIC_IV = "biometric_iv"
        private const val PBKDF2_ITERATIONS = 100000
        private const val KEY_PASSWORD_HINT = "password_hint"
        private const val KEY_LENGTH = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isMasterPasswordSet(): Boolean {
        return prefs.contains(KEY_ENCRYPTED_MASTER)
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    suspend fun setupMasterPassword(password: String): Result<SecretKey> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val salt = SecureRandom().generateSeed(32)
                val derivedKey = deriveKeySecurely(password, salt)
                val masterKey = generateRandomKey()
                val encryptedMasterKey = encryptWithKey(masterKey.encoded, derivedKey)

                prefs.edit {
                    putString(KEY_ENCRYPTED_MASTER,
                        Base64.encodeToString(encryptedMasterKey, Base64.NO_WRAP))
                    putString(KEY_SALT,
                        Base64.encodeToString(salt, Base64.NO_WRAP))
                }

                val result = SecretKeySpec(masterKey.encoded, "AES")
                derivedKey.secureZero()
                masterKey.encoded?.let { java.util.Arrays.fill(it, 0.toByte()) }
                result
            }
        }
    }

    private fun deriveKeySecurely(password: String, salt: ByteArray): SecureDerivedKey {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecureDerivedKey(spec, keyBytes)
    }

    suspend fun unlockWithPassword(password: String): Result<SecretKey> {
        return withContext(Dispatchers.IO) {
            try {
                val saltBase64 = prefs.getString(KEY_SALT, null)
                    ?: throw IllegalStateException("Salt not found")
                val encryptedMasterBase64 = prefs.getString(KEY_ENCRYPTED_MASTER, null)
                    ?: throw IllegalStateException("Encrypted master key not found")

                val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
                val encryptedMasterKey = Base64.decode(encryptedMasterBase64, Base64.NO_WRAP)

                val derivedKey = deriveKeySecurely(password, salt)
                val masterKeyBytes = decryptWithKey(encryptedMasterKey, derivedKey)
                derivedKey.secureZero()

                Result.success(SecretKeySpec(masterKeyBytes, "AES"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun changeMasterPassword(
        oldPassword: String,
        newPassword: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val masterKey = unlockWithPassword(oldPassword).getOrThrow()

                val newSalt = SecureRandom().generateSeed(32)
                val newDerivedKey = deriveKeySecurely(newPassword, newSalt)
                val encryptedMasterKey = encryptWithKey(masterKey.encoded, newDerivedKey)

                prefs.edit {
                    putString(KEY_ENCRYPTED_MASTER,
                        Base64.encodeToString(encryptedMasterKey, Base64.NO_WRAP))
                    putString(KEY_SALT,
                        Base64.encodeToString(newSalt, Base64.NO_WRAP))
                }

                newDerivedKey.secureZero()
                masterKey.encoded?.let { java.util.Arrays.fill(it, 0.toByte()) }

                if (isBiometricEnabled()) {
                    disableBiometric()
                }
            }
        }
    }

    fun enableBiometric(
        activity: FragmentActivity,
        masterKey: SecretKey,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (BuildConfig.DEBUG) Log.d(TAG, "enableBiometric: starting")
        try {
            if (keyStore.containsAlias(KEYSTORE_BIOMETRIC_ALIAS)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "enableBiometric: deleting existing key")
                keyStore.deleteEntry(KEYSTORE_BIOMETRIC_ALIAS)
            }

            if (BuildConfig.DEBUG) Log.d(TAG, "enableBiometric: creating biometric key")
            createBiometricKey()

            if (BuildConfig.DEBUG) Log.d(TAG, "enableBiometric: creating cipher")
            val cipher = createBiometricCipher()

            biometricAuthManager.authenticateWithCrypto(
                activity = activity,
                action = BiometricAuthManager.AuthAction.ENABLE,
                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                onResult = { result ->
                    when (result) {
                        is BiometricAuthManager.AuthResult.SuccessWithCrypto -> {
                            if (BuildConfig.DEBUG) Log.d(TAG, "enableBiometric: onAuthenticationSucceeded")
                            result.result.cryptoObject?.cipher?.let { c ->
                                try {
                                    val encrypted = c.doFinal(masterKey.encoded)
                                    val actualIv = c.iv
                                    prefs.edit {
                                        putString(KEY_BIOMETRIC_ENCRYPTED_MASTER,
                                            Base64.encodeToString(encrypted, Base64.NO_WRAP))
                                        putString(KEY_BIOMETRIC_IV,
                                            Base64.encodeToString(actualIv, Base64.NO_WRAP))
                                        putBoolean(KEY_BIOMETRIC_ENABLED, true)
                                        apply()
                                    }
                                    if (BuildConfig.DEBUG) Log.d(TAG, "enableBiometric: success saved to prefs with IV size ${actualIv?.size}")
                                    onSuccess()
                                } catch (e: Exception) {
                                    Log.e(TAG, "enableBiometric: doFinal failed: ${e.message}")
                                    onError("Encryption failed: ${e.message}")
                                }
                            } ?: run {
                                Log.e(TAG, "enableBiometric: Cipher is null")
                                onError("Cipher is null")
                            }
                        }
                        is BiometricAuthManager.AuthResult.Error -> {
                            Log.e(TAG, "enableBiometric: onAuthenticationError: ${result.code} - ${result.message}")
                            onError(result.message)
                        }
                        is BiometricAuthManager.AuthResult.Cancelled -> {
                            if (BuildConfig.DEBUG) Log.d(TAG, "enableBiometric: cancelled")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "enableBiometric: exception: ${e.message}", e)
            onError(e.message ?: "Failed to enable biometric")
        }
    }

    fun unlockWithBiometric(
        activity: FragmentActivity,
        onSuccess: (SecretKey) -> Unit,
        onError: (String) -> Unit
    ) {
        if (BuildConfig.DEBUG) Log.d(TAG, "unlockWithBiometric: starting")
        try {
            val ivBase64 = prefs.getString(KEY_BIOMETRIC_IV, null)
            if (ivBase64 == null) {
                Log.e(TAG, "unlockWithBiometric: Biometric not set up")
                onError("Biometric not set up")
                return
            }

            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipher = createBiometricCipherForDecrypt(iv)

            biometricAuthManager.authenticateWithCrypto(
                activity = activity,
                action = BiometricAuthManager.AuthAction.UNLOCK,
                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                onResult = { result ->
                    when (result) {
                        is BiometricAuthManager.AuthResult.SuccessWithCrypto -> {
                            if (BuildConfig.DEBUG) Log.d(TAG, "unlockWithBiometric: onAuthenticationSucceeded")
                            result.result.cryptoObject?.cipher?.let { c ->
                                val encryptedBase64 = prefs.getString(KEY_BIOMETRIC_ENCRYPTED_MASTER, null)
                                if (encryptedBase64 != null) {
                                    try {
                                        val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                                        val masterKeyBytes = c.doFinal(encrypted)
                                        if (BuildConfig.DEBUG) Log.d(TAG, "unlockWithBiometric: success")
                                        onSuccess(SecretKeySpec(masterKeyBytes, "AES"))
                                    } catch (e: Exception) {
                                        Log.e(TAG, "unlockWithBiometric: doFinal failed: ${e.message}")
                                        if (e is javax.crypto.AEADBadTagException) {
                                            Log.w(TAG, "unlockWithBiometric: AEADBadTagException, disabling biometric")
                                            disableBiometric()
                                            onError("Biometric data corrupted. Please re-enable in settings.")
                                        } else {
                                            onError("Decryption failed: ${e.message}")
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "unlockWithBiometric: No biometric data found")
                                    onError("No biometric data found")
                                }
                            } ?: run {
                                Log.e(TAG, "unlockWithBiometric: Cipher is null")
                                onError("Cipher is null")
                            }
                        }
                        is BiometricAuthManager.AuthResult.Error -> {
                            Log.e(TAG, "unlockWithBiometric: onAuthenticationError: ${result.code} - ${result.message}")
                            onError(result.message)
                        }
                        is BiometricAuthManager.AuthResult.Cancelled -> {
                            if (BuildConfig.DEBUG) Log.d(TAG, "unlockWithBiometric: cancelled")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "unlockWithBiometric: exception: ${e.message}", e)
            onError(e.message ?: "Failed to unlock with biometric")
        }
    }

    fun disableBiometric() {
        prefs.edit {
            remove(KEY_BIOMETRIC_ENCRYPTED_MASTER)
            remove(KEY_BIOMETRIC_IV)
            putBoolean(KEY_BIOMETRIC_ENABLED, false)
            apply()
        }
        try {
            if (keyStore.containsAlias(KEYSTORE_BIOMETRIC_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_BIOMETRIC_ALIAS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "disableBiometric: failed to delete key entry", e)
        }
    }

    fun setPasswordHint(hint: String) {
        prefs.edit { putString(KEY_PASSWORD_HINT, hint) }
    }

    fun getPasswordHint(): String? {
        return prefs.getString(KEY_PASSWORD_HINT, null)
    }

    private fun generateRandomKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(KEY_LENGTH, SecureRandom())
        return keyGenerator.generateKey()
    }

    private fun encryptWithKey(data: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(data)
        return cipher.iv + encrypted
    }

    private fun decryptWithKey(data: ByteArray, key: SecretKey): ByteArray {
        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = data.copyOfRange(GCM_IV_LENGTH, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }

    private fun createBiometricKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_BIOMETRIC_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_LENGTH)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()

        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    private fun createBiometricCipher(): Cipher {
        val secretKey = keyStore.getKey(KEYSTORE_BIOMETRIC_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    private fun createBiometricCipherForDecrypt(iv: ByteArray): Cipher {
        val secretKey = keyStore.getKey(KEYSTORE_BIOMETRIC_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher
    }
}
