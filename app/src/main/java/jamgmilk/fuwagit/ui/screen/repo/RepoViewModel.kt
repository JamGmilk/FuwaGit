package jamgmilk.fuwagit.ui.screen.repo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.data.local.RepoDataStore
import jamgmilk.fuwagit.data.source.RepoPathUtils
import jamgmilk.fuwagit.domain.model.CloneCredential
import jamgmilk.fuwagit.domain.model.GitBranch
import jamgmilk.fuwagit.domain.model.RepoData
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshKeysUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshPrivateKeyUseCase
import jamgmilk.fuwagit.domain.usecase.git.CheckoutBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.CheckRepoStatusUseCase
import jamgmilk.fuwagit.domain.usecase.git.CherryPickUseCase
import jamgmilk.fuwagit.domain.usecase.git.CleanUseCase
import jamgmilk.fuwagit.domain.usecase.git.CloneUseCase
import jamgmilk.fuwagit.domain.usecase.repo.GetRemoteUrlUseCase
import jamgmilk.fuwagit.domain.usecase.repo.GetRepoInfoUseCase
import jamgmilk.fuwagit.domain.usecase.repo.ConfigureRemoteUseCase
import jamgmilk.fuwagit.domain.usecase.repo.HasGitDirUseCase
import jamgmilk.fuwagit.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RepoFolderItem(
    val id: String,
    val name: String,
    val path: String,
    val isGitRepo: Boolean,
    val isDirectory: Boolean = true,
    val localPath: String? = null,
    val source: String = "Saved",
    val permissionHint: String = "Saved",
    val isActive: Boolean,
    val isRemovable: Boolean = true,
    val lastModified: Long = 0L,
)

data class RepoUiState(
    val repoItems: List<RepoFolderItem> = emptyList(),
    val targetPath: String? = null,
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
class RepoViewModel @Inject constructor(
    private val getRepoInfoUseCase: GetRepoInfoUseCase,
    private val getRemoteUrlUseCase: GetRemoteUrlUseCase,
    private val configureRemoteUseCase: ConfigureRemoteUseCase,
    private val cloneUseCase: CloneUseCase,
    private val cleanUseCase: CleanUseCase,
    private val getHttpsCredentialsUseCase: GetHttpsCredentialsUseCase,
    private val getHttpsPasswordUseCase: GetHttpsPasswordUseCase,
    private val getSshKeysUseCase: GetSshKeysUseCase,
    private val getSshPrivateKeyUseCase: GetSshPrivateKeyUseCase,
    private val repoDataStore: RepoDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoUiState())
    val uiState: StateFlow<RepoUiState> = _uiState.asStateFlow()

    private val _savedRepos = MutableStateFlow<List<RepoData>>(emptyList())
    val savedRepos: StateFlow<List<RepoData>> = _savedRepos.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Repo)
    val currentScreenFlow: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _swipeEnabled = MutableStateFlow(true)
    val swipeEnabledFlow: StateFlow<Boolean> = _swipeEnabled.asStateFlow()

    var swipeEnabled: Boolean
        get() = _swipeEnabled.value
        set(value) { _swipeEnabled.value = value }

    var currentScreen: Screen
        get() = _currentScreen.value
        set(value) { _currentScreen.value = value }

    private val _targetPath = MutableStateFlow<String?>(null)
    val targetPath: StateFlow<String?> = _targetPath.asStateFlow()

    private val _terminalOutput = MutableStateFlow<List<String>>(emptyList())
    val terminalOutput: StateFlow<List<String>> = _terminalOutput.asStateFlow()

    private var storageInitialized = false

    fun loadSavedRepos() {
        viewModelScope.launch {
            val repos = repoDataStore.getAllRepos()
            _savedRepos.value = repos
        }
    }

    suspend fun addRepo(path: String, alias: String? = null): Boolean {
        val repo = RepoData(path = path, alias = alias)
        val result = repoDataStore.addRepo(repo)
        if (result) {
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
            loadSavedRepos()
        }
    }

    fun initializeStorage(context: Context) {
        if (storageInitialized) return
        storageInitialized = true

        viewModelScope.launch {
            val currentRepoPath = repoDataStore.getCurrentRepoPath()
            if (currentRepoPath != null) {
                _targetPath.value = currentRepoPath
                _uiState.value = _uiState.value.copy(targetPath = currentRepoPath)
                currentScreen = Screen.Status
            }
            loadSavedRepos()
        }
    }

    suspend fun setCurrentRepo(path: String?) {
        repoDataStore.setCurrentRepo(path)
        if (path != null) {
            _targetPath.value = path
            _uiState.value = _uiState.value.copy(targetPath = path)
            repoDataStore.updateLastAccessed(path)
        }
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
            _uiState.value = _uiState.value.copy(isLoading = true)
            appendTerminalLog("git clone $uri", "Cloning...")

            cloneUseCase(uri, localPath, branch, credentials)
                .onSuccess { result ->
                    appendTerminalLog("git clone $uri", result)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    addRepo(localPath, null)
                    onResult(Result.success(result))
                }
                .onFailure { e ->
                    val errorMsg = "Error: ${e.message}"
                    appendTerminalLog("git clone $uri", errorMsg)
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
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
            _uiState.value = _uiState.value.copy(isLoading = true)
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

            cloneUseCase(uri, localPath, branch, credentials)
                .onSuccess { result ->
                    appendTerminalLog("git clone $uri", result)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    addRepo(localPath, null)
                    onResult(Result.success(result))
                }
                .onFailure { e ->
                    val errorMsg = "Error: ${e.message}"
                    appendTerminalLog("git clone $uri", errorMsg)
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                    onResult(Result.failure(e))
                }
        }
    }

    private fun appendTerminalLog(command: String, result: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] > $command\n$result"
        _terminalOutput.value += line
    }
}
