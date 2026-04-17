package jamgmilk.fuwagit.ui.screen.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.UiMessage
import jamgmilk.fuwagit.domain.usecase.credential.CredentialStoreFacade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Stable

@Stable
data class MasterPasswordUiState(
    val isMasterPasswordSet: Boolean = false,
    val passwordHint: String? = null,
    val isBiometricEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiMessage? = null,
    val isComplete: Boolean = false
)

@HiltViewModel
class MasterPasswordViewModel @Inject constructor(
    private val credentialFacade: CredentialStoreFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow(MasterPasswordUiState())
    val uiState: StateFlow<MasterPasswordUiState> = _uiState.asStateFlow()

    init {
        initialize()
    }

    fun initialize() {
        _uiState.update {
            it.copy(
                isMasterPasswordSet = credentialFacade.isMasterPasswordSet(),
                passwordHint = credentialFacade.getMasterPasswordHint(),
                isBiometricEnabled = credentialFacade.isBiometricEnabled()
            )
        }
    }

    fun setupMasterPassword(password: String, confirmPassword: String, hint: String?) {
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = UiMessage.Credential.PasswordMismatch) }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = UiMessage.Credential.PasswordMinChars()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            credentialFacade.setupMasterPassword(password, confirmPassword, hint)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isMasterPasswordSet = true,
                            isComplete = true
                        )
                    }
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiMessage.Generic(e.message ?: "Setup failed")
                        )
                    }
                }
        }
    }

    fun changeMasterPassword(oldPassword: String, newPassword: String, confirmPassword: String, hint: String?) {
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(error = UiMessage.Credential.PasswordMismatch) }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(error = UiMessage.Credential.PasswordMinChars()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            credentialFacade.changeMasterPassword(oldPassword, newPassword, hint)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordHint = hint,
                            isComplete = true
                        )
                    }
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = UiMessage.Credential.IncorrectOldPassword
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun enableBiometric(activity: FragmentActivity) {
        viewModelScope.launch {
            credentialFacade.enableBiometric(activity) { result ->
                when (result) {
                    is AppResult.Success -> {
                        _uiState.update { it.copy(isBiometricEnabled = true) }
                    }
                    is AppResult.Error -> {
                        _uiState.update { it.copy(error = UiMessage.Generic(result.message ?: "Biometric error")) }
                    }
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
}