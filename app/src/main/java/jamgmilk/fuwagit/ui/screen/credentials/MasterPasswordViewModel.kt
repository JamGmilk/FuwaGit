package jamgmilk.fuwagit.ui.screen.credentials

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.usecase.credential.CredentialStoreFacade
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.Stable

sealed class MasterPasswordEvent {
    data object SetupSuccess : MasterPasswordEvent()
    data object ChangeSuccess : MasterPasswordEvent()
    data class Error(val message: String) : MasterPasswordEvent()
    data object BiometricEnabled : MasterPasswordEvent()
    data class BiometricError(val message: String) : MasterPasswordEvent()
}

@Stable
data class MasterPasswordUiState(
    val isMasterPasswordSet: Boolean = false,
    val passwordHint: String? = null,
    val isBiometricEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)

@HiltViewModel
class MasterPasswordViewModel @Inject constructor(
    private val credentialFacade: CredentialStoreFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow(MasterPasswordUiState())
    val uiState: StateFlow<MasterPasswordUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MasterPasswordEvent>()
    val events: SharedFlow<MasterPasswordEvent> = _events.asSharedFlow()

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
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters") }
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
                    _events.emit(MasterPasswordEvent.SetupSuccess)
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Setup failed"
                        )
                    }
                    _events.emit(MasterPasswordEvent.Error(e.message ?: "Setup failed"))
                }
        }
    }

    fun changeMasterPassword(
        activity: FragmentActivity?,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String,
        hint: String?,
        biometricEnabled: Boolean
    ) {
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(error = "Passwords do not match") }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val wasBiometricEnabled = _uiState.value.isBiometricEnabled

            credentialFacade.changeMasterPassword(oldPassword, newPassword, hint)
                .onSuccess {
                    when {
                        wasBiometricEnabled && !biometricEnabled -> {
                            credentialFacade.disableBiometric()
                            finishPasswordChange(hint = hint, biometricEnabled = false)
                        }

                        !wasBiometricEnabled && biometricEnabled -> {
                            credentialFacade.unlockWithPassword(newPassword)
                                .onSuccess {
                                    if (activity == null) {
                                        val message = AppException.MasterKeyNotUnlocked().message
                                        _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                error = message
                                            )
                                        }
                                        _events.emit(MasterPasswordEvent.Error(message))
                                        return@onSuccess
                                    }

                                    credentialFacade.enableBiometric(activity) { result ->
                                        when (result) {
                                            is AppResult.Success -> {
                                                viewModelScope.launch {
                                                    finishPasswordChange(hint = hint, biometricEnabled = true)
                                                }
                                            }

                                            is AppResult.Error -> {
                                                viewModelScope.launch {
                                                    val message = result.message ?: "Biometric error"
                                                    _uiState.update {
                                                        it.copy(
                                                            isLoading = false,
                                                            error = message
                                                        )
                                                    }
                                                    _events.emit(MasterPasswordEvent.Error(message))
                                                }
                                            }
                                        }
                                    }
                                }
                                .onError { e ->
                                    val message = e.message ?: AppException.MasterKeyNotUnlocked().message
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            error = message
                                        )
                                    }
                                    _events.emit(MasterPasswordEvent.Error(message))
                                }
                        }

                        else -> {
                            finishPasswordChange(
                                hint = hint,
                                biometricEnabled = wasBiometricEnabled
                            )
                        }
                    }
                }
                .onError { e ->
                    val message = changePasswordErrorMessage(e)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = message
                        )
                    }
                    _events.emit(MasterPasswordEvent.Error(message))
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
                        viewModelScope.launch { _events.emit(MasterPasswordEvent.BiometricEnabled) }
                    }
                    is AppResult.Error -> {
                        _uiState.update { it.copy(error = result.message ?: "Biometric error") }
                        viewModelScope.launch { _events.emit(MasterPasswordEvent.BiometricError(result.message ?: "Biometric error")) }
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

    private suspend fun finishPasswordChange(hint: String?, biometricEnabled: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = false,
                passwordHint = hint,
                isBiometricEnabled = biometricEnabled,
                isComplete = true
            )
        }
        _events.emit(MasterPasswordEvent.ChangeSuccess)
    }

    private fun changePasswordErrorMessage(exception: AppException): String {
        return when (exception) {
            is AppException.InvalidPassword -> "Incorrect old password"
            else -> exception.message ?: "Change password failed"
        }
    }
}
