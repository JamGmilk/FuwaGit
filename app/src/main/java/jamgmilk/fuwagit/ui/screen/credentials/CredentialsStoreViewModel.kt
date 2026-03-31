package jamgmilk.fuwagit.ui.screen.credentials

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.data.local.credential.HttpsCredential
import jamgmilk.fuwagit.data.local.credential.SshKey
import jamgmilk.fuwagit.domain.usecase.credential.AddHttpsCredentialUseCase
import jamgmilk.fuwagit.domain.usecase.credential.AddSshKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.DeleteHttpsCredentialUseCase
import jamgmilk.fuwagit.domain.usecase.credential.DeleteSshKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.DisableBiometricUseCase
import jamgmilk.fuwagit.domain.usecase.credential.EnableBiometricUseCase
import jamgmilk.fuwagit.domain.usecase.credential.ExportCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetMasterPasswordHintUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshKeysUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshPrivateKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.ImportCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.IsBiometricEnabledUseCase
import jamgmilk.fuwagit.domain.usecase.credential.IsMasterPasswordSetUseCase
import jamgmilk.fuwagit.domain.usecase.credential.IsUnlockedUseCase
import jamgmilk.fuwagit.domain.usecase.credential.LockCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.SetupMasterPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.UnlockWithPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.UpdateHttpsCredentialUseCase
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
    private val isMasterPasswordSetUseCase: IsMasterPasswordSetUseCase,
    private val isBiometricEnabledUseCase: IsBiometricEnabledUseCase,
    private val getMasterPasswordHintUseCase: GetMasterPasswordHintUseCase,
    private val isUnlockedUseCase: IsUnlockedUseCase,
    private val lockCredentialsUseCase: LockCredentialsUseCase,
    private val exportCredentialsUseCase: ExportCredentialsUseCase,
    private val importCredentialsUseCase: ImportCredentialsUseCase,
    private val enableBiometricUseCase: EnableBiometricUseCase,
    private val disableBiometricUseCase: DisableBiometricUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialsStoreUiState())
    val uiState: StateFlow<CredentialsStoreUiState> = _uiState.asStateFlow()

    fun initialize() {
        val isSet = isMasterPasswordSetUseCase()
        val isBioEnabled = isBiometricEnabledUseCase()

        _uiState.value = _uiState.value.copy(
            isMasterPasswordSet = isSet,
            isBiometricEnabled = isBioEnabled,
            passwordHint = getMasterPasswordHintUseCase()
        )

        loadCredentials()
    }

    fun setupMasterPassword(password: String, confirmPassword: String, hint: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            setupMasterPasswordUseCase(password, confirmPassword, hint)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isMasterPasswordSet = true,
                        showUnlockDialog = false
                    )
                    loadCredentials()
                }
                .onError { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun unlockWithPassword(password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            unlockWithPasswordUseCase(password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isDecryptionUnlocked = true,
                        showUnlockDialog = false
                    )
                    loadCredentials()
                }
                .onError { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            if (!isUnlockedUseCase()) return@launch

            _uiState.value = _uiState.value.copy(isDecryptionUnlocked = true)

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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            addHttpsCredentialUseCase(host, username, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadCredentials()
                }
                .onError { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun updateHttpsCredential(uuid: String, host: String?, username: String?, password: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            updateHttpsCredentialUseCase(uuid, host, username, password)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadCredentials()
                }
                .onError { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun deleteHttpsCredential(uuid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            deleteHttpsCredentialUseCase(uuid)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadCredentials()
                }
                .onError { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun addSshKey(name: String, type: String, publicKey: String, privateKey: String, passphrase: String?, fingerprint: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            addSshKeyUseCase(name, type, publicKey, privateKey, passphrase, fingerprint)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadCredentials()
                }
                .onError { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun deleteSshKey(uuid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            deleteSshKeyUseCase(uuid)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    loadCredentials()
                }
                .onError { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun exportCredentials() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            exportCredentialsUseCase()
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        exportedData = data,
                        showExportDialog = true
                    )
                }
                .onError { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun importCredentials(jsonData: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            importCredentialsUseCase(jsonData)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showImportDialog = false,
                        importSuccess = true
                    )
                    loadCredentials()
                }
                .onError { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
        }
    }

    fun enableBiometric(activity: FragmentActivity) {
        viewModelScope.launch {
            enableBiometricUseCase()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isBiometricEnabled = true)
                }
                .onError { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
        }
    }

    fun disableBiometric() {
        viewModelScope.launch {
            disableBiometricUseCase()
            _uiState.value = _uiState.value.copy(isBiometricEnabled = false)
        }
    }

    fun lock() {
        lockCredentialsUseCase()
        _uiState.value = _uiState.value.copy(
            isDecryptionUnlocked = false,
            httpsCredentials = emptyList(),
            sshKeys = emptyList()
        )
    }

    fun showUnlockDialog() {
        _uiState.value = _uiState.value.copy(showUnlockDialog = true)
    }

    fun dismissUnlockDialog() {
        _uiState.value = _uiState.value.copy(showUnlockDialog = false)
    }

    fun showExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = true)
    }

    fun dismissExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false, exportedData = null)
    }

    fun showImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = true)
    }

    fun dismissImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    suspend fun getHttpsPassword(uuid: String): String? {
        return when (val result = getHttpsPasswordUseCase(uuid)) {
            is jamgmilk.fuwagit.core.result.AppResult.Success -> result.data
            is jamgmilk.fuwagit.core.result.AppResult.Error -> null
        }
    }

    suspend fun getSshPrivateKey(uuid: String): String? {
        return when (val result = getSshPrivateKeyUseCase(uuid)) {
            is jamgmilk.fuwagit.core.result.AppResult.Success -> result.data
            is jamgmilk.fuwagit.core.result.AppResult.Error -> null
        }
    }
}
