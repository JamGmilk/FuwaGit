package jamgmilk.obsigit.ui.screen.credentials

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jamgmilk.obsigit.credential.store.MasterKeyManager
import jamgmilk.obsigit.credential.store.PublicHttpsCredential
import jamgmilk.obsigit.credential.store.PublicSshKey
import jamgmilk.obsigit.credential.store.SecureCredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.crypto.SecretKey

data class CredentialsStoreUiState(
    val isMasterPasswordSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isUnlocked: Boolean = false,
    val showSetupDialog: Boolean = false,
    val showUnlockDialog: Boolean = false,
    val passwordHint: String? = null,
    val httpsCredentials: List<PublicHttpsCredential> = emptyList(),
    val sshKeys: List<PublicSshKey> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CredentialsStoreViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(CredentialsStoreUiState())
    val uiState: StateFlow<CredentialsStoreUiState> = _uiState.asStateFlow()
    
    private var credentialStore: SecureCredentialStore? = null
    private var masterKeyManager: MasterKeyManager? = null
    private var masterKey: SecretKey? = null
    
    fun initialize(context: Context) {
        credentialStore = SecureCredentialStore(context)
        masterKeyManager = MasterKeyManager(context)
        
        val isSet = masterKeyManager!!.isMasterPasswordSet()
        val isBioEnabled = masterKeyManager!!.isBiometricEnabled()
        
        _uiState.value = _uiState.value.copy(
            isMasterPasswordSet = isSet,
            isBiometricEnabled = isBioEnabled,
            showSetupDialog = !isSet,
            passwordHint = masterKeyManager!!.getPasswordHint()
        )
        
        if (isSet) {
            val cachedKey = credentialStore!!.getCachedMasterKey()
            if (cachedKey != null) {
                masterKey = cachedKey
                loadCredentials()
            } else {
                _uiState.value = _uiState.value.copy(showUnlockDialog = true)
            }
        }
    }
    
    fun setupMasterPassword(password: String, confirmPassword: String, hint: String?) {
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = "Passwords do not match")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = masterKeyManager!!.setupMasterPassword(password)
                if (result.isSuccess) {
                    val key = result.getOrThrow()
                    if (hint != null) {
                        masterKeyManager!!.setPasswordHint(hint)
                    }
                    masterKey = key
                    credentialStore!!.cacheMasterKey(key)
                    
                    _uiState.value = _uiState.value.copy(
                        isMasterPasswordSet = true,
                        isUnlocked = true,
                        showSetupDialog = false,
                        isLoading = false,
                        passwordHint = hint
                    )
                    
                    loadCredentials()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to setup password"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to setup password: ${e.message}"
                )
            }
        }
    }
    
    fun showUnlockDialog() {
        _uiState.value = _uiState.value.copy(showUnlockDialog = true, error = null)
    }
    
    fun hideUnlockDialog() {
        _uiState.value = _uiState.value.copy(showUnlockDialog = false, error = null)
    }
    
    fun unlockWithPassword(password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val result = masterKeyManager!!.unlockWithPassword(password)
                if (result.isSuccess) {
                    val key = result.getOrThrow()
                    masterKey = key
                    credentialStore!!.cacheMasterKey(key)
                    
                    _uiState.value = _uiState.value.copy(
                        isUnlocked = true,
                        showUnlockDialog = false,
                        isLoading = false
                    )
                    
                    loadCredentials()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Incorrect password"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to unlock: ${e.message}"
                )
            }
        }
    }
    
    fun unlockWithBiometric(activity: FragmentActivity) {
        masterKeyManager?.unlockWithBiometric(
            activity,
            onSuccess = { key ->
                masterKey = key
                credentialStore!!.cacheMasterKey(key)
                
                _uiState.value = _uiState.value.copy(
                    isUnlocked = true,
                    showUnlockDialog = false
                )
                
                loadCredentials()
            },
            onError = { error ->
                _uiState.value = _uiState.value.copy(error = error)
            }
        )
    }
    
    private fun loadCredentials() {
        viewModelScope.launch {
            val store = credentialStore ?: return@launch
            val key = masterKey ?: return@launch
            
            val publicData = store.loadPublicData()
            
            _uiState.value = _uiState.value.copy(
                httpsCredentials = publicData.https_credentials,
                sshKeys = publicData.ssh_keys,
                passwordHint = masterKeyManager?.getPasswordHint()
            )
        }
    }
    
    fun addHttpsCredential(host: String, username: String, password: String) {
        viewModelScope.launch {
            val store = credentialStore ?: return@launch
            val key = masterKey ?: return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                store.addHttpsCredential(host, username, password, key)
                loadCredentials()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to add credential: ${e.message}"
                )
            }
        }
    }
    
    fun deleteHttpsCredential(uuid: String) {
        viewModelScope.launch {
            val store = credentialStore ?: return@launch
            val key = masterKey ?: return@launch
            
            try {
                store.deleteHttpsCredential(uuid, key)
                loadCredentials()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete credential: ${e.message}"
                )
            }
        }
    }
    
    suspend fun getHttpsPassword(uuid: String): String? {
        val store = credentialStore ?: return null
        val key = masterKey ?: return null
        return store.getHttpsPassword(uuid, key)
    }
    
    fun addSshKey(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String?,
        fingerprint: String,
        comment: String
    ) {
        viewModelScope.launch {
            val store = credentialStore ?: return@launch
            val key = masterKey ?: return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                store.addSshKey(
                    name = name,
                    type = type,
                    publicKey = publicKey,
                    privateKey = privateKey,
                    passphrase = passphrase,
                    fingerprint = fingerprint,
                    comment = comment,
                    masterKey = key
                )
                loadCredentials()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to add SSH key: ${e.message}"
                )
            }
        }
    }
    
    fun deleteSshKey(uuid: String) {
        viewModelScope.launch {
            val store = credentialStore ?: return@launch
            val key = masterKey ?: return@launch
            
            try {
                store.deleteSshKey(uuid, key)
                loadCredentials()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete SSH key: ${e.message}"
                )
            }
        }
    }
    
    suspend fun getSshPrivateKey(uuid: String): String? {
        val store = credentialStore ?: return null
        val key = masterKey ?: return null
        return store.getSshPrivateKey(uuid, key)
    }
    
    fun lock() {
        masterKey = null
        credentialStore?.clearCachedMasterKey()
        _uiState.value = _uiState.value.copy(
            isUnlocked = false,
            httpsCredentials = emptyList(),
            sshKeys = emptyList()
        )
    }
}
