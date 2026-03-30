package jamgmilk.fuwagit.ui.screen.credentials

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.data.local.credential.HttpsCredential
import jamgmilk.fuwagit.data.local.credential.SshKey
import jamgmilk.fuwagit.domain.usecase.CredentialUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CredentialsStoreUiState(
    val isMasterPasswordSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isDecryptionUnlocked: Boolean = false,
    val showUnlockDialog: Boolean = false,
    val passwordHint: String? = null,
    val httpsCredentials: List<HttpsCredential> = emptyList(),
    val sshKeys: List<SshKey> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val exportedData: String? = null,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val importSuccess: Boolean = false
)

@HiltViewModel
class CredentialsStoreViewModel @Inject constructor(
    private val credentialUseCases: CredentialUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsStoreUiState())
    val uiState: StateFlow<CredentialsStoreUiState> = _uiState.asStateFlow()

    fun initialize() {
        val isSet = credentialUseCases.isMasterPasswordSet()
        val isBioEnabled = credentialUseCases.isBiometricEnabled()

        _uiState.value = _uiState.value.copy(
            isMasterPasswordSet = isSet,
            isBiometricEnabled = isBioEnabled,
            passwordHint = credentialUseCases.getMasterPasswordHint()
        )

        loadCredentials()
    }

    fun setupMasterPassword(password: String, confirmPassword: String, hint: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            credentialUseCases.setupMasterPassword(password, confirmPassword, hint)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isMasterPasswordSet = true,
                        isDecryptionUnlocked = true,
                        isLoading = false,
                        passwordHint = hint
                    )
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
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

            credentialUseCases.unlockWithPassword(password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isDecryptionUnlocked = true,
                        showUnlockDialog = false,
                        isLoading = false
                    )
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun unlockWithBiometric(activity: FragmentActivity) {
        _uiState.value = _uiState.value.copy(
            error = "Biometric authentication requires additional setup"
        )
    }

    fun enableBiometric() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            credentialUseCases.enableBiometric()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isBiometricEnabled = true,
                        isLoading = false
                    )
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun disableBiometric() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            credentialUseCases.disableBiometric()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isBiometricEnabled = false,
                        isLoading = false
                    )
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            credentialUseCases.getHttpsCredentials()
                .onSuccess { credentials ->
                    _uiState.value = _uiState.value.copy(httpsCredentials = credentials)
                }

            credentialUseCases.getSshKeys()
                .onSuccess { keys ->
                    _uiState.value = _uiState.value.copy(sshKeys = keys)
                }
        }
    }

    fun addHttpsCredential(host: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            credentialUseCases.addHttpsCredential(host, username, password)
                .onSuccess {
                    loadCredentials()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun updateHttpsCredential(uuid: String, host: String? = null, username: String? = null, password: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            credentialUseCases.updateHttpsCredential(uuid, host, username, password)
                .onSuccess {
                    loadCredentials()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
        }
    }

    fun deleteHttpsCredential(uuid: String) {
        viewModelScope.launch {
            credentialUseCases.deleteHttpsCredential(uuid)
                .onSuccess {
                    loadCredentials()
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    suspend fun getHttpsPassword(uuid: String): String? {
        return when (val result = credentialUseCases.getHttpsPassword(uuid)) {
            is jamgmilk.fuwagit.domain.model.AppResult.Success -> result.data
            is jamgmilk.fuwagit.domain.model.AppResult.Error -> null
        }
    }

    fun addSshKey(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String?,
        fingerprint: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            credentialUseCases.addSshKey(
                name = name,
                type = type,
                publicKey = publicKey,
                privateKey = privateKey,
                passphrase = passphrase,
                fingerprint = fingerprint
            ).onSuccess { uuid ->
                loadCredentials()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onError { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to add SSH key: ${exception.message}"
                )
            }
        }
    }

    fun deleteSshKey(uuid: String) {
        viewModelScope.launch {
            credentialUseCases.deleteSshKey(uuid)
                .onSuccess {
                    loadCredentials()
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    suspend fun getSshPrivateKey(uuid: String): String? {
        return when (val result = credentialUseCases.getSshPrivateKey(uuid)) {
            is jamgmilk.fuwagit.domain.model.AppResult.Success -> result.data
            is jamgmilk.fuwagit.domain.model.AppResult.Error -> null
        }
    }

    fun exportCredentials() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            credentialUseCases.exportCredentials()
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(
                        exportedData = data,
                        showExportDialog = true,
                        isLoading = false
                    )
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Export failed: ${exception.message}"
                    )
                }
        }
    }

    fun importCredentials(jsonData: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            credentialUseCases.importCredentials(jsonData)
                .onSuccess {
                    loadCredentials()
                    _uiState.value = _uiState.value.copy(
                        showImportDialog = false,
                        importSuccess = true,
                        isLoading = false
                    )
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Import failed: ${exception.message}"
                    )
                }
        }
    }

    fun showImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = true, error = null)
    }

    fun hideImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = false)
    }

    fun hideExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false, exportedData = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearImportSuccess() {
        _uiState.value = _uiState.value.copy(importSuccess = false)
    }

    fun isDecryptionUnlocked(): Boolean {
        return credentialUseCases.isUnlocked()
    }

    fun lock() {
        credentialUseCases.lock()
        _uiState.value = _uiState.value.copy(isDecryptionUnlocked = false)
    }
}
