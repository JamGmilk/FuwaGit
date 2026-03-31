package jamgmilk.fuwagit.ui.screen.myrepos

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.data.util.RepoPathUtils
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.repo.RepoData
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshKeysUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshPrivateKeyUseCase
import jamgmilk.fuwagit.domain.usecase.git.CleanUseCase
import jamgmilk.fuwagit.domain.usecase.git.CloneRepositoryUseCase
import jamgmilk.fuwagit.domain.usecase.git.ConfigureRemoteUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetRepoInfoUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetRemoteUrlUseCase
import jamgmilk.fuwagit.domain.state.RepoStateManager
import jamgmilk.fuwagit.domain.state.RepoInfo
import jamgmilk.fuwagit.domain.usecase.CurrentRepoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RepoFolderItem(
    val path: String,
    val alias: String, // 在添加或克隆时需要填入 alias，不填默认就为文件夹名
    val isGitRepo: Boolean,
    val isRemote: Boolean,
    val isActive: Boolean,
    val lastModified: Long = 0L,
)

data class RepoUiState(
    val repoItems: List<RepoFolderItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class HttpsCredentialItem(
    val uuid: String,
    val host: String,
    val username: String,
    val displayName: String
)

data class SshKeyItem(
    val uuid: String,
    val name: String,
    val fingerprint: String,
    val displayName: String
)

@HiltViewModel
class MyReposViewModel @Inject constructor(
    private val repoDataStore: RepoDataStore,
    private val currentRepoManager: RepoStateManager,
    private val currentRepoUseCase: CurrentRepoUseCase,
    private val cleanUseCase: CleanUseCase,
    private val cloneRepositoryUseCase: CloneRepositoryUseCase,
    private val configureRemoteUseCase: ConfigureRemoteUseCase,
    private val getRepoInfoUseCase: GetRepoInfoUseCase,
    private val getRemoteUrlUseCase: GetRemoteUrlUseCase,
    private val getHttpsCredentialsUseCase: GetHttpsCredentialsUseCase,
    private val getHttpsPasswordUseCase: GetHttpsPasswordUseCase,
    private val getSshKeysUseCase: GetSshKeysUseCase,
    private val getSshPrivateKeyUseCase: GetSshPrivateKeyUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val currentRepoInfo: StateFlow<RepoInfo> = currentRepoManager.repoInfo

    private val _savedRepos = MutableStateFlow<List<RepoData>>(emptyList())
    val savedRepos: StateFlow<List<RepoData>> = _savedRepos.asStateFlow()

    val uiState: StateFlow<RepoUiState> = combine(
        _savedRepos,
        currentRepoInfo,
        _isLoading,
        _error
    ) { repos, currentRepo, loading, error ->
        RepoUiState(
            repoItems = repos.map { repo ->
                RepoFolderItem(
                    path = repo.path,
                    alias = repo.displayName,
                    isGitRepo = true,
                    isRemote = false,
                    isActive = repo.path == currentRepo.repoPath,
                    lastModified = repo.lastAccessedAt
                )
            },
            isLoading = loading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RepoUiState()
    )

    private val _terminalOutput = MutableStateFlow<List<String>>(emptyList())
    val terminalOutput: StateFlow<List<String>> = _terminalOutput.asStateFlow()

    private var storageInitialized = false

    fun loadSavedRepos() {
        viewModelScope.launch {
            val repos = repoDataStore.getAllRepos()
            _savedRepos.value = repos
        }
    }

    fun initializeStorage(context: Context) {
        if (storageInitialized) return
        storageInitialized = true
        viewModelScope.launch {
            currentRepoUseCase.initializeFromStorage()
        }
        loadSavedRepos()
    }

    suspend fun addRepo(path: String, alias: String? = null): Boolean {
        val repo = RepoData(path = path, alias = alias)
        val result = repoDataStore.addRepo(repo)
        if (result) {
            if (currentRepoManager.getRepoPath() == null) {
                currentRepoManager.setRepoPath(path)
            }
            loadSavedRepos()
        }
        return result
    }

    suspend fun removeRepo(repo: RepoData): Boolean {
        val result = repoDataStore.removeRepo(repo.path)
        if (result) {
            loadSavedRepos()
        }
        return result
    }

    suspend fun updateRepoAlias(path: String, alias: String?) {
        repoDataStore.updateRepo(path) { it.copy(alias = alias) }
        loadSavedRepos()
    }

    suspend fun toggleFavorite(path: String) {
        repoDataStore.toggleFavorite(path)
        loadSavedRepos()
    }

    fun removeRepo(context: Context, item: RepoFolderItem) {
        viewModelScope.launch {
            repoDataStore.removeRepo(item.path)
            if (currentRepoManager.getRepoPath() == item.path) {
                currentRepoManager.clearRepo()
            }
            loadSavedRepos()
        }
    }

    suspend fun setCurrentRepo(path: String?) {
        currentRepoManager.setRepoPath(path)
    }

    fun refreshRepoItems(context: Context) {
        viewModelScope.launch {
            loadSavedRepos()
        }
    }

    suspend fun cleanRepo(path: String, dryRun: Boolean = false): Result<String> {
        return cleanUseCase(path, dryRun)
    }

    suspend fun getRepoInfo(localPath: String): Map<String, String> {
        return getRepoInfoUseCase(localPath)
    }

    suspend fun getRemoteUrl(localPath: String, name: String = "origin"): String? {
        return getRemoteUrlUseCase(localPath, name)
    }

    fun configureRemote(localPath: String, name: String, url: String) {
        viewModelScope.launch {
            configureRemoteUseCase(localPath, name, url)
                .onSuccess { result ->
                    appendTerminalLog("git remote add $name $url", result)
                }
                .onFailure { e ->
                    appendTerminalLog("git remote add $name $url", "Error: ${e.message}")
                }
        }
    }

    fun cloneRepository(
        context: Context,
        uri: String,
        localPath: String,
        branch: String? = null,
        credentials: CloneCredential? = null,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            appendTerminalLog("git clone $uri", "Cloning...")

            cloneRepositoryUseCase(uri, localPath, branch, credentials)
                .onSuccess { result ->
                    appendTerminalLog("git clone $uri", result)
                    _isLoading.value = false
                    addRepo(localPath, null)
                    onResult(Result.success(result))
                }
                .onFailure { e ->
                    val errorMsg = "Error: ${e.message}"
                    appendTerminalLog("git clone $uri", errorMsg)
                    _isLoading.value = false
                    _error.value = errorMsg
                    onResult(Result.failure(e))
                }
        }
    }

    fun isDirectoryEmpty(path: String): Boolean {
        val dir = File(path)
        return !dir.exists() || dir.listFiles()?.isEmpty() != false
    }

    suspend fun getHttpsCredentials(): List<HttpsCredentialItem> {
        return getHttpsCredentialsUseCase()
            .map { credentials ->
                credentials.map { cred ->
                    HttpsCredentialItem(
                        uuid = cred.uuid,
                        host = cred.host,
                        username = cred.username,
                        displayName = "${cred.username}@${cred.host}"
                    )
                }
            }
            .getOrNull() ?: emptyList()
    }

    suspend fun getHttpsPassword(uuid: String): String? {
        return getHttpsPasswordUseCase(uuid).getOrNull()
    }

    suspend fun getSshKeys(): List<SshKeyItem> {
        return getSshKeysUseCase()
            .map { keys ->
                keys.map { key ->
                    SshKeyItem(
                        uuid = key.uuid,
                        name = key.name,
                        fingerprint = key.fingerprint,
                        displayName = key.name
                    )
                }
            }
            .getOrNull() ?: emptyList()
    }

    suspend fun getSshPrivateKey(uuid: String): String? {
        return getSshPrivateKeyUseCase(uuid).getOrNull()
    }

    fun cloneWithCredentials(
        context: Context,
        uri: String,
        localPath: String,
        branch: String? = null,
        httpsCredentialUuid: String? = null,
        sshKeyUuid: String? = null,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            appendTerminalLog("git clone $uri", "Cloning...")

            val credentials: CloneCredential? = when {
                httpsCredentialUuid != null -> {
                    val httpsCred = getHttpsCredentials().find { it.uuid == httpsCredentialUuid }
                    val password = getHttpsPassword(httpsCredentialUuid)
                    if (httpsCred != null && password != null) {
                        CloneCredential.Https(httpsCred.username, password)
                    } else null
                }
                sshKeyUuid != null -> {
                    val privateKey = getSshPrivateKey(sshKeyUuid)
                    if (privateKey != null) {
                        CloneCredential.Ssh(privateKey, null)
                    } else null
                }
                else -> null
            }

            cloneRepositoryUseCase(uri, localPath, branch, credentials)
                .onSuccess { result ->
                    appendTerminalLog("git clone $uri", result)
                    _isLoading.value = false
                    addRepo(localPath, null)
                    onResult(Result.success(result))
                }
                .onFailure { e ->
                    val errorMsg = "Error: ${e.message}"
                    appendTerminalLog("git clone $uri", errorMsg)
                    _isLoading.value = false
                    _error.value = errorMsg
                    onResult(Result.failure(e))
                }
        }
    }

    fun appendTerminalLog(command: String, output: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] > $command\n$output"
        _terminalOutput.value += line
    }
}
