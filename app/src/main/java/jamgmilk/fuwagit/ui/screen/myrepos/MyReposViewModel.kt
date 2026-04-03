package jamgmilk.fuwagit.ui.screen.myrepos

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.core.util.PathUtils
import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.CloneOptions
import jamgmilk.fuwagit.domain.model.repo.RepoData
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.state.RepoInfo
import jamgmilk.fuwagit.domain.state.RepoStateManager
import jamgmilk.fuwagit.domain.usecase.CurrentRepoUseCase
import jamgmilk.fuwagit.domain.usecase.git.GitRepoFacade
import jamgmilk.fuwagit.domain.usecase.credential.CredentialFacade
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
    val error: String? = null,
    val untrackedFilesForClean: List<String> = emptyList(),
    val cleanedFilesForResult: List<String> = emptyList()
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
    private val gitRepo: GitRepoFacade,
    private val credential: CredentialFacade
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
    private val _untrackedFilesForClean = MutableStateFlow<List<String>>(emptyList())
    private val _cleanedFilesForResult = MutableStateFlow<List<String>>(emptyList())

    val uiState: StateFlow<RepoUiState> = combine(
        listOf(
            _savedRepos,
            currentRepoInfo,
            _isLoading,
            _error,
            _untrackedFilesForClean,
            _cleanedFilesForResult
        )
    ) { values ->
        val repos = values[0] as List<RepoData>
        val currentRepo = values[1] as RepoInfo
        val loading = values[2] as Boolean
        val error = values[3] as String?
        val untrackedFiles = values[4] as List<String>
        val cleanedFiles = values[5] as List<String>
        
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
            error = error,
            untrackedFilesForClean = untrackedFiles,
            cleanedFilesForResult = cleanedFiles
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
        return gitRepo.clean(path, dryRun).map {
            if (dryRun) {
                // 更新 untracked files 列表用于预览
                _untrackedFilesForClean.value = it.files
            }
            it.toString()
        }
    }

    /**
     * 请求 Clean 预览：执行 dry-run 获取将要删除的文件列表
     */
    fun requestCleanPreview() {
        val path = currentRepoInfo.value.repoPath ?: return

        viewModelScope.launch {
            _isLoading.value = true
            gitRepo.clean(path, dryRun = true).fold(
                onSuccess = { result ->
                    _isLoading.value = false
                    if (result.files.isEmpty()) {
                        _error.value = "No untracked files to clean"
                        _untrackedFilesForClean.value = emptyList()
                    } else {
                        _untrackedFilesForClean.value = result.files
                    }
                },
                onFailure = { e ->
                    _isLoading.value = false
                    _error.value = "Failed to get untracked files: ${e.message}"
                    _untrackedFilesForClean.value = emptyList()
                }
            )
        }
    }

    /**
     * 确认执行 Clean 操作（实际删除文件）
     */
    fun confirmCleanUntracked() {
        val path = currentRepoInfo.value.repoPath ?: return
        val filesToClean = _untrackedFilesForClean.value

        viewModelScope.launch {
            // 先执行 dry-run 获取实际会被删除的文件
            gitRepo.clean(path, dryRun = true).fold(
                onSuccess = { dryRunResult ->
                    // 然后执行实际清理
                    gitRepo.clean(path, dryRun = false).fold(
                        onSuccess = {
                            _isLoading.value = false
                            _cleanedFilesForResult.value = dryRunResult.files
                            _untrackedFilesForClean.value = emptyList()
                            _error.value = null
                            loadSavedRepos() // 刷新仓库列表（大小可能已变化）
                        },
                        onFailure = { e ->
                            _isLoading.value = false
                            _error.value = "Failed to clean: ${e.message}"
                            _untrackedFilesForClean.value = emptyList()
                        }
                    )
                },
                onFailure = { e ->
                    _isLoading.value = false
                    _error.value = "Failed to get file list: ${e.message}"
                    _untrackedFilesForClean.value = emptyList()
                }
            )
        }
    }

    /**
     * 清除 Clean 预览状态
     */
    fun clearCleanPreview() {
        _untrackedFilesForClean.value = emptyList()
    }

    /**
     * 清除 Clean 结果状态
     */
    fun clearCleanResult() {
        _cleanedFilesForResult.value = emptyList()
    }

    suspend fun getRepoInfo(localPath: String): Map<String, String> {
        return gitRepo.getRepoInfo(localPath)
    }

    suspend fun getRemotes(localPath: String): List<Pair<String, String>> {
        return gitRepo.getRemotes(localPath)
            .getOrNull()
            ?.map { it.name to it.fetchUrl }
            ?: emptyList()
    }

    suspend fun getRemoteUrl(localPath: String, name: String = "origin"): String? {
        return gitRepo.getRemoteUrl(localPath, name)
    }

    suspend fun addLocalRepository(path: String, alias: String?, remoteUrl: String?) {
        withContext(Dispatchers.IO) {
            val gitDir = File(path, ".git")
            if (!gitDir.exists()) {
                gitRepo.initRepo(path).getOrThrow()
                if (!remoteUrl.isNullOrBlank()) {
                    gitRepo.configureRemote(path, "origin", remoteUrl).getOrThrow()
                }
            }
        }
        addRepo(path, alias)
    }

    fun configureRemote(localPath: String, name: String, url: String) {
        viewModelScope.launch {
            gitRepo.configureRemote(localPath, name, url)
        }
    }

    suspend fun isDirectoryEmpty(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            val dir = File(path)
            !dir.exists() || dir.listFiles()?.isEmpty() != false
        }
    }

    suspend fun getHttpsCredentials(): List<HttpsCredential> {
        val result = credential.getHttpsCredentials()
        return if (result is AppResult.Success) result.data else emptyList()
    }

    suspend fun getHttpsPassword(uuid: String): String? {
        val result = credential.getHttpsPassword(uuid)
        return if (result is AppResult.Success) result.data else null
    }

    suspend fun getSshKeys(): List<SshKey> {
        val result = credential.getSshKeys()
        return if (result is AppResult.Success) result.data else emptyList()
    }

    suspend fun getSshPrivateKey(uuid: String): String? {
        val result = credential.getSshPrivateKey(uuid)
        return if (result is AppResult.Success) result.data else null
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

            val httpsCreds = getHttpsCredentials()
            val sshKeys = getSshKeys()

            val credentials: CloneCredential? = credential.resolveCredentials(
                httpsCredentialUuid,
                sshKeyUuid,
                httpsCreds,
                sshKeys
            )

            gitRepo.clone(uri, localPath, credentials, CloneOptions())
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
