package jamgmilk.fuwagit.ui.screen.credentials

import androidx.compose.runtime.Stable
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CredentialStoreEvent {
    data object CredentialAdded : CredentialStoreEvent()
    data object CredentialDeleted : CredentialStoreEvent()
    data object CredentialExported : CredentialStoreEvent()
    data object CredentialImported : CredentialStoreEvent()
    data object BiometricEnabled : CredentialStoreEvent()
    data object UnlockSuccess : CredentialStoreEvent()
    data class Error(val message: String) : CredentialStoreEvent()
}

@Stable
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
    val exportedData: String? = null
)

@HiltViewModel
    class CredentialStoreViewModel @Inject constructor(
    private val credentialFacade: CredentialStoreFacade,
    private val testSshConnectionUseCase: TestSshConnectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsStoreUiState())
    val uiState: StateFlow<CredentialsStoreUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CredentialStoreEvent>()
    val events: SharedFlow<CredentialStoreEvent> = _events.asSharedFlow()

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
                    viewModelScope.launch { _events.emit(CredentialStoreEvent.UnlockSuccess) }
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
                .onSuccess {
                    _events.emit(CredentialStoreEvent.CredentialAdded)
                    loadCredentials()
                }
        }
    }

    fun deleteHttpsCredential(uuid: String) {
        executeWithLoading {
            credentialFacade.deleteHttpsCredential(uuid)
                .onSuccess {
                    _events.emit(CredentialStoreEvent.CredentialDeleted)
                    loadCredentials()
                }
        }
    }

    fun addSshKey(name: String, type: String, publicKey: String, privateKey: String, passphrase: String?, fingerprint: String) {
        executeWithLoading {
            credentialFacade.addSshKey(name, type, publicKey, privateKey, passphrase, fingerprint)
                .onSuccess {
                    _events.emit(CredentialStoreEvent.CredentialAdded)
                    loadCredentials()
                }
        }
    }

    fun deleteSshKey(uuid: String) {
        executeWithLoading {
            credentialFacade.deleteSshKey(uuid)
                .onSuccess {
                    _events.emit(CredentialStoreEvent.CredentialDeleted)
                    loadCredentials()
                }
        }
    }

    fun exportCredentials() {
        executeWithLoading {
            credentialFacade.exportCredentials()
                .onSuccess { data ->
                    _uiState.update {
                        it.copy(exportedData = data)
                    }
                    _events.emit(CredentialStoreEvent.CredentialExported)
                }
        }
    }

    fun importCredentials(jsonData: String) {
        executeWithLoading {
            credentialFacade.importCredentials(jsonData)
                .onSuccess {
                    _events.emit(CredentialStoreEvent.CredentialImported)
                    loadCredentials()
                }
        }
    }

    fun enableBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ) {
        viewModelScope.launch {
            credentialFacade.enableBiometric(activity, title, subtitle, negativeButtonText)
                .onSuccess {
                    _uiState.update { it.copy(isBiometricEnabled = true) }
                    _events.emit(CredentialStoreEvent.BiometricEnabled)
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message ?: "Biometric error") }
                    _events.emit(CredentialStoreEvent.Error(e.message ?: "Biometric error"))
                }
        }
    }

    fun unlockWithBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ) {
        viewModelScope.launch {
            credentialFacade.unlockWithBiometric(activity, title, subtitle, negativeButtonText)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDecryptionUnlocked = true,
                            isBiometricEnabled = true,
                            showUnlockDialog = false
                        )
                    }
                    _events.emit(CredentialStoreEvent.UnlockSuccess)
                    loadCredentials()
                }
                .onError { e ->
                    _uiState.update { it.copy(error = e.message ?: "Biometric error") }
                    _events.emit(CredentialStoreEvent.Error(e.message ?: "Biometric error"))
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
                    _uiState.update { it.copy(error = e.message ?: "Unknown error") }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun testSshConnection(
        host: String,
        sshKeyUuid: String,
        onResult: (SshTestResult) -> Unit
    ) {
        viewModelScope.launch {
            if (!credentialFacade.isUnlocked()) {
                onResult(SshTestResult.Failure("Credential vault is locked. Please unlock first."))
                return@launch
            }

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
