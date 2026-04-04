package jamgmilk.fuwagit.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.data.jgit.GitConfigManager
import jamgmilk.fuwagit.data.local.prefs.GitConfigDataStore
import jamgmilk.fuwagit.data.local.prefs.AppPreferencesStore
import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.domain.usecase.git.ApplyGitConfigToAllRepos
import jamgmilk.fuwagit.domain.usecase.git.ApplyGitConfigToGlobal
import jamgmilk.fuwagit.domain.usecase.git.ApplyGitConfigToRepo
import jamgmilk.fuwagit.domain.usecase.git.GetGlobalUserConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.Stable

@Stable
data class SettingsUiState(
    val savedReposCount: Int = 0,
    val userName: String = "",
    val userEmail: String = "",
    val defaultBranch: String = "main",
    val autoSync: Boolean = false,
    val conflictSafeMode: Boolean = true,
    val backupBeforeSync: Boolean = true,
    val verboseLogging: Boolean = false,
    val darkMode: String = "system",
    val globalUserName: String? = null,
    val globalUserEmail: String? = null,
    val applyResult: ApplyConfigResult? = null
)

data class ApplyConfigResult(
    val successCount: Int,
    val failureCount: Int,
    val totalCount: Int,
    val failures: Map<String, String> = emptyMap()
) {
    val allSuccess: Boolean get() = failureCount == 0 && successCount > 0
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repoDataStore: RepoDataStore,
    private val gitConfigDataStore: GitConfigDataStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val gitConfigManager: GitConfigManager,
    private val applyGitConfigToRepo: ApplyGitConfigToRepo,
    private val applyGitConfigToGlobal: ApplyGitConfigToGlobal,
    private val applyGitConfigToAllRepos: ApplyGitConfigToAllRepos,
    private val getGlobalUserConfig: GetGlobalUserConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadGlobalConfig()
        observeDataStores()
    }

    private fun observeDataStores() {
        viewModelScope.launch {
            launch {
                repoDataStore.getSavedReposFlow().collect { repos ->
                    _uiState.update { it.copy(savedReposCount = repos.size) }
                }
            }
            launch {
                gitConfigDataStore.configFlow.collect { config ->
                    _uiState.update {
                        it.copy(
                            userName = config.userName,
                            userEmail = config.userEmail,
                            defaultBranch = config.defaultBranch
                        )
                    }
                }
            }
            launch {
                appPreferencesStore.preferencesFlow.collect { prefs ->
                    _uiState.update {
                        it.copy(
                            autoSync = prefs.autoSync,
                            conflictSafeMode = prefs.conflictSafeMode,
                            backupBeforeSync = prefs.backupBeforeSync,
                            verboseLogging = prefs.verboseLogging,
                            darkMode = prefs.darkMode
                        )
                    }
                }
            }
        }
    }

    /**
     * 鍔犺浇 global git config
     */
    fun loadGlobalConfig() {
        viewModelScope.launch {
            val (name, email) = getGlobalUserConfig()
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
            gitConfigDataStore.setUserConfig(name, email)
        }
    }

    fun saveDefaultBranch(branch: String) {
        viewModelScope.launch {
            gitConfigDataStore.setDefaultBranch(branch)
        }
    }

    fun saveAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesStore.setAutoSync(enabled)
        }
    }

    fun saveConflictSafeMode(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesStore.setConflictSafeMode(enabled)
        }
    }

    fun saveBackupBeforeSync(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesStore.setBackupBeforeSync(enabled)
        }
    }

    fun saveVerboseLogging(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesStore.setVerboseLogging(enabled)
        }
    }

    fun saveDarkMode(mode: String) {
        viewModelScope.launch {
            appPreferencesStore.setDarkMode(mode)
        }
    }

    suspend fun reloadUserConfig() {
        // DataStore automatically reloads from disk when needed
    }

    fun applyConfigToGlobal(name: String, email: String) {
        viewModelScope.launch {
            applyGitConfigToGlobal(name, email)
                .onSuccess { loadGlobalConfig() }
        }
    }

    fun applyConfigToRepo(repoPath: String, name: String, email: String) {
        viewModelScope.launch {
            applyGitConfigToRepo(repoPath, name, email)
        }
    }

    fun applyConfigToAllRepos(name: String, email: String, alsoApplyToGlobal: Boolean) {
        viewModelScope.launch {
            val repos = repoDataStore.getAllRepos()
            val repoPaths = repos.map { it.path }
            val result = applyGitConfigToAllRepos(repoPaths, name, email, alsoApplyToGlobal)

            _uiState.update {
                it.copy(
                    applyResult = ApplyConfigResult(
                        successCount = result.successCount,
                        failureCount = result.failureCount,
                        totalCount = result.totalCount,
                        failures = result.results
                            .filterValues { it.isFailure }
                            .mapValues { (_, resultValue) ->
                                resultValue.exceptionOrNull()?.message ?: "Failed to apply config"
                            }
                    )
                )
            }
        }
    }

    fun clearApplyResult() {
        _uiState.update { it.copy(applyResult = null) }
    }
}
