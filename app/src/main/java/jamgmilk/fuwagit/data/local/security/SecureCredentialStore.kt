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
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private class SecureSecretKey(private val keyBytes: ByteArray, private val algorithm: String) : SecretKey {
    override fun getAlgorithm(): String = algorithm
    override fun getFormat(): String = "RAW"
    override fun getEncoded(): ByteArray = keyBytes
    fun secureZero() {
        java.util.Arrays.fill(keyBytes, 0.toByte())
    }
}

@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferencesStore: jamgmilk.fuwagit.data.local.prefs.AppPreferencesStore
) {

    companion object {
        private const val DATA_FILE = "credential_data.json"
        private const val ENCRYPTED_MARKER = "ENC:AES_GCM:"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val HMAC_KEY_ALIAS = "fuwagit_credential_hmac_key"
        private const val HMAC_KEY_SIZE = 32
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val dataFile: File by lazy {
        File(context.filesDir, DATA_FILE)
    }

    private var cachedMasterKey: SecretKey? = null
    private var lastUnlockTime: Long = 0
    private var isBiometricSession: Boolean = false
    private val sessionLock = Any()
    private val fileLock = Any()

    private val hmacKey: SecretKey by lazy {
        getOrCreateHmacKey()
    }

    private fun getOrCreateHmacKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return if (keyStore.containsAlias(HMAC_KEY_ALIAS)) {
            keyStore.getKey(HMAC_KEY_ALIAS, null) as SecretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
                "AndroidKeyStore"
            )
            val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                HMAC_KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_SIGN or android.security.keystore.KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(HMAC_KEY_SIZE * 8)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun computeHmac(data: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(hmacKey)
        val hmacBytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
    }

    private fun verifyHmac(data: String, expectedHmac: String): Boolean {
        return try {
            val computed = computeHmac(data)
            computed == expectedHmac
        } catch (_: Exception) {
            false
        }
    }

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
        synchronized(fileLock) {
            return try {
                if (!dataFile.exists()) {
                    CredentialData()
                } else {
                    val content = dataFile.readText()
                    if (content.isBlank()) {
                        CredentialData()
                    } else {
                        val lines = content.lines()
                        if (lines.size >= 2 && lines[0].length == 44) {
                            val hmac = lines[0]
                            val jsonContent = lines.drop(1).joinToString("\n")
                            if (verifyHmac(jsonContent, hmac)) {
                                json.decodeFromString(jsonContent)
                            } else {
                                CredentialData()
                            }
                        } else {
                            json.decodeFromString(content)
                        }
                    }
                }
            } catch (_: Exception) {
                CredentialData()
            }
        }
    }

    fun saveCredentialData(data: CredentialData) {
        synchronized(fileLock) {
            val updatedData = data.copy(updatedAt = System.currentTimeMillis())
            val jsonString = json.encodeToString(updatedData)
            val hmac = computeHmac(jsonString)

            val tempFile = File(context.filesDir, "$DATA_FILE.tmp")
            try {
                tempFile.writeText("$hmac\n$jsonString")
                if (!tempFile.renameTo(dataFile)) {
                    tempFile.copyTo(dataFile, overwrite = true)
                    tempFile.delete()
                }
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        }
    }

    fun getPublicCredentials(): List<HttpsCredential> {
        return loadCredentialData().httpsCredentials
    }

    fun getPublicSshKeys(): List<SshKey> {
        return loadCredentialData().sshKeys
    }

    fun cacheMasterKey(key: SecretKey) {
        synchronized(sessionLock) {
            secureClearCachedKey()
            val secureKey = SecureSecretKey(key.encoded, key.algorithm)
            cachedMasterKey = secureKey
            lastUnlockTime = System.currentTimeMillis()
            isBiometricSession = false
        }
    }

    fun cacheMasterKeyFromBiometric(key: SecretKey) {
        synchronized(sessionLock) {
            secureClearCachedKey()
            val secureKey = SecureSecretKey(key.encoded, key.algorithm)
            cachedMasterKey = secureKey
            lastUnlockTime = System.currentTimeMillis()
            isBiometricSession = true
        }
    }

    private fun secureClearCachedKey() {
        (cachedMasterKey as? SecureSecretKey)?.secureZero()
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

            val effectiveTimeout = if (isBiometricSession) sessionTimeout else 0L

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
                validateImportData(exportData)
                val importedData = encryptAllSecrets(exportData.credentialData, masterKey)
                val existingData = loadCredentialData()
                val mergedData = mergeCredentialData(existingData, importedData)
                saveCredentialData(mergedData)
            }
        }
    }

    private fun validateImportData(exportData: ExportData) {
        val data = exportData.credentialData

        if (data.httpsCredentials.size > 1000) {
            throw SecurityException("Too many HTTPS credentials: ${data.httpsCredentials.size}")
        }

        if (data.sshKeys.size > 1000) {
            throw SecurityException("Too many SSH keys: ${data.sshKeys.size}")
        }

        for (cred in data.httpsCredentials) {
            validateUuid(cred.uuid, "HTTPS credential")
            if (cred.host.length > 500) {
                throw SecurityException("HTTPS credential host too long: ${cred.host}")
            }
            if (cred.username.length > 500) {
                throw SecurityException("HTTPS credential username too long: ${cred.username}")
            }
            if (cred.password.isEmpty()) {
                throw SecurityException("HTTPS credential password is empty for: ${cred.uuid}")
            }
            if (cred.password.length > 10000) {
                throw SecurityException("HTTPS credential password too long for: ${cred.uuid}")
            }
        }

        for (key in data.sshKeys) {
            validateUuid(key.uuid, "SSH key")
            if (key.name.isEmpty() || key.name.length > 200) {
                throw SecurityException("SSH key name invalid: ${key.name}")
            }
            if (key.type.isEmpty() || key.type.length > 50) {
                throw SecurityException("SSH key type invalid: ${key.type}")
            }
            if (key.publicKey.isEmpty() || key.publicKey.length > 10000) {
                throw SecurityException("SSH public key invalid for key: ${key.uuid}")
            }
            if (key.privateKey.isEmpty()) {
                throw SecurityException("SSH private key is empty for: ${key.uuid}")
            }
            if (key.privateKey.length > 50000) {
                throw SecurityException("SSH private key too long for: ${key.uuid}")
            }
            if (key.fingerprint.isEmpty() || key.fingerprint.length > 200) {
                throw SecurityException("SSH fingerprint invalid for key: ${key.uuid}")
            }
        }
    }

    private fun validateUuid(uuid: String, context: String) {
        if (uuid.isEmpty() || uuid.length > 100) {
            throw SecurityException("$context has invalid UUID: $uuid")
        }
        if (!uuid.matches(Regex("^[a-zA-Z0-9-_]+$"))) {
            throw SecurityException("$context has malformed UUID: $uuid")
        }
    }

    private fun mergeCredentialData(existing: CredentialData, imported: CredentialData): CredentialData {
        val mergedHttpsCredentials = mergeCredentialLists(
            existing.httpsCredentials,
            imported.httpsCredentials
        )
        val mergedSshKeys = mergeSshKeyLists(
            existing.sshKeys,
            imported.sshKeys
        )
        return existing.copy(
            httpsCredentials = mergedHttpsCredentials,
            sshKeys = mergedSshKeys,
            updatedAt = System.currentTimeMillis()
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
                cred.updatedAt > existingCred.updatedAt -> {
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
                key.createdAt > existingKey.createdAt -> {
                    mergedMap[key.uuid] = key
                }
            }
        }
        return mergedMap.values.toList()
    }

    fun addHttpsCredential(
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
            createdAt = now,
            updatedAt = now
        )

        saveCredentialData(data.copy(
            httpsCredentials = data.httpsCredentials + newCredential
        ))

        return uuid
    }

    fun updateHttpsCredential(
        uuid: String,
        host: String? = null,
        username: String? = null,
        password: String? = null,
        masterKey: SecretKey
    ) {
        val now = System.currentTimeMillis()

        val data = loadCredentialData()
        val existingCred = data.httpsCredentials.find { it.uuid == uuid } ?: return

        val updatedCred = existingCred.copy(
            host = host ?: existingCred.host,
            username = username ?: existingCred.username,
            password = if (password != null) encryptField(password, masterKey) else existingCred.password,
            updatedAt = now
        )

        saveCredentialData(data.copy(
            httpsCredentials = data.httpsCredentials.map {
                if (it.uuid == uuid) updatedCred else it
            }
        ))
    }

    fun deleteHttpsCredential(uuid: String) {
        val data = loadCredentialData()
        saveCredentialData(data.copy(
            httpsCredentials = data.httpsCredentials.filter { it.uuid != uuid }
        ))
    }

    fun addSshKey(
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
            publicKey = publicKey,
            privateKey = encryptedPrivateKey,
            passphrase = encryptedPassphrase,
            fingerprint = fingerprint,
            createdAt = now
        )

        saveCredentialData(data.copy(
            sshKeys = data.sshKeys + newKey
        ))

        return uuid
    }

    fun deleteSshKey(uuid: String) {
        val data = loadCredentialData()
        saveCredentialData(data.copy(
            sshKeys = data.sshKeys.filter { it.uuid != uuid }
        ))
    }

    fun getHttpsPassword(uuid: String, masterKey: SecretKey): String? {
        val data = loadCredentialData()
        val cred = data.httpsCredentials.find { it.uuid == uuid } ?: return null
        return decryptField(cred.password, masterKey)
    }

    fun getSshPrivateKey(uuid: String, masterKey: SecretKey): String? {
        val data = loadCredentialData()
        val key = data.sshKeys.find { it.uuid == uuid } ?: return null
        return decryptField(key.privateKey, masterKey)
    }

    fun getSshPassphrase(uuid: String, masterKey: SecretKey): String? {
        val data = loadCredentialData()
        val key = data.sshKeys.find { it.uuid == uuid } ?: return null
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
                throw SecurityException("Unexpected plaintext credential detected")
            } else {
                val encoded = value.substring(ENCRYPTED_MARKER.length)
                val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
                val decrypted = decrypt(encrypted, key)
                String(decrypted, StandardCharsets.UTF_8)
            }
        } catch (e: SecurityException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun decryptAllSecrets(data: CredentialData, masterKey: SecretKey): CredentialData {
        val failedCredentials = mutableListOf<String>()
        val failedKeys = mutableListOf<String>()

        val decryptedCredentials = data.httpsCredentials.map { cred ->
            val decryptedPassword = decryptField(cred.password, masterKey)
            if (decryptedPassword == null) {
                failedCredentials.add(cred.uuid)
            }
            cred.copy(password = decryptedPassword ?: cred.password)
        }

        val decryptedKeys = data.sshKeys.map { key ->
            val decryptedPrivateKey = decryptField(key.privateKey, masterKey)
            val decryptedPassphrase = key.passphrase?.let { decryptField(it, masterKey) }
            if (decryptedPrivateKey == null) {
                failedKeys.add(key.uuid)
            }
            key.copy(
                privateKey = decryptedPrivateKey ?: key.privateKey,
                passphrase = decryptedPassphrase ?: key.passphrase
            )
        }

        if (failedCredentials.isNotEmpty() || failedKeys.isNotEmpty()) {
            throw jamgmilk.fuwagit.core.result.AppException.DecryptionFailed(
                "Decryption failed for credentials: $failedCredentials, sshKeys: $failedKeys"
            )
        }

        return data.copy(
            httpsCredentials = decryptedCredentials,
            sshKeys = decryptedKeys
        )
    }

    private fun encryptAllSecrets(data: CredentialData, masterKey: SecretKey): CredentialData {
        return data.copy(
            httpsCredentials = data.httpsCredentials.map { cred ->
                cred.copy(password = encryptField(cred.password, masterKey))
            },
            sshKeys = data.sshKeys.map { key ->
                key.copy(
                    privateKey = encryptField(key.privateKey, masterKey),
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
