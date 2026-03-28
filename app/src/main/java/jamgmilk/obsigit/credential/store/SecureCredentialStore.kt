package jamgmilk.obsigit.credential.store

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureCredentialStore(private val context: Context) {
    
    companion object {
        private const val PUBLIC_FILE = "public_data.json"
        private const val PRIVATE_FILE = "private_data.json.enc"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    
    private val publicFile: File by lazy {
        File(context.filesDir, PUBLIC_FILE)
    }
    
    private val privateFile: File by lazy {
        File(context.filesDir, PRIVATE_FILE)
    }
    
    private var cachedMasterKey: WeakReference<SecretKey>? = null
    private var lastUnlockTime: Long = 0
    private val sessionTimeout: Long = 5 * 60 * 1000 // 5 minutes
    
    fun loadPublicData(): PublicCredentialData {
        return try {
            if (!publicFile.exists()) {
                PublicCredentialData()
            } else {
                json.decodeFromString(publicFile.readText())
            }
        } catch (e: Exception) {
            PublicCredentialData()
        }
    }
    
    fun savePublicData(data: PublicCredentialData) {
        val updatedData = data.copy(updated_at = System.currentTimeMillis())
        publicFile.writeText(json.encodeToString(updatedData))
    }
    
    suspend fun loadPrivateData(masterKey: SecretKey): PrivateCredentialData {
        return withContext(Dispatchers.IO) {
            try {
                if (!privateFile.exists()) {
                    PrivateCredentialData()
                } else {
                    val encrypted = privateFile.readBytes()
                    val decrypted = decrypt(encrypted, masterKey)
                    json.decodeFromString(String(decrypted, StandardCharsets.UTF_8))
                }
            } catch (e: Exception) {
                PrivateCredentialData()
            }
        }
    }
    
    suspend fun savePrivateData(data: PrivateCredentialData, masterKey: SecretKey) {
        withContext(Dispatchers.IO) {
            val jsonString = json.encodeToString(data)
            val encrypted = encrypt(jsonString.toByteArray(StandardCharsets.UTF_8), masterKey)
            privateFile.writeBytes(encrypted)
        }
    }
    
    fun cacheMasterKey(key: SecretKey) {
        cachedMasterKey = WeakReference(key)
        lastUnlockTime = System.currentTimeMillis()
    }
    
    fun getCachedMasterKey(): SecretKey? {
        val key = cachedMasterKey?.get()
        if (key != null && System.currentTimeMillis() - lastUnlockTime < sessionTimeout) {
            return key
        }
        cachedMasterKey = null
        return null
    }
    
    fun clearCachedMasterKey() {
        cachedMasterKey = null
        lastUnlockTime = 0
    }
    
    fun isSessionValid(): Boolean {
        return cachedMasterKey?.get() != null && 
               System.currentTimeMillis() - lastUnlockTime < sessionTimeout
    }
    
    suspend fun exportAll(masterKey: SecretKey): String {
        return withContext(Dispatchers.IO) {
            val publicData = loadPublicData()
            val privateData = loadPrivateData(masterKey)
            
            json.encodeToString(
                ExportData(
                    public_data = publicData,
                    private_data = privateData,
                    exported_at = System.currentTimeMillis()
                )
            )
        }
    }
    
    suspend fun importAll(jsonString: String, masterKey: SecretKey): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val data = json.decodeFromString<ExportData>(jsonString)
                savePublicData(data.public_data)
                savePrivateData(data.private_data, masterKey)
            }
        }
    }
    
    // HTTPS Credential Operations
    suspend fun addHttpsCredential(
        host: String,
        username: String,
        password: String? = null,
        pat: String? = null,
        masterKey: SecretKey
    ): String {
        val id = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        val publicData = loadPublicData()
        val publicCred = PublicHttpsCredential(
            id = id,
            host = host,
            username = username,
            created_at = now,
            updated_at = now,
            has_password = password != null,
            has_pat = pat != null
        )
        
        val privateData = loadPrivateData(masterKey)
        val privateCred = PrivateHttpsCredential(
            id = id,
            password = password,
            pat = pat
        )
        
        savePublicData(publicData.copy(
            https_credentials = publicData.https_credentials + publicCred
        ))
        
        savePrivateData(privateData.copy(
            https_credentials = privateData.https_credentials + privateCred
        ), masterKey)
        
        return id
    }
    
    suspend fun updateHttpsCredential(
        id: String,
        host: String? = null,
        username: String? = null,
        password: String? = null,
        pat: String? = null,
        masterKey: SecretKey
    ) {
        val now = System.currentTimeMillis()
        
        val publicData = loadPublicData()
        val existingPublicCred = publicData.https_credentials.find { it.id == id } ?: return
        val publicCred = existingPublicCred.copy(
            host = host ?: existingPublicCred.host,
            username = username ?: existingPublicCred.username,
            updated_at = now,
            has_password = password != null || existingPublicCred.has_password,
            has_pat = pat != null || existingPublicCred.has_pat
        )
        
        val privateData = loadPrivateData(masterKey)
        val existingPrivateCred = privateData.https_credentials.find { it.id == id } ?: return
        val privateCred = existingPrivateCred.copy(
            password = password ?: existingPrivateCred.password,
            pat = pat ?: existingPrivateCred.pat
        )
        
        savePublicData(publicData.copy(
            https_credentials = publicData.https_credentials.map { 
                if (it.id == id) publicCred else it 
            }
        ))
        
        savePrivateData(privateData.copy(
            https_credentials = privateData.https_credentials.map { 
                if (it.id == id) privateCred else it 
            }
        ), masterKey)
    }
    
    suspend fun deleteHttpsCredential(id: String, masterKey: SecretKey) {
        val publicData = loadPublicData()
        val privateData = loadPrivateData(masterKey)
        
        savePublicData(publicData.copy(
            https_credentials = publicData.https_credentials.filter { it.id != id }
        ))
        
        savePrivateData(privateData.copy(
            https_credentials = privateData.https_credentials.filter { it.id != id }
        ), masterKey)
    }
    
    // SSH Key Operations
    suspend fun addSshKey(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String? = null,
        fingerprint: String,
        comment: String = "",
        masterKey: SecretKey
    ): String {
        val id = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        val publicData = loadPublicData()
        val newPublicKey = PublicSshKey(
            id = id,
            name = name,
            type = type,
            public_key = publicKey,
            fingerprint = fingerprint,
            comment = comment,
            created_at = now,
            has_passphrase = passphrase != null,
            has_private_key = true
        )
        
        val privateData = loadPrivateData(masterKey)
        val newPrivateKey = PrivateSshKey(
            id = id,
            private_key = privateKey,
            passphrase = passphrase
        )
        
        savePublicData(publicData.copy(
            ssh_keys = publicData.ssh_keys + newPublicKey
        ))
        
        savePrivateData(privateData.copy(
            ssh_keys = privateData.ssh_keys + newPrivateKey
        ), masterKey)
        
        return id
    }
    
    suspend fun deleteSshKey(id: String, masterKey: SecretKey) {
        val publicData = loadPublicData()
        val privateData = loadPrivateData(masterKey)
        
        savePublicData(publicData.copy(
            ssh_keys = publicData.ssh_keys.filter { it.id != id }
        ))
        
        savePrivateData(privateData.copy(
            ssh_keys = privateData.ssh_keys.filter { it.id != id }
        ), masterKey)
    }
    
    suspend fun getHttpsPassword(id: String, masterKey: SecretKey): String? {
        val privateData = loadPrivateData(masterKey)
        return privateData.https_credentials.find { it.id == id }?.password
    }
    
    suspend fun getHttpsPat(id: String, masterKey: SecretKey): String? {
        val privateData = loadPrivateData(masterKey)
        return privateData.https_credentials.find { it.id == id }?.pat
    }
    
    suspend fun getSshPrivateKey(id: String, masterKey: SecretKey): String? {
        val privateData = loadPrivateData(masterKey)
        return privateData.ssh_keys.find { it.id == id }?.private_key
    }
    
    suspend fun getSshPassphrase(id: String, masterKey: SecretKey): String? {
        val privateData = loadPrivateData(masterKey)
        return privateData.ssh_keys.find { it.id == id }?.passphrase
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
