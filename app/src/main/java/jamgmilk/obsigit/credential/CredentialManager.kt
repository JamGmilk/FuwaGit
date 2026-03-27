package jamgmilk.obsigit.credential

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import androidx.core.content.edit

data class HttpsCredential(
    val id: String,
    val host: String,
    val username: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis()
)

class CredentialManager(private val context: Context) {
    
    companion object {
        private const val PREFS_FILE_NAME = "secure_credentials"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "obsigit_master_key"
        
        private const val KEY_HOST_PREFIX = "host_"
        private const val KEY_USERNAME_PREFIX = "user_"
        private const val KEY_PASSWORD_PREFIX = "pass_"
        private const val KEY_CREATED_PREFIX = "created_"
        private const val KEY_CREDENTIAL_IDS = "credential_ids"
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    suspend fun saveCredential(credential: HttpsCredential): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val id = credential.id.ifEmpty { 
                java.util.UUID.randomUUID().toString() 
            }
            val ids = getCredentialIds().toMutableSet()
            ids.add(id)
            
            val editor = encryptedPrefs.edit()
            editor.putStringSet(KEY_CREDENTIAL_IDS, HashSet(ids))
            editor.putString("$KEY_HOST_PREFIX$id", credential.host)
            editor.putString("$KEY_USERNAME_PREFIX$id", credential.username)
            editor.putString("$KEY_PASSWORD_PREFIX$id", credential.password)
            editor.putLong("$KEY_CREATED_PREFIX$id", credential.createdAt)
            
            if (!editor.commit()) {
                throw Exception("Failed to save credential to encrypted storage")
            }
        }
    }
    
    suspend fun getCredential(id: String): Result<HttpsCredential?> = withContext(Dispatchers.IO) {
        runCatching {
            if (!getCredentialIds().contains(id)) return@runCatching null
            
            HttpsCredential(
                id = id,
                host = encryptedPrefs.getString("$KEY_HOST_PREFIX$id", "") ?: "",
                username = encryptedPrefs.getString("$KEY_USERNAME_PREFIX$id", "") ?: "",
                password = encryptedPrefs.getString("$KEY_PASSWORD_PREFIX$id", "") ?: "",
                createdAt = encryptedPrefs.getLong("$KEY_CREATED_PREFIX$id", 0L)
            )
        }
    }
    
    suspend fun getAllCredentials(): Result<List<HttpsCredential>> = withContext(Dispatchers.IO) {
        runCatching {
            getCredentialIds().mapNotNull { id ->
                getCredential(id).getOrNull()
            }
        }
    }
    
    suspend fun deleteCredential(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ids = getCredentialIds().toMutableSet()
            ids.remove(id)
            
            val editor = encryptedPrefs.edit()
            editor.putStringSet(KEY_CREDENTIAL_IDS, HashSet(ids))
            editor.remove("$KEY_HOST_PREFIX$id")
            editor.remove("$KEY_USERNAME_PREFIX$id")
            editor.remove("$KEY_PASSWORD_PREFIX$id")
            editor.remove("$KEY_CREATED_PREFIX$id")
            
            if (!editor.commit()) {
                throw Exception("Failed to delete credential from encrypted storage")
            }
        }
    }
    
    suspend fun updateCredential(credential: HttpsCredential): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!getCredentialIds().contains(credential.id)) {
                throw IllegalArgumentException("Credential not found: ${credential.id}")
            }
            
            val editor = encryptedPrefs.edit()
            editor.putString("$KEY_HOST_PREFIX${credential.id}", credential.host)
            editor.putString("$KEY_USERNAME_PREFIX${credential.id}", credential.username)
            editor.putString("$KEY_PASSWORD_PREFIX${credential.id}", credential.password)
            
            if (!editor.commit()) {
                throw Exception("Failed to update credential in encrypted storage")
            }
        }
    }
    
    suspend fun getCredentialByHost(host: String): Result<HttpsCredential?> = withContext(Dispatchers.IO) {
        runCatching {
            getAllCredentials().getOrNull()?.find { 
                it.host.equals(host, ignoreCase = true) 
            }
        }
    }
    
    private fun getCredentialIds(): Set<String> {
        return encryptedPrefs.getStringSet(KEY_CREDENTIAL_IDS, emptySet()) ?: emptySet()
    }
    
    fun isKeyStoreAvailable(): Boolean {
        return try {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            true
        } catch (e: Exception) {
            false
        }
    }
}
