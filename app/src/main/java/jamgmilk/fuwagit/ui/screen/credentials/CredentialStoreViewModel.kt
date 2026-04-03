package jamgmilk.fuwagit.ui.screen.credentials

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.data.local.security.MasterKeyManager
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import jamgmilk.fuwagit.domain.usecase.credential.AddHttpsCredentialUseCase
import jamgmilk.fuwagit.domain.usecase.credential.AddSshKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.DeleteHttpsCredentialUseCase
import jamgmilk.fuwagit.domain.usecase.credential.DeleteSshKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.EnableBiometricUseCase
import jamgmilk.fuwagit.domain.usecase.credential.ExportCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshKeysUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshPrivateKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.ImportCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.SetupMasterPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.UnlockWithBiometricUseCase
import jamgmilk.fuwagit.domain.usecase.credential.UnlockWithPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.UpdateHttpsCredentialUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.Stable

@Stable
data class CredentialsStoreUiState(
    val isMasterPasswordSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isDecryptionUnlocked: Boolean = false,
    val showUnlockDialog: Boolean = false,
    val showChangePasswordDialog: Boolean = false,
    val changePasswordError: String? = null,
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
class CredentialStoreViewModel @Inject constructor(
    private val setupMasterPasswordUseCase: SetupMasterPasswordUseCase,
    private val unlockWithPasswordUseCase: UnlockWithPasswordUseCase,
    private val getHttpsCredentialsUseCase: GetHttpsCredentialsUseCase,
    private val addHttpsCredentialUseCase: AddHttpsCredentialUseCase,
    private val updateHttpsCredentialUseCase: UpdateHttpsCredentialUseCase,
    private val deleteHttpsCredentialUseCase: DeleteHttpsCredentialUseCase,
    private val getHttpsPasswordUseCase: GetHttpsPasswordUseCase,
    private val getSshKeysUseCase: GetSshKeysUseCase,
    private val addSshKeyUseCase: AddSshKeyUseCase,
    private val deleteSshKeyUseCase: DeleteSshKeyUseCase,
    private val getSshPrivateKeyUseCase: GetSshPrivateKeyUseCase,
    private val exportCredentialsUseCase: ExportCredentialsUseCase,
    private val importCredentialsUseCase: ImportCredentialsUseCase,
    private val enableBiometricUseCase: EnableBiometricUseCase,
    private val unlockWithBiometricUseCase: UnlockWithBiometricUseCase,
    private val masterKeyManager: MasterKeyManager,
    private val credentialRepository: CredentialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsStoreUiState())
    val uiState: StateFlow<CredentialsStoreUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    fun initialize() {
        _uiState.update {
            it.copy(
                isMasterPasswordSet = credentialRepository.isMasterPasswordSet(),
                isBiometricEnabled = credentialRepository.isBiometricEnabled(),
                passwordHint = credentialRepository.getMasterPasswordHint()
            )
        }
        loadCredentials()
    }

    fun setupMasterPassword(password: String, confirmPassword: String, hint: String?) {
        executeWithLoading {
            setupMasterPasswordUseCase(password, confirmPassword, hint)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isMasterPasswordSet = true,
                            showUnlockDialog = false
                        )
                    }
                    loadCredentials()
                }
        }
    }

    fun unlockWithPassword(password: String) {
        executeWithLoading {
            unlockWithPasswordUseCase(password)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDecryptionUnlocked = true,
                            showUnlockDialog = false
                        )
                    }
                    loadCredentials()
                }
        }
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isDecryptionUnlocked = credentialRepository.isUnlocked())
            }

            getHttpsCredentialsUseCase()
                .onSuccess { credentials ->
                    _uiState.update { it.copy(httpsCredentials = credentials) }
                }

            getSshKeysUseCase()
                .onSuccess { keys ->
                    _uiState.update { it.copy(sshKeys = keys) }
                }
        }
    }

    fun addHttpsCredential(host: String, username: String, password: String) {
        executeWithLoading {
            addHttpsCredentialUseCase(host, username, password)
                .onSuccess { loadCredentials() }
        }
    }

    fun updateHttpsCredential(uuid: String, host: String?, username: String?, password: String?) {
        executeWithLoading {
            updateHttpsCredentialUseCase(uuid, host, username, password)
                .onSuccess { loadCredentials() }
        }
    }

    fun deleteHttpsCredential(uuid: String) {
        executeWithLoading {
            deleteHttpsCredentialUseCase(uuid)
                .onSuccess { loadCredentials() }
        }
    }

    fun addSshKey(name: String, type: String, publicKey: String, privateKey: String, passphrase: String?, fingerprint: String) {
        executeWithLoading {
            addSshKeyUseCase(name, type, publicKey, privateKey, passphrase, fingerprint)
                .onSuccess { loadCredentials() }
        }
    }

    fun deleteSshKey(uuid: String) {
        executeWithLoading {
            deleteSshKeyUseCase(uuid)
                .onSuccess { loadCredentials() }
        }
    }

    fun exportCredentials() {
        executeWithLoading {
            exportCredentialsUseCase()
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            exportedData = data,
                            showExportDialog = true
                        )
                    }
                }
        }
    }

    fun importCredentials(jsonData: String) {
        executeWithLoading {
            importCredentialsUseCase(jsonData)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showImportDialog = false,
                            importSuccess = true
                        )
                    }
                    loadCredentials()
                }
        }
    }

    fun enableBiometric(activity: FragmentActivity) {
        enableBiometricUseCase(activity) { result ->
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isBiometricEnabled = true) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun unlockWithBiometric(activity: FragmentActivity) {
        unlockWithBiometricUseCase(activity) { result ->
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isDecryptionUnlocked = true,
                            isBiometricEnabled = true,
                            showUnlockDialog = false
                        )
                    }
                    loadCredentials()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    fun disableBiometric() {
        viewModelScope.launch {
            credentialRepository.disableBiometric()
            _uiState.update { it.copy(isBiometricEnabled = false) }
        }
    }

    fun lock() {
        credentialRepository.lock()
        _uiState.update {
            it.copy(isDecryptionUnlocked = false)
        }
    }

    fun showUnlockDialog() {
        _uiState.update { it.copy(showUnlockDialog = true) }
    }

    fun dismissUnlockDialog() {
        _uiState.update { it.copy(showUnlockDialog = false) }
    }

    fun showExportDialog() {
        _uiState.update { it.copy(showExportDialog = true) }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(showExportDialog = false, exportedData = null) }
    }

    fun showImportDialog() {
        _uiState.update { it.copy(showImportDialog = true) }
    }

    fun dismissImportDialog() {
        _uiState.update { it.copy(showImportDialog = false) }
    }

    fun showChangePasswordDialog() {
        _uiState.update { it.copy(showChangePasswordDialog = true, changePasswordError = null) }
    }

    fun dismissChangePasswordDialog() {
        _uiState.update { it.copy(showChangePasswordDialog = false, changePasswordError = null) }
    }

    fun changeMasterPassword(oldPassword: String, newPassword: String, confirmPassword: String, hint: String?) {
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(changePasswordError = "Passwords do not match") }
            return
        }
        if (newPassword.length < 8) {
            _uiState.update { it.copy(changePasswordError = "New password must be at least 8 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, changePasswordError = null) }
            
            masterKeyManager.changeMasterPassword(oldPassword, newPassword)
                .onSuccess {
                    if (hint != null) {
                        masterKeyManager.setPasswordHint(hint)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showChangePasswordDialog = false,
                            changePasswordError = null,
                            passwordHint = hint,
                            isBiometricEnabled = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            changePasswordError = e.message ?: "Incorrect old password"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    suspend fun getHttpsPassword(uuid: String): String? {
        return getHttpsPasswordUseCase(uuid).getOrNull()
    }

    suspend fun getSshPrivateKey(uuid: String): String? {
        return getSshPrivateKeyUseCase(uuid).getOrNull()
    }

    private inline fun executeWithLoading(crossinline block: suspend () -> AppResult<*>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            block()
                .onError { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
