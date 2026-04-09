package jamgmilk.fuwagit.data.local.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import jamgmilk.fuwagit.BuildConfig
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
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import jamgmilk.fuwagit.data.biometric.BiometricAuthManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val biometricAuthManager: BiometricAuthManager
) {

    companion object {
        private const val KEYSTORE_ALIAS = "fuwagit_credential_key"
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

    private val prefs: android.content.SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
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
                val derivedKey = deriveKey(password, salt)
                val masterKey = generateRandomKey()
                val encryptedMasterKey = encryptWithKey(masterKey.encoded, derivedKey)

                prefs.edit {
                    putString(KEY_ENCRYPTED_MASTER,
                        Base64.encodeToString(encryptedMasterKey, Base64.NO_WRAP))
                    putString(KEY_SALT,
                        Base64.encodeToString(salt, Base64.NO_WRAP))
                }

                SecretKeySpec(masterKey.encoded, "AES")
            }
        }
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

                val derivedKey = deriveKey(password, salt)
                val masterKeyBytes = decryptWithKey(encryptedMasterKey, derivedKey)

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
                val newDerivedKey = deriveKey(newPassword, newSalt)
                val encryptedMasterKey = encryptWithKey(masterKey.encoded, newDerivedKey)

                prefs.edit {
                    putString(KEY_ENCRYPTED_MASTER,
                        Base64.encodeToString(encryptedMasterKey, Base64.NO_WRAP))
                    putString(KEY_SALT,
                        Base64.encodeToString(newSalt, Base64.NO_WRAP))
                }

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
        if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "enableBiometric: starting")
        try {
            if (keyStore.containsAlias(KEYSTORE_BIOMETRIC_ALIAS)) {
                if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "enableBiometric: deleting existing key")
                keyStore.deleteEntry(KEYSTORE_BIOMETRIC_ALIAS)
            }

            if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "enableBiometric: creating biometric key")
            createBiometricKey()

            if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "enableBiometric: creating cipher")
            val cipher = createBiometricCipher(Cipher.ENCRYPT_MODE)

            biometricAuthManager.authenticateWithCrypto(
                activity = activity,
                action = BiometricAuthManager.AuthAction.ENABLE,
                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                onResult = { result ->
                    when (result) {
                        is BiometricAuthManager.AuthResult.SuccessWithCrypto -> {
                            if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "enableBiometric: onAuthenticationSucceeded")
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
                                    if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "enableBiometric: success saved to prefs with IV size ${actualIv?.size}")
                                    onSuccess()
                                } catch (e: Exception) {
                                    android.util.Log.e("MasterKeyManager", "enableBiometric: doFinal failed: ${e.message}")
                                    onError("Encryption failed: ${e.message}")
                                }
                            } ?: run {
                                android.util.Log.e("MasterKeyManager", "enableBiometric: Cipher is null")
                                onError("Cipher is null")
                            }
                        }
                        is BiometricAuthManager.AuthResult.Error -> {
                            android.util.Log.e("MasterKeyManager", "enableBiometric: onAuthenticationError: ${result.code} - ${result.message}")
                            onError(result.message)
                        }
                        is BiometricAuthManager.AuthResult.Cancelled -> {
                            if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "enableBiometric: cancelled")
                        }
                        else -> {}
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("MasterKeyManager", "enableBiometric: exception: ${e.message}", e)
            onError(e.message ?: "Failed to enable biometric")
        }
    }

    fun unlockWithBiometric(
        activity: FragmentActivity,
        onSuccess: (SecretKey) -> Unit,
        onError: (String) -> Unit
    ) {
        if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "unlockWithBiometric: starting")
        try {
            val ivBase64 = prefs.getString(KEY_BIOMETRIC_IV, null)
            if (ivBase64 == null) {
                android.util.Log.e("MasterKeyManager", "unlockWithBiometric: Biometric not set up")
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
                            if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "unlockWithBiometric: onAuthenticationSucceeded")
                            result.result.cryptoObject?.cipher?.let { c ->
                                val encryptedBase64 = prefs.getString(KEY_BIOMETRIC_ENCRYPTED_MASTER, null)
                                if (encryptedBase64 != null) {
                                    try {
                                        val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                                        val masterKeyBytes = c.doFinal(encrypted)
                                        if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "unlockWithBiometric: success")
                                        onSuccess(SecretKeySpec(masterKeyBytes, "AES"))
                                    } catch (e: Exception) {
                                        android.util.Log.e("MasterKeyManager", "unlockWithBiometric: doFinal failed: ${e.message}")
                                        if (e is javax.crypto.AEADBadTagException) {
                                            android.util.Log.w("MasterKeyManager", "unlockWithBiometric: AEADBadTagException, disabling biometric")
                                            disableBiometric()
                                            onError("Biometric data corrupted. Please re-enable in settings.")
                                        } else {
                                            onError("Decryption failed: ${e.message}")
                                        }
                                    }
                                } else {
                                    android.util.Log.e("MasterKeyManager", "unlockWithBiometric: No biometric data found")
                                    onError("No biometric data found")
                                }
                            } ?: run {
                                android.util.Log.e("MasterKeyManager", "unlockWithBiometric: Cipher is null")
                                onError("Cipher is null")
                            }
                        }
                        is BiometricAuthManager.AuthResult.Error -> {
                            android.util.Log.e("MasterKeyManager", "unlockWithBiometric: onAuthenticationError: ${result.code} - ${result.message}")
                            onError(result.message)
                        }
                        is BiometricAuthManager.AuthResult.Cancelled -> {
                            if (BuildConfig.DEBUG) android.util.Log.d("MasterKeyManager", "unlockWithBiometric: cancelled")
                        }
                        else -> {}
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("MasterKeyManager", "unlockWithBiometric: exception: ${e.message}", e)
            onError(e.message ?: "Failed to unlock with biometric")
        }
    }

    fun disableBiometric() {
        prefs.edit {
            remove(KEY_BIOMETRIC_ENCRYPTED_MASTER)
            putBoolean(KEY_BIOMETRIC_ENABLED, false)
            apply()
        }
        try {
            if (keyStore.containsAlias(KEYSTORE_BIOMETRIC_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_BIOMETRIC_ALIAS)
            }
        } catch (e: Exception) {
        }
    }

    fun setPasswordHint(hint: String) {
        prefs.edit { putString(KEY_PASSWORD_HINT, hint) }
    }

    fun getPasswordHint(): String? {
        return prefs.getString(KEY_PASSWORD_HINT, null)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH
        )
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            // Security: Clear the password char array to prevent sensitive data from lingering in memory
            spec.clearPassword()
        }
    }

    private fun generateRandomKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(KEY_LENGTH)
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
            .setInvalidatedByBiometricEnrollment(true) // 要启用吗这个qwq
            .build()

        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    private fun createBiometricCipher(mode: Int): Cipher {
        val secretKey = keyStore.getKey(KEYSTORE_BIOMETRIC_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, secretKey)
        return cipher
    }

    private fun createBiometricCipherForDecrypt(iv: ByteArray): Cipher {
        val secretKey = keyStore.getKey(KEYSTORE_BIOMETRIC_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher
    }
}
