package jamgmilk.fuwagit.ui.screen.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.repository.ConfigRepository
import jamgmilk.fuwagit.domain.repository.RepoRepository
import jamgmilk.fuwagit.domain.repository.SettingsRepository
import jamgmilk.fuwagit.domain.usecase.git.ApplyGitConfigToAllRepos
import jamgmilk.fuwagit.ui.state.RepoStateManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
data class SettingsUiState(
    val savedReposCount: Int = 0,
    val userName: String = "",
    val userEmail: String = "",
    val defaultBranch: String = "main",
    val setUpstreamOnPush: Boolean = true,
    val autoSync: Boolean = false,
    val conflictSafeMode: Boolean = true,
    val backupBeforeSync: Boolean = true,
    val verboseLogging: Boolean = false,
    val darkMode: String = "system",
    val language: String = "system",
    val dynamicColor: Boolean = true,
    val globalUserName: String? = null,
    val globalUserEmail: String? = null,
    val applyResult: ApplyConfigResult? = null,
    val autoLockTimeout: String = "600",
    val isFirstRun: Boolean = true
)

data class ApplyConfigResult(
    val successCount: Int,
    val failureCount: Int,
    val totalCount: Int,
    val failures: Map<String, String> = emptyMap()
) {
    val allSuccess: Boolean get() = failureCount == 0 && successCount > 0
}

sealed class SettingsEvent {
    data class LanguageChanged(val language: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val configRepository: ConfigRepository,
    private val repoRepository: RepoRepository,
    private val applyGitConfigToAllRepos: ApplyGitConfigToAllRepos,
    private val repoStateManager: RepoStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadGlobalConfig()
        observeRepositories()
    }

    private fun observeRepositories() {
        viewModelScope.launch {
            launch {
                repoRepository.getAllReposFlow().collect { repos ->
                    _uiState.update { it.copy(savedReposCount = repos.size) }
                }
            }
            launch {
                settingsRepository.gitConfigFlow().collect { config ->
                    _uiState.update {
                        it.copy(
                            userName = config.userName,
                            userEmail = config.userEmail,
                            defaultBranch = config.defaultBranch,
                            setUpstreamOnPush = config.setUpstreamOnPush
                        )
                    }
                }
            }
            launch {
                settingsRepository.preferencesFlow().collect { prefs ->
                    _uiState.update {
                        it.copy(
                            autoSync = prefs.autoSync,
                            conflictSafeMode = prefs.conflictSafeMode,
                            backupBeforeSync = prefs.backupBeforeSync,
                            verboseLogging = prefs.verboseLogging,
                            darkMode = prefs.darkMode,
                            language = prefs.language,
                            dynamicColor = prefs.dynamicColor,
                            autoLockTimeout = prefs.autoLockTimeout,
                            isFirstRun = prefs.isFirstRun
                        )
                    }
                }
            }
        }
    }

    fun loadGlobalConfig() {
        viewModelScope.launch {
            val (name, email) = configRepository.getEffectiveUserConfig("")
            _uiState.update {
                it.copy(
                    globalUserName = name,
                    globalUserEmail = email
                )
            }
        }
    }

    fun saveUserConfig(name: String, email: String) {
        viewModelScope.launch {
            settingsRepository.saveUserConfig(name, email)
        }
    }

    fun saveDefaultBranch(branch: String) {
        viewModelScope.launch {
            settingsRepository.saveDefaultBranch(branch)
        }
    }

    fun saveSetUpstreamOnPush(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveSetUpstreamOnPush(enabled)
        }
    }

    fun saveAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveAutoSync(enabled)
        }
    }

    fun saveConflictSafeMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveConflictSafeMode(enabled)
        }
    }

    fun saveBackupBeforeSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveBackupBeforeSync(enabled)
        }
    }

    fun saveVerboseLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveVerboseLogging(enabled)
        }
    }

    fun saveDarkMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.saveDarkMode(mode)
        }
    }

    fun saveLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.saveLanguage(language)
            _events.emit(SettingsEvent.LanguageChanged(language))
        }
    }

    fun saveDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveDynamicColor(enabled)
        }
    }

    fun saveAutoLockTimeout(timeout: String) {
        viewModelScope.launch {
            settingsRepository.saveAutoLockTimeout(timeout)
        }
    }

    fun reloadUserConfig() {
        loadGlobalConfig()
    }

    fun applyConfigToAllRepos(name: String, email: String, alsoApplyToGlobal: Boolean) {
        viewModelScope.launch {
            val repos = repoRepository.getAllRepos()
            val repoPaths = repos.map { it.path }
            val result = applyGitConfigToAllRepos(repoPaths, name, email, alsoApplyToGlobal)

            _uiState.update {
                it.copy(
                    applyResult = ApplyConfigResult(
                        successCount = result.successCount,
                        failureCount = result.failureCount,
                        totalCount = result.totalCount,
                        failures = result.results
                            .filterValues { result -> result.isFailure }
                            .mapValues { (_, result) ->
                                result.exceptionOrNull()?.message ?: "Failed to apply config"
                            }
                    )
                )
            }
        }
    }

    fun clearApplyResult() {
        _uiState.update { it.copy(applyResult = null) }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            settingsRepository.resetFirstRun()
        }
    }

    fun getCurrentRepoPath(): String? {
        return repoStateManager.getRepoPath()
    }
}
