package jamgmilk.obsigit.ui.screen.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jamgmilk.obsigit.domain.model.HttpsCredential
import jamgmilk.obsigit.credential.SshKeyInfo
import jamgmilk.obsigit.credential.SshKeyType
import jamgmilk.obsigit.domain.usecase.credential.GetCredentialsUseCase
import jamgmilk.obsigit.domain.usecase.credential.ManageSshKeysUseCase
import jamgmilk.obsigit.domain.usecase.credential.SaveCredentialUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CredentialsViewModel(
    private val saveCredentialUseCase: SaveCredentialUseCase,
    private val getCredentialsUseCase: GetCredentialsUseCase,
    private val manageSshKeysUseCase: ManageSshKeysUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CredentialsUiState())
    val uiState: StateFlow<CredentialsUiState> = _uiState.asStateFlow()
    
    init {
        loadCredentials()
    }
    
    fun loadCredentials() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val httpsResult = getCredentialsUseCase.getAll()
            val sshResult = manageSshKeysUseCase.getAll()
            val keyStoreAvailable = getCredentialsUseCase.isKeyStoreAvailable()
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    httpsCredentials = httpsResult.getOrNull() ?: emptyList(),
                    sshKeys = sshResult.getOrNull() ?: emptyList(),
                    keyStoreAvailable = keyStoreAvailable,
                    error = httpsResult.exceptionOrNull()?.message ?: sshResult.exceptionOrNull()?.message
                )
            }
        }
    }
    
    fun saveHttpsCredential(credential: HttpsCredential) {
        viewModelScope.launch {
            val result = saveCredentialUseCase(credential)
            if (result.isSuccess) {
                loadCredentials()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun updateHttpsCredential(credential: HttpsCredential) {
        viewModelScope.launch {
            val result = saveCredentialUseCase.update(credential)
            if (result.isSuccess) {
                loadCredentials()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun deleteHttpsCredential(id: String) {
        viewModelScope.launch {
            val result = getCredentialsUseCase.delete(id)
            if (result.isSuccess) {
                loadCredentials()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun generateSshKey(name: String, type: SshKeyType, comment: String) {
        viewModelScope.launch {
            val result = manageSshKeysUseCase.generate(name, type, comment)
            if (result.isSuccess) {
                loadCredentials()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun importSshKey(name: String, privateKey: String, publicKey: String?, passphrase: String?) {
        viewModelScope.launch {
            val result = manageSshKeysUseCase.import(name, privateKey, publicKey, passphrase)
            if (result.isSuccess) {
                loadCredentials()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun deleteSshKey(id: String) {
        viewModelScope.launch {
            val result = manageSshKeysUseCase.delete(id)
            if (result.isSuccess) {
                loadCredentials()
            } else {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }
    
    fun exportSshPublicKey(id: String): String? {
        var result: String? = null
        viewModelScope.launch {
            val exportResult = manageSshKeysUseCase.exportPublicKey(id)
            result = exportResult.getOrNull()
        }
        return result
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
