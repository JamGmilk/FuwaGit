package jamgmilk.fuwagit.ui.screen.credentials

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.usecase.credential.CredentialStoreFacade
import jamgmilk.fuwagit.domain.usecase.git.TestSshConnectionUseCase
import jamgmilk.fuwagit.ui.screen.permissions.SshTestResult
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
    private val credentialFacade: CredentialStoreFacade,
    private val testSshConnectionUseCase: TestSshConnectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsStoreUiState())
    val uiState: StateFlow<CredentialsStoreUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    fun initialize() {
        _uiState.update {
            it.copy(
                isMasterPasswordSet = credentialFacade.isMasterPasswordSet(),
                isBiometricEnabled = credentialFacade.isBiometricEnabled(),
                passwordHint = credentialFacade.getMasterPasswordHint()
            )
        }
        loadCredentials()
    }

    fun setupMasterPassword(password: String, confirmPassword: String, hint: String?) {
        executeWithLoading {
            credentialFacade.setupMasterPassword(password, confirmPassword, hint)
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
            credentialFacade.unlockWithPassword(password)
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
                it.copy(isDecryptionUnlocked = credentialFacade.isUnlocked())
            }

            credentialFacade.getHttpsCredentials()
                .onSuccess { credentials ->
                    _uiState.update { it.copy(httpsCredentials = credentials) }
                }

            credentialFacade.getSshKeys()
                .onSuccess { keys ->
                    _uiState.update { it.copy(sshKeys = keys) }
                }
        }
    }

    fun addHttpsCredential(host: String, username: String, password: String) {
        executeWithLoading {
            credentialFacade.addHttpsCredential(host, username, password)
                .onSuccess { loadCredentials() }
        }
    }

    fun deleteHttpsCredential(uuid: String) {
        executeWithLoading {
            credentialFacade.deleteHttpsCredential(uuid)
                .onSuccess { loadCredentials() }
        }
    }

    fun addSshKey(name: String, type: String, publicKey: String, privateKey: String, passphrase: String?, fingerprint: String) {
        executeWithLoading {
            credentialFacade.addSshKey(name, type, publicKey, privateKey, passphrase, fingerprint)
                .onSuccess { loadCredentials() }
        }
    }

    fun deleteSshKey(uuid: String) {
        executeWithLoading {
            credentialFacade.deleteSshKey(uuid)
                .onSuccess { loadCredentials() }
        }
    }

    fun exportCredentials() {
        executeWithLoading {
            credentialFacade.exportCredentials()
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
            credentialFacade.importCredentials(jsonData)
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
        viewModelScope.launch {
            credentialFacade.enableBiometric(activity) { result ->
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
    }

    fun unlockWithBiometric(activity: FragmentActivity) {
        credentialFacade.unlockWithBiometric(activity) { result ->
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
            credentialFacade.disableBiometric()
            _uiState.update { it.copy(isBiometricEnabled = false) }
        }
    }

    fun lock() {
        credentialFacade.lock()
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
        if (newPassword.length < 6) {
            _uiState.update { it.copy(changePasswordError = "New password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, changePasswordError = null) }

            credentialFacade.changeMasterPassword(oldPassword, newPassword, hint)
                .onSuccess {
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
                .onError { e ->
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
        return credentialFacade.getHttpsPassword(uuid).getOrNull()
    }

    suspend fun getSshPrivateKey(uuid: String): String? {
        return credentialFacade.getSshPrivateKey(uuid).getOrNull()
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

    /**
     * Test SSH connection with the given host and key UUID.
     * Retrieves the decrypted private key from the vault (requires unlock).
     *
     * @param host The SSH host (e.g., "git@github.com")
     * @param sshKeyUuid The UUID of the SSH key to test
     * @param onResult Callback to receive the test result
     */
    fun testSshConnection(
        host: String,
        sshKeyUuid: String,
        onResult: (SshTestResult) -> Unit
    ) {
        viewModelScope.launch {
            onResult(SshTestResult.Testing)

            try {
                val privateKeyResult = credentialFacade.getSshPrivateKey(sshKeyUuid)
                val passphraseResult = credentialFacade.getSshPassphrase(sshKeyUuid)

                privateKeyResult
                    .onSuccess { privateKey ->
                        val passphrase = passphraseResult.getOrNull()
                        testSshConnectionUseCase(host, privateKey, passphrase)
                            .onSuccess { message ->
                                onResult(SshTestResult.Success(message))
                            }
                            .onError { exception ->
                                onResult(SshTestResult.Failure(exception.message ?: "Connection failed"))
                            }
                    }
                    .onError { exception ->
                        onResult(SshTestResult.Failure("Failed to access key: ${exception.message ?: "Unknown error"}"))
                    }
            } catch (e: Exception) {
                onResult(SshTestResult.Failure("Unexpected error: ${e.message ?: "Unknown error"}"))
            }
        }
    }
}
