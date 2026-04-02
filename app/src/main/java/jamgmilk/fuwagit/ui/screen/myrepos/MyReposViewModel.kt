package jamgmilk.fuwagit.ui.screen.myrepos

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.core.util.PathUtils
import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.repo.RepoData
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.state.RepoInfo
import jamgmilk.fuwagit.domain.state.RepoStateManager
import jamgmilk.fuwagit.domain.usecase.CurrentRepoUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshKeysUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshPrivateKeyUseCase
import jamgmilk.fuwagit.domain.usecase.git.CleanUseCase
import jamgmilk.fuwagit.domain.usecase.git.CloneRepositoryUseCase
import jamgmilk.fuwagit.domain.usecase.git.ConfigureRemoteUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetRemotesUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetRemoteUrlUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetRepoInfoUseCase
import jamgmilk.fuwagit.domain.usecase.git.InitRepoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class RepoFolderItem(
    val path: String,
    val alias: String,
    val isGitRepo: Boolean,
    val isRemote: Boolean,
    val isActive: Boolean,
    val lastModified: Long = 0L,
    val size: Long = 0L
) {
    val shortPath: String
        get() = PathUtils.getShortPath(path)

    val formattedSize: String
        get() {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
                else -> String.format("%.1f GB", size / (1024.0 * 1024 * 1024))
            }
        }
}

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
    private val getSshPrivateKeyUseCase: GetSshPrivateKeyUseCase,
    private val initRepoUseCase: InitRepoUseCase,
    private val getRemotesUseCase: GetRemotesUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val currentRepoInfo: StateFlow<RepoInfo> = currentRepoManager.repoInfo

    private val _savedRepos = MutableStateFlow<List<RepoData>>(emptyList())

    private fun calculateFolderSize(path: String): Long {
        return try {
            val file = File(path)
            if (!file.exists()) return 0L
            if (!file.isDirectory) return file.length()

            var size = 0L
            val queue = ArrayDeque<File>()
            queue.add(file)
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                current.listFiles()?.forEach { child ->
                    if (child.isDirectory) {
                        queue.add(child)
                    } else {
                        size += child.length()
                    }
                }
            }
            size
        } catch (e: Exception) {
            0L
        }
    }

    private val _repoSizes = mutableStateMapOf<String, Long>()

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
                    isGitRepo = File(repo.path, ".git").exists(),
                    isRemote = false,
                    isActive = repo.path == currentRepo.repoPath,
                    lastModified = repo.lastAccessedAt,
                    size = _repoSizes[repo.path] ?: 0L
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

    private var storageInitialized = false

    fun loadSavedRepos() {
        viewModelScope.launch {
            val repos = repoDataStore.getAllRepos()
            _savedRepos.value = repos
            
            // Background size calculation
            repos.forEach { repo ->
                if (!_repoSizes.containsKey(repo.path)) {
                    calculateSizeAsync(repo.path)
                }
            }
        }
    }

    private fun calculateSizeAsync(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val size = calculateFolderSize(path)
            withContext(Dispatchers.Main) {
                _repoSizes[path] = size
            }
        }
    }

    fun initializeStorage(context: Context) {
        if (storageInitialized) return
        storageInitialized = true
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

    suspend fun removeRepo(item: RepoFolderItem) {
        repoDataStore.removeRepo(item.path)
        if (currentRepoManager.getRepoPath() == item.path) {
            currentRepoManager.clearRepo()
        }
        loadSavedRepos()
    }

    suspend fun setCurrentRepo(path: String?) {
        currentRepoManager.setRepoPath(path)
    }

    fun refreshRepoItems() {
        loadSavedRepos()
    }

    suspend fun cleanRepo(path: String, dryRun: Boolean = false): Result<String> {
        return cleanUseCase(path, dryRun)
    }

    suspend fun getRepoInfo(localPath: String): Map<String, String> {
        return getRepoInfoUseCase(localPath)
    }

    suspend fun getRemotes(localPath: String): List<Pair<String, String>> {
        return getRemotesUseCase(localPath)
            .map { remotes -> remotes.map { it.name to it.fetchUrl } }
            .getOrNull() ?: emptyList()
    }

    suspend fun getRemoteUrl(localPath: String, name: String = "origin"): String? {
        return getRemoteUrlUseCase(localPath, name)
    }

    suspend fun addLocalRepository(path: String, alias: String?, remoteUrl: String?) {
        withContext(Dispatchers.IO) {
            val gitDir = File(path, ".git")
            if (!gitDir.exists()) {
                initRepoUseCase(path).getOrThrow()
                if (!remoteUrl.isNullOrBlank()) {
                    configureRemoteUseCase(path, "origin", remoteUrl)
                }
            }
        }
        addRepo(path, alias)
    }

    fun configureRemote(localPath: String, name: String, url: String) {
        viewModelScope.launch {
            configureRemoteUseCase(localPath, name, url)
        }
    }

    suspend fun isDirectoryEmpty(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            val dir = File(path)
            !dir.exists() || dir.listFiles()?.isEmpty() != false
        }
    }

    suspend fun getHttpsCredentials(): List<HttpsCredential> {
        return getHttpsCredentialsUseCase()
            .getOrNull() ?: emptyList()
    }

    suspend fun getHttpsPassword(uuid: String): String? {
        return getHttpsPasswordUseCase(uuid).getOrNull()
    }

    suspend fun getSshKeys(): List<SshKey> {
        return getSshKeysUseCase()
            .getOrNull() ?: emptyList()
    }

    suspend fun getSshPrivateKey(uuid: String): String? {
        return getSshPrivateKeyUseCase(uuid).getOrNull()
    }

    suspend fun showHttpsCredentialSelector(): String? {
        return null
    }

    suspend fun showSshKeySelector(): String? {
        return null
    }

    fun cloneWithCredentials(
        uri: String,
        localPath: String,
        branch: String? = null,
        httpsCredentialUuid: String? = null,
        sshKeyUuid: String? = null,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

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
                    _isLoading.value = false
                    addRepo(localPath, null)
                    onResult(Result.success(result))
                }
                .onFailure { e ->
                    _isLoading.value = false
                    _error.value = e.message
                    onResult(Result.failure(e))
                }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
