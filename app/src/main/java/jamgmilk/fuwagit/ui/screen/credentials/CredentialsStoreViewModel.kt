package jamgmilk.fuwagit.ui.screen.credentials

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jamgmilk.fuwagit.credential.store.HttpsCredential
import jamgmilk.fuwagit.credential.store.SshKey
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import jamgmilk.fuwagit.domain.usecase.credential.AddHttpsCredentialUseCase
import jamgmilk.fuwagit.domain.usecase.credential.AddSshKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.DeleteHttpsCredentialUseCase
import jamgmilk.fuwagit.domain.usecase.credential.DeleteSshKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshKeysUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshPrivateKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.SetupMasterPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.UnlockWithPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.UpdateHttpsCredentialUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CredentialsStoreUiState(
    val isMasterPasswordSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isDecryptionUnlocked: Boolean = false,
    val showUnlockDialog: Boolean = false,
    val passwordHint: String? = null,
    val httpsCredentials: List<HttpsCredential> = emptyList(),
    val sshKeys: List<SshKey> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CredentialsStoreViewModel(
    private val credentialRepository: CredentialRepository,
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
    private val getSshPrivateKeyUseCase: GetSshPrivateKeyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsStoreUiState())
    val uiState: StateFlow<CredentialsStoreUiState> = _uiState.asStateFlow()

    fun initialize() {
        val isSet = credentialRepository.isMasterPasswordSet()
        val isBioEnabled = credentialRepository.isBiometricEnabled()

        _uiState.value = _uiState.value.copy(
            isMasterPasswordSet = isSet,
            isBiometricEnabled = isBioEnabled,
            passwordHint = credentialRepository.getMasterPasswordHint()
        )

        loadCredentials()
    }

    fun setupMasterPassword(password: String, confirmPassword: String, hint: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            setupMasterPasswordUseCase(password, confirmPassword, hint)
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

            unlockWithPasswordUseCase(password)
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
            error = "Biometric authentication not yet implemented"
        )
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            getHttpsCredentialsUseCase()
                .onSuccess { credentials ->
                    _uiState.value = _uiState.value.copy(httpsCredentials = credentials)
                }

            getSshKeysUseCase()
                .onSuccess { keys ->
                    _uiState.value = _uiState.value.copy(sshKeys = keys)
                }
        }
    }

    fun addHttpsCredential(host: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            addHttpsCredentialUseCase(host, username, password)
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

            updateHttpsCredentialUseCase(uuid, host, username, password)
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
            deleteHttpsCredentialUseCase(uuid)
                .onSuccess {
                    loadCredentials()
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    suspend fun getHttpsPassword(uuid: String): String? {
        return when (val result = getHttpsPasswordUseCase(uuid)) {
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
        fingerprint: String,
        comment: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            addSshKeyUseCase(
                name = name,
                type = type,
                publicKey = publicKey,
                privateKey = privateKey,
                passphrase = passphrase,
                fingerprint = fingerprint,
                comment = comment
            ).onSuccess {
                loadCredentials()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onError { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = exception.message
                )
            }
        }
    }

    fun deleteSshKey(uuid: String) {
        viewModelScope.launch {
            deleteSshKeyUseCase(uuid)
                .onSuccess {
                    loadCredentials()
                }
                .onError { exception ->
                    _uiState.value = _uiState.value.copy(error = exception.message)
                }
        }
    }

    suspend fun getSshPrivateKey(uuid: String): String? {
        return when (val result = getSshPrivateKeyUseCase(uuid)) {
            is jamgmilk.fuwagit.domain.model.AppResult.Success -> result.data
            is jamgmilk.fuwagit.domain.model.AppResult.Error -> null
        }
    }

    fun isDecryptionUnlocked(): Boolean {
        return credentialRepository.isUnlocked()
    }

    fun lock() {
        credentialRepository.lock()
        _uiState.value = _uiState.value.copy(isDecryptionUnlocked = false)
    }
}
