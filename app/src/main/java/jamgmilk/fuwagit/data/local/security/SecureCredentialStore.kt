package jamgmilk.fuwagit.data.local.security

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import jamgmilk.fuwagit.data.local.credential.CredentialData
import jamgmilk.fuwagit.data.local.credential.ExportData
import jamgmilk.fuwagit.data.local.credential.HttpsCredential
import jamgmilk.fuwagit.data.local.credential.SshKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferencesStore: jamgmilk.fuwagit.data.local.prefs.AppPreferencesStore
) {

    companion object {
        private const val DATA_FILE = "credential_data.json"
        private const val ENCRYPTED_MARKER = "ENC:AES_GCM:"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val BIOMETRIC_MAX_SESSION_MILLIS = 24 * 60 * 60 * 1000L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val dataFile: File by lazy {
        File(context.filesDir, DATA_FILE).also { file ->
            // Explicitly set file permissions to owner read/write only
            // While filesDir defaults to 0700, this ensures security even if defaults change
            setFilePermissions(file)
        }
    }

    /**
     * Sets explicit file permissions to owner read/write only (0600).
     * This provides defense-in-depth security for sensitive credential data.
     */
    private fun setFilePermissions(file: File) {
        if (file.exists()) {
            // Remove all permissions first
            file.setReadable(false, false)
            file.setWritable(false, false)
            file.setExecutable(false, false)
            // Set owner read/write only
            file.setReadable(true, true)
            file.setWritable(true, true)
        }
    }

    private var cachedMasterKey: SecretKey? = null
    private var lastUnlockTime: Long = 0
    private var isBiometricSession: Boolean = false
    private val sessionLock = Any()

    /**
     * Gets the session timeout in milliseconds from user preferences.
     * Returns 0 if auto-lock is disabled, or the configured timeout value otherwise.
     * Negative values are treated as disabled (0).
     * Minimum timeout is 30 seconds, maximum is 24 hours.
     */
    private suspend fun getSessionTimeoutMillis(): Long {
        val timeoutSeconds = appPreferencesStore.preferencesFlow
            .first { true }
            .autoLockTimeout
            .toLongOrNull() ?: 300L

        val validTimeout = when {
            timeoutSeconds < 0 -> 0L
            timeoutSeconds == 0L -> 0L
            timeoutSeconds < 30 -> 30L
            timeoutSeconds > 86400 -> 86400L
            else -> timeoutSeconds
        }

        return if (validTimeout == 0L) 0L else validTimeout * 1000L
    }

    fun loadCredentialData(): CredentialData {
        return try {
            if (!dataFile.exists()) {
                CredentialData()
            } else {
                val content = dataFile.readText()
                if (content.isBlank()) {
                    CredentialData()
                } else {
                    json.decodeFromString(content)
                }
            }
        } catch (e: Exception) {
            CredentialData()
        }
    }

    fun saveCredentialData(data: CredentialData) {
        val updatedData = data.copy(updated_at = System.currentTimeMillis())
        val jsonString = json.encodeToString(updatedData)

        // Atomic write using a temporary file
        val tempFile = File(context.filesDir, "$DATA_FILE.tmp")
        try {
            tempFile.writeText(jsonString)
            // Set restrictive permissions before renaming
            setFilePermissions(tempFile)
            if (!tempFile.renameTo(dataFile)) {
                // If rename fails, try to copy and delete
                tempFile.copyTo(dataFile, overwrite = true)
                setFilePermissions(dataFile)
                tempFile.delete()
            }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    fun getPublicCredentials(): List<HttpsCredential> {
        return loadCredentialData().https_credentials
    }

    fun getPublicSshKeys(): List<SshKey> {
        return loadCredentialData().ssh_keys
    }

    fun cacheMasterKey(key: SecretKey) {
        synchronized(sessionLock) {
            secureClearCachedKey()
            cachedMasterKey = key
            lastUnlockTime = System.currentTimeMillis()
            isBiometricSession = false
        }
    }

    fun cacheMasterKeyFromBiometric(key: SecretKey) {
        synchronized(sessionLock) {
            secureClearCachedKey()
            cachedMasterKey = key
            lastUnlockTime = System.currentTimeMillis()
            isBiometricSession = true
        }
    }

    private fun secureClearCachedKey() {
        cachedMasterKey?.let { key ->
            val keyBytes = key.encoded
            if (keyBytes != null) {
                java.util.Arrays.fill(keyBytes, 0.toByte())
            }
        }
        cachedMasterKey = null
    }

    suspend fun getCachedMasterKey(): SecretKey? {
        val sessionTimeout = getSessionTimeoutMillis()
        
        return synchronized(sessionLock) {
            val key = cachedMasterKey

            if (key == null) {
                lastUnlockTime = 0
                isBiometricSession = false
                return@synchronized null
            }

            val biometricTimeout = if (isBiometricSession) BIOMETRIC_MAX_SESSION_MILLIS else 0L

            val effectiveTimeout = when {
                sessionTimeout == 0L && biometricTimeout == 0L -> 0L
                sessionTimeout == 0L -> biometricTimeout
                biometricTimeout == 0L -> sessionTimeout
                else -> minOf(sessionTimeout, biometricTimeout)
            }

            if (effectiveTimeout == 0L) {
                return@synchronized key
            }

            val elapsed = System.currentTimeMillis() - lastUnlockTime
            if (elapsed < effectiveTimeout) {
                return@synchronized key
            }

            secureClearCachedKey()
            lastUnlockTime = 0
            isBiometricSession = false
            null
        }
    }

    fun clearCachedMasterKey() {
        synchronized(sessionLock) {
            secureClearCachedKey()
            lastUnlockTime = 0
            isBiometricSession = false
        }
    }

    suspend fun isSessionValid(): Boolean {
        val sessionTimeout = getSessionTimeoutMillis()
        
        return synchronized(sessionLock) {
            val key = cachedMasterKey

            if (key == null) {
                isBiometricSession = false
                return@synchronized false
            }

            val biometricTimeout = if (isBiometricSession) BIOMETRIC_MAX_SESSION_MILLIS else 0L

            val effectiveTimeout = when {
                sessionTimeout == 0L && biometricTimeout == 0L -> 0L
                sessionTimeout == 0L -> biometricTimeout
                biometricTimeout == 0L -> sessionTimeout
                else -> minOf(sessionTimeout, biometricTimeout)
            }

            if (effectiveTimeout == 0L) {
                return@synchronized true
            }

            val elapsed = System.currentTimeMillis() - lastUnlockTime
            val isValid = elapsed < effectiveTimeout

            if (!isValid) {
                secureClearCachedKey()
                lastUnlockTime = 0
                isBiometricSession = false
            }

            isValid
        }
    }

    suspend fun getCredentialDataWithDecryptedSecrets(masterKey: SecretKey): CredentialData {
        return withContext(Dispatchers.IO) {
            val data = loadCredentialData()
            decryptAllSecrets(data, masterKey)
        }
    }

    suspend fun exportAllCredentials(masterKey: SecretKey): String {
        return withContext(Dispatchers.IO) {
            val data = loadCredentialData()
            val dataWithDecryptedSecrets = decryptAllSecrets(data, masterKey)
            val exportData = ExportData(dataWithDecryptedSecrets)
            json.encodeToString(exportData)
        }
    }

    suspend fun importAllCredentials(jsonString: String, masterKey: SecretKey): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val exportData = json.decodeFromString<ExportData>(jsonString)
                val importedData = encryptAllSecrets(exportData.credential_data, masterKey)
                val existingData = loadCredentialData()
                val mergedData = mergeCredentialData(existingData, importedData)
                saveCredentialData(mergedData)
            }
        }
    }

    private fun mergeCredentialData(existing: CredentialData, imported: CredentialData): CredentialData {
        val mergedHttpsCredentials = mergeCredentialLists(
            existing.https_credentials,
            imported.https_credentials
        )
        val mergedSshKeys = mergeSshKeyLists(
            existing.ssh_keys,
            imported.ssh_keys
        )
        return existing.copy(
            https_credentials = mergedHttpsCredentials,
            ssh_keys = mergedSshKeys,
            updated_at = System.currentTimeMillis()
        )
    }

    private fun mergeCredentialLists(
        existing: List<HttpsCredential>,
        imported: List<HttpsCredential>
    ): List<HttpsCredential> {
        val mergedMap = mutableMapOf<String, HttpsCredential>()
        for (cred in existing) {
            mergedMap[cred.uuid] = cred
        }
        for (cred in imported) {
            val existingCred = mergedMap[cred.uuid]
            when {
                existingCred == null -> {
                    mergedMap[cred.uuid] = cred
                }
                cred.updated_at > existingCred.updated_at -> {
                    mergedMap[cred.uuid] = cred
                }
            }
        }
        return mergedMap.values.toList()
    }

    private fun mergeSshKeyLists(
        existing: List<SshKey>,
        imported: List<SshKey>
    ): List<SshKey> {
        val mergedMap = mutableMapOf<String, SshKey>()
        for (key in existing) {
            mergedMap[key.uuid] = key
        }
        for (key in imported) {
            val existingKey = mergedMap[key.uuid]
            when {
                existingKey == null -> {
                    mergedMap[key.uuid] = key
                }
                key.created_at > existingKey.created_at -> {
                    mergedMap[key.uuid] = key
                }
            }
        }
        return mergedMap.values.toList()
    }

    suspend fun addHttpsCredential(
        host: String,
        username: String,
        password: String,
        masterKey: SecretKey
    ): String {
        val uuid = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val data = loadCredentialData()
        val encryptedPassword = encryptField(password, masterKey)

        val newCredential = HttpsCredential(
            uuid = uuid,
            host = host,
            username = username,
            password = encryptedPassword,
            created_at = now,
            updated_at = now
        )

        saveCredentialData(data.copy(
            https_credentials = data.https_credentials + newCredential
        ))

        return uuid
    }

    suspend fun updateHttpsCredential(
        uuid: String,
        host: String? = null,
        username: String? = null,
        password: String? = null,
        masterKey: SecretKey
    ) {
        val now = System.currentTimeMillis()

        val data = loadCredentialData()
        val existingCred = data.https_credentials.find { it.uuid == uuid } ?: return

        val updatedCred = existingCred.copy(
            host = host ?: existingCred.host,
            username = username ?: existingCred.username,
            password = if (password != null) encryptField(password, masterKey) else existingCred.password,
            updated_at = now
        )

        saveCredentialData(data.copy(
            https_credentials = data.https_credentials.map {
                if (it.uuid == uuid) updatedCred else it
            }
        ))
    }

    suspend fun deleteHttpsCredential(uuid: String) {
        val data = loadCredentialData()
        saveCredentialData(data.copy(
            https_credentials = data.https_credentials.filter { it.uuid != uuid }
        ))
    }

    suspend fun addSshKey(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String? = null,
        fingerprint: String,
        masterKey: SecretKey
    ): String {
        val uuid = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val data = loadCredentialData()
        val encryptedPrivateKey = encryptField(privateKey, masterKey)
        val encryptedPassphrase = passphrase?.let { encryptField(it, masterKey) }

        val newKey = SshKey(
            uuid = uuid,
            name = name,
            type = type,
            public_key = publicKey,
            private_key = encryptedPrivateKey,
            passphrase = encryptedPassphrase,
            fingerprint = fingerprint,
            created_at = now
        )

        saveCredentialData(data.copy(
            ssh_keys = data.ssh_keys + newKey
        ))

        return uuid
    }

    suspend fun deleteSshKey(uuid: String) {
        val data = loadCredentialData()
        saveCredentialData(data.copy(
            ssh_keys = data.ssh_keys.filter { it.uuid != uuid }
        ))
    }

    suspend fun getHttpsPassword(uuid: String, masterKey: SecretKey): String? {
        val data = loadCredentialData()
        val cred = data.https_credentials.find { it.uuid == uuid } ?: return null
        return decryptField(cred.password, masterKey)
    }

    suspend fun getSshPrivateKey(uuid: String, masterKey: SecretKey): String? {
        val data = loadCredentialData()
        val key = data.ssh_keys.find { it.uuid == uuid } ?: return null
        return decryptField(key.private_key, masterKey)
    }

    suspend fun getSshPassphrase(uuid: String, masterKey: SecretKey): String? {
        val data = loadCredentialData()
        val key = data.ssh_keys.find { it.uuid == uuid } ?: return null
        val passphrase = key.passphrase ?: return null
        return decryptField(passphrase, masterKey)
    }

    private fun encryptField(value: String, key: SecretKey): String {
        val encrypted = encrypt(value.toByteArray(StandardCharsets.UTF_8), key)
        val encoded = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        return ENCRYPTED_MARKER + encoded
    }

    private fun decryptField(value: String, key: SecretKey): String? {
        return try {
            if (!value.startsWith(ENCRYPTED_MARKER)) {
                value
            } else {
                val encoded = value.substring(ENCRYPTED_MARKER.length)
                val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
                val decrypted = decrypt(encrypted, key)
                String(decrypted, StandardCharsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun decryptAllSecrets(data: CredentialData, masterKey: SecretKey): CredentialData {
        val failedCredentials = mutableListOf<String>()
        val failedKeys = mutableListOf<String>()

        val decryptedCredentials = data.https_credentials.map { cred ->
            val decryptedPassword = decryptField(cred.password, masterKey)
            if (decryptedPassword == null) {
                failedCredentials.add(cred.uuid)
            }
            cred.copy(password = decryptedPassword ?: cred.password)
        }

        val decryptedKeys = data.ssh_keys.map { key ->
            val decryptedPrivateKey = decryptField(key.private_key, masterKey)
            val decryptedPassphrase = key.passphrase?.let { decryptField(it, masterKey) }
            if (decryptedPrivateKey == null) {
                failedKeys.add(key.uuid)
            }
            key.copy(
                private_key = decryptedPrivateKey ?: key.private_key,
                passphrase = decryptedPassphrase ?: key.passphrase
            )
        }

        if (failedCredentials.isNotEmpty() || failedKeys.isNotEmpty()) {
            throw jamgmilk.fuwagit.core.result.AppException.DecryptionFailed(
                "Decryption failed for credentials: $failedCredentials, ssh_keys: $failedKeys"
            )
        }

        return data.copy(
            https_credentials = decryptedCredentials,
            ssh_keys = decryptedKeys
        )
    }

    private fun encryptAllSecrets(data: CredentialData, masterKey: SecretKey): CredentialData {
        return data.copy(
            https_credentials = data.https_credentials.map { cred ->
                cred.copy(password = encryptField(cred.password, masterKey))
            },
            ssh_keys = data.ssh_keys.map { key ->
                key.copy(
                    private_key = encryptField(key.private_key, masterKey),
                    passphrase = key.passphrase?.let { encryptField(it, masterKey) }
                )
            }
        )
    }

    private fun encrypt(data: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(data)
        return cipher.iv + encrypted
    }

    private fun decrypt(data: ByteArray, key: SecretKey): ByteArray {
        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = data.copyOfRange(GCM_IV_LENGTH, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }
}
