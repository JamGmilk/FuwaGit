package jamgmilk.fuwagit.data.local.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val secureCredentialStore: SecureCredentialStore
) {

    companion object {
        private const val TAG = "MasterKeyManager"
        private const val PREFS_NAME = "credential_key_store"
        private const val KEY_ENCRYPTED_MASTER = "encrypted_master_key"
        private const val KEY_SALT = "salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PASSWORD_HINT = "password_hint"
        private const val PBKDF2_ITERATIONS = 100000
        private const val KEY_ENCRYPTED_BIOMETRIC_MASTER = "encrypted_biometric_master_key"
        private const val KEY_LENGTH = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
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

                val encodedBytes = masterKey.encoded
                val result = SecretKeySpec(encodedBytes.copyOf(), "AES")
                java.util.Arrays.fill(encodedBytes, 0.toByte())
                derivedKey.secureZero()
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

    suspend fun enableBiometric(masterKey: SecretKey): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                secureCredentialStore.cacheMasterKey(masterKey)
                prefs.edit {
                    putBoolean(KEY_BIOMETRIC_ENABLED, true)
                }
            }
        }
    }

    suspend fun unlockWithBiometric(): Result<SecretKey> {
        return withContext(Dispatchers.IO) {
            try {
                val cachedKey = secureCredentialStore.getCachedMasterKey()
                if (cachedKey != null) {
                    Result.success(cachedKey)
                } else {
                    Result.failure(Exception("Biometric session expired. Please enter your password."))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun disableBiometric() {
        withContext(Dispatchers.IO) {
            secureCredentialStore.clearCachedMasterKey()
            prefs.edit {
                putBoolean(KEY_BIOMETRIC_ENABLED, false)
            }
        }
    }

    fun setPasswordHint(hint: String) {
        prefs.edit { putString(KEY_PASSWORD_HINT, hint) }
    }

    fun getPasswordHint(): String? {
        return prefs.getString(KEY_PASSWORD_HINT, null)
    }

    fun hasEncryptedMasterKey(): Boolean {
        return prefs.contains(KEY_ENCRYPTED_BIOMETRIC_MASTER)
    }

    fun getEncryptedMasterKey(): String? {
        return prefs.getString(KEY_ENCRYPTED_BIOMETRIC_MASTER, null)
    }

    fun saveEncryptedMasterKey(encryptedKey: String) {
        prefs.edit { putString(KEY_ENCRYPTED_BIOMETRIC_MASTER, encryptedKey) }
    }

    fun clearEncryptedMasterKey() {
        prefs.edit { remove(KEY_ENCRYPTED_BIOMETRIC_MASTER) }
    }

    fun setBiometricEnabledInternal(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BIOMETRIC_ENABLED, enabled) }
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
}
