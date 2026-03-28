package jamgmilk.obsigit.ui.screen.credentials

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jamgmilk.obsigit.credential.store.MasterKeyManager
import jamgmilk.obsigit.credential.store.PublicCredentialData
import jamgmilk.obsigit.credential.store.PublicHttpsCredential
import jamgmilk.obsigit.credential.store.PublicSshKey
import jamgmilk.obsigit.credential.store.SecureCredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.crypto.SecretKey

data class CredentialsStoreUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    val isInitialized: Boolean = false,
    val isMasterPasswordSet: Boolean = false,
    val isUnlocked: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    
    val showSetupDialog: Boolean = false,
    val showUnlockDialog: Boolean = false,
    
    val publicData: PublicCredentialData = PublicCredentialData(),
    val httpsCredentials: List<PublicHttpsCredential> = emptyList(),
    val sshKeys: List<PublicSshKey> = emptyList(),
    
    val passwordHint: String? = null
)

class CredentialsStoreViewModel : ViewModel() {
    
    private lateinit var masterKeyManager: MasterKeyManager
    private lateinit var credentialStore: SecureCredentialStore
    private var masterKey: SecretKey? = null
    
    private val _uiState = MutableStateFlow(CredentialsStoreUiState())
    val uiState: StateFlow<CredentialsStoreUiState> = _uiState.asStateFlow()
    
    fun initialize(context: android.content.Context) {
        if (_uiState.value.isInitialized) return
        
        masterKeyManager = MasterKeyManager(context.applicationContext)
        credentialStore = SecureCredentialStore(context.applicationContext)
        
        val isPasswordSet = masterKeyManager.isMasterPasswordSet()
        val isBiometricEnabled = masterKeyManager.isBiometricEnabled()
        val passwordHint = masterKeyManager.getPasswordHint()
        
        val publicData = credentialStore.loadPublicData()
        
        _uiState.update { 
            it.copy(
                isInitialized = true,
                isMasterPasswordSet = isPasswordSet,
                isBiometricEnabled = isBiometricEnabled,
                publicData = publicData,
                httpsCredentials = publicData.https_credentials,
                sshKeys = publicData.ssh_keys,
                passwordHint = passwordHint,
                showSetupDialog = !isPasswordSet
            )
        }
    }
    
    fun setupMasterPassword(password: String, confirmPassword: String, hint: String?) {
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }
        
        if (password.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            masterKeyManager.setupMasterPassword(password)
                .onSuccess { key ->
                    masterKey = key
                    credentialStore.cacheMasterKey(key)
                    
                    if (!hint.isNullOrBlank()) {
                        masterKeyManager.setPasswordHint(hint)
                    }
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isMasterPasswordSet = true,
                            isUnlocked = true,
                            showSetupDialog = false,
                            passwordHint = hint
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
        }
    }
    
    fun unlockWithPassword(password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            masterKeyManager.unlockWithPassword(password)
                .onSuccess { key ->
                    masterKey = key
                    credentialStore.cacheMasterKey(key)
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isUnlocked = true,
                            showUnlockDialog = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Invalid password"
                        )
                    }
                }
        }
    }
    
    fun unlockWithBiometric(activity: FragmentActivity) {
        masterKeyManager.unlockWithBiometric(
            activity = activity,
            onSuccess = { key ->
                masterKey = key
                credentialStore.cacheMasterKey(key)
                
                _uiState.update { 
                    it.copy(
                        isUnlocked = true,
                        showUnlockDialog = false
                    )
                }
            },
            onError = { error ->
                _uiState.update { it.copy(error = error) }
            }
        )
    }
    
    fun enableBiometric(activity: FragmentActivity) {
        val key = masterKey ?: return
        
        masterKeyManager.enableBiometric(
            activity = activity,
            masterKey = key,
            onSuccess = {
                _uiState.update { it.copy(isBiometricEnabled = true) }
            },
            onError = { error ->
                _uiState.update { it.copy(error = error) }
            }
        )
    }
    
    fun disableBiometric() {
        masterKeyManager.disableBiometric()
        _uiState.update { it.copy(isBiometricEnabled = false) }
    }
    
    fun showUnlockDialog() {
        _uiState.update { it.copy(showUnlockDialog = true) }
    }
    
    fun hideUnlockDialog() {
        _uiState.update { it.copy(showUnlockDialog = false) }
    }
    
    fun lock() {
        masterKey = null
        credentialStore.clearCachedMasterKey()
        _uiState.update { it.copy(isUnlocked = false) }
    }
    
    fun addHttpsCredential(
        host: String,
        username: String,
        password: String?,
        pat: String?
    ) {
        val key = masterKey ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            credentialStore.addHttpsCredential(
                host = host,
                username = username,
                password = password,
                pat = pat,
                masterKey = key
            )
            
            refreshPublicData()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun updateHttpsCredential(
        id: String,
        host: String?,
        username: String?,
        password: String?,
        pat: String?
    ) {
        val key = masterKey ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            credentialStore.updateHttpsCredential(
                id = id,
                host = host,
                username = username,
                password = password,
                pat = pat,
                masterKey = key
            )
            
            refreshPublicData()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun deleteHttpsCredential(id: String) {
        val key = masterKey ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            credentialStore.deleteHttpsCredential(id, key)
            
            refreshPublicData()
            _uiState.update { it.copy(isLoading = false) }
        }
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
        val key = masterKey ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            credentialStore.addSshKey(
                name = name,
                type = type,
                publicKey = publicKey,
                privateKey = privateKey,
                passphrase = passphrase,
                fingerprint = fingerprint,
                comment = comment,
                masterKey = key
            )
            
            refreshPublicData()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun deleteSshKey(id: String) {
        val key = masterKey ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            credentialStore.deleteSshKey(id, key)
            
            refreshPublicData()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    suspend fun getHttpsPassword(id: String): String? {
        val key = masterKey ?: return null
        return credentialStore.getHttpsPassword(id, key)
    }
    
    suspend fun getHttpsPat(id: String): String? {
        val key = masterKey ?: return null
        return credentialStore.getHttpsPat(id, key)
    }
    
    suspend fun getSshPrivateKey(id: String): String? {
        val key = masterKey ?: return null
        return credentialStore.getSshPrivateKey(id, key)
    }
    
    suspend fun getSshPassphrase(id: String): String? {
        val key = masterKey ?: return null
        return credentialStore.getSshPassphrase(id, key)
    }
    
    fun exportAll(onResult: (String) -> Unit) {
        val key = masterKey ?: return
        
        viewModelScope.launch {
            val exported = credentialStore.exportAll(key)
            onResult(exported)
        }
    }
    
    fun importAll(json: String) {
        val key = masterKey ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            credentialStore.importAll(json, key)
            
            refreshPublicData()
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    private fun refreshPublicData() {
        val publicData = credentialStore.loadPublicData()
        _uiState.update { 
            it.copy(
                publicData = publicData,
                httpsCredentials = publicData.https_credentials,
                sshKeys = publicData.ssh_keys
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun isSessionValid(): Boolean {
        return credentialStore.isSessionValid()
    }
}
