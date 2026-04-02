package jamgmilk.fuwagit.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.data.jgit.GitConfigManager
import jamgmilk.fuwagit.data.local.prefs.GitConfigStore
import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.domain.usecase.git.ApplyGitConfigToAllRepos
import jamgmilk.fuwagit.domain.usecase.git.ApplyGitConfigToGlobal
import jamgmilk.fuwagit.domain.usecase.git.ApplyGitConfigToRepo
import jamgmilk.fuwagit.domain.usecase.git.GetGlobalUserConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val gitConfigStore: GitConfigStore,
    private val gitConfigManager: GitConfigManager,
    private val applyGitConfigToRepo: ApplyGitConfigToRepo,
    private val applyGitConfigToGlobal: ApplyGitConfigToGlobal,
    private val applyGitConfigToAllRepos: ApplyGitConfigToAllRepos,
    private val getGlobalUserConfig: GetGlobalUserConfig
) : ViewModel() {

    val savedReposCount: StateFlow<Int> = repoDataStore.getSavedReposFlow()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val userName: StateFlow<String> = gitConfigStore.configFlow
        .map { it.userName }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val userEmail: StateFlow<String> = gitConfigStore.configFlow
        .map { it.userEmail }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val defaultBranch: StateFlow<String> = gitConfigStore.configFlow
        .map { it.defaultBranch }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "main"
        )

    val autoSync: StateFlow<Boolean> = gitConfigStore.configFlow
        .map { it.autoSync }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val conflictSafeMode: StateFlow<Boolean> = gitConfigStore.configFlow
        .map { it.conflictSafeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val backupBeforeSync: StateFlow<Boolean> = gitConfigStore.configFlow
        .map { it.backupBeforeSync }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val verboseLogging: StateFlow<Boolean> = gitConfigStore.configFlow
        .map { it.verboseLogging }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Global config 状态
    private val _globalUserName = MutableStateFlow<String?>(null)
    private val _globalUserEmail = MutableStateFlow<String?>(null)
    val globalUserName: StateFlow<String?> = _globalUserName.asStateFlow()
    val globalUserEmail: StateFlow<String?> = _globalUserEmail.asStateFlow()

    // 应用配置结果
    private val _applyResult = MutableStateFlow<ApplyConfigResult?>(null)
    val applyResult: StateFlow<ApplyConfigResult?> = _applyResult.asStateFlow()

    init {
        loadGlobalConfig()
    }

    /**
     * 加载 global git config
     */
    fun loadGlobalConfig() {
        viewModelScope.launch {
            val (name, email) = getGlobalUserConfig()
            _globalUserName.value = name
            _globalUserEmail.value = email
        }
    }

    fun saveUserConfig(name: String, email: String) {
        viewModelScope.launch {
            gitConfigStore.setUserConfig(name, email)
        }
    }

    fun saveDefaultBranch(branch: String) {
        viewModelScope.launch {
            gitConfigStore.setDefaultBranch(branch)
        }
    }

    fun saveAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            gitConfigStore.setAutoSync(enabled)
        }
    }

    fun saveConflictSafeMode(enabled: Boolean) {
        viewModelScope.launch {
            gitConfigStore.setConflictSafeMode(enabled)
        }
    }

    fun saveBackupBeforeSync(enabled: Boolean) {
        viewModelScope.launch {
            gitConfigStore.setBackupBeforeSync(enabled)
        }
    }

    fun saveVerboseLogging(enabled: Boolean) {
        viewModelScope.launch {
            gitConfigStore.setVerboseLogging(enabled)
        }
    }

    suspend fun reloadUserConfig() {
        gitConfigStore.reloadFromFile()
    }

    /**
     * 将用户配置应用到 global git config
     */
    fun applyConfigToGlobal(name: String, email: String) {
        viewModelScope.launch {
            applyGitConfigToGlobal(name, email)
                .onSuccess {
                    loadGlobalConfig()
                }
        }
    }

    /**
     * 将用户配置应用到指定仓库
     */
    fun applyConfigToRepo(repoPath: String, name: String, email: String) {
        viewModelScope.launch {
            applyGitConfigToRepo(repoPath, name, email)
        }
    }

    /**
     * 将用户配置应用到所有仓库
     */
    fun applyConfigToAllRepos(name: String, email: String, alsoApplyToGlobal: Boolean) {
        viewModelScope.launch {
            val repos = repoDataStore.getAllRepos()
            val repoPaths = repos.map { it.path }
            
            val result = applyGitConfigToAllRepos(repoPaths, name, email, alsoApplyToGlobal)
            
            _applyResult.value = ApplyConfigResult(
                successCount = result.successCount,
                failureCount = result.failureCount,
                totalCount = result.totalCount,
                failures = result.results
                    .filterValues { it.isFailure }
                    .mapValues { (_, resultValue) ->
                        resultValue.exceptionOrNull()?.message ?: "Failed to apply config"
                    }
            )
        }
    }

    /**
     * 清除应用配置结果
     */
    fun clearApplyResult() {
        _applyResult.value = null
    }
}
