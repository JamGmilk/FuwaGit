package jamgmilk.fuwagit.ui.screen.onboarding

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.repository.SettingsRepository
import jamgmilk.fuwagit.domain.usecase.credential.EnableBiometricUseCase
import jamgmilk.fuwagit.domain.usecase.credential.SetupMasterPasswordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.Stable

enum class OnboardingStep(val index: Int) {
    WELCOME(0),
    PERMISSIONS(1),
    MASTER_PASSWORD(2),
    GIT_CONFIG(3),
    ADD_REPO(4),
    COMPLETE(5)
}

@Stable
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val isMasterPasswordSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val defaultBranch: String = "main",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordHint: String = "",
    val enableBiometric: Boolean = false,
    val isSettingPassword: Boolean = false,
    val passwordError: String? = null,
    val isSavingConfig: Boolean = false,
    val isEnablingBiometric: Boolean = false,
    val isFirstRun: Boolean = true
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val setupMasterPasswordUseCase: SetupMasterPasswordUseCase,
    private val enableBiometricUseCase: EnableBiometricUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.preferencesFlow().collect { prefs ->
                _uiState.update { it.copy(isFirstRun = prefs.isFirstRun) }
            }
        }
        viewModelScope.launch {
            settingsRepository.gitConfigFlow().collect { config ->
                _uiState.update {
                    it.copy(
                        userName = config.userName,
                        userEmail = config.userEmail,
                        defaultBranch = config.defaultBranch
                    )
                }
            }
        }
    }

    fun goToStep(step: OnboardingStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun nextStep() {
        val next = OnboardingStep.entries.getOrNull(_uiState.value.currentStep.index + 1)
        if (next != null) {
            _uiState.update { it.copy(currentStep = next) }
        }
    }

    fun previousStep() {
        val prev = OnboardingStep.entries.getOrNull(_uiState.value.currentStep.index - 1)
        if (prev != null) {
            _uiState.update { it.copy(currentStep = prev) }
        }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, passwordError = null) }
    }

    fun updatePasswordHint(hint: String) {
        _uiState.update { it.copy(passwordHint = hint) }
    }

    fun updateEnableBiometric(enable: Boolean) {
        _uiState.update { it.copy(enableBiometric = enable) }
    }

    fun updateUserName(name: String) {
        _uiState.update { it.copy(userName = name) }
    }

    fun updateUserEmail(email: String) {
        _uiState.update { it.copy(userEmail = email) }
    }

    fun updateDefaultBranch(branch: String) {
        _uiState.update { it.copy(defaultBranch = branch) }
    }

    fun setupMasterPassword(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(passwordError = "Passwords do not match") }
            return
        }

        _uiState.update { it.copy(isSettingPassword = true, passwordError = null) }
        viewModelScope.launch {
            setupMasterPasswordUseCase(state.password, state.confirmPassword, state.passwordHint.ifBlank { null })
                .onSuccess {
                    _uiState.update { it.copy(isSettingPassword = false, isMasterPasswordSet = true) }
                    onSuccess()
                }
                .onError { e ->
                    _uiState.update { it.copy(isSettingPassword = false, passwordError = e.message) }
                }
        }
    }

    fun enableBiometricAuth(activity: FragmentActivity, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isEnablingBiometric = true) }
        viewModelScope.launch {
            enableBiometricUseCase(activity) { result ->
                when (result) {
                    is AppResult.Success -> {
                        _uiState.update { it.copy(isEnablingBiometric = false, isBiometricEnabled = true) }
                        onSuccess()
                    }
                    is AppResult.Error -> {
                        _uiState.update { it.copy(isEnablingBiometric = false) }
                    }
                }
            }
        }
    }

    fun saveGitConfig(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isSavingConfig = true) }
        viewModelScope.launch {
            settingsRepository.saveUserConfig(state.userName, state.userEmail)
            settingsRepository.saveDefaultBranch(state.defaultBranch)
            _uiState.update { it.copy(isSavingConfig = false) }
            onSuccess()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setFirstRunCompleted()
        }
    }

    fun skipMasterPassword() {
        _uiState.update { it.copy(isMasterPasswordSet = false) }
        nextStep()
    }
}
