package jamgmilk.fuwagit.ui.screen.myrepos

import android.content.Context
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
import jamgmilk.fuwagit.ui.state.RepoInfo
import jamgmilk.fuwagit.ui.state.RepoStateManager
import jamgmilk.fuwagit.domain.usecase.git.GitRepoFacade
import jamgmilk.fuwagit.domain.usecase.credential.CredentialFacade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MyReposViewModel @Inject constructor(
    private val repoDataStore: RepoDataStore,
    private val currentRepoManager: RepoStateManager,
    private val gitRepo: GitRepoFacade,
    private val credential: CredentialFacade
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoUiState())
    val uiState: StateFlow<RepoUiState> = _uiState.asStateFlow()

    val currentRepoInfo: StateFlow<RepoInfo> = currentRepoManager.repoInfo

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

    private var storageInitialized = false

    init {
        viewModelScope.launch {
            repoDataStore.getSavedReposFlow().collectLatest { repos ->
                val currentPath = currentRepoManager.getRepoPath()
                val currentSizes = _uiState.value.repoSizes
                val items = repos.map { repo ->
                    RepoFolderItem(
                        path = repo.path,
                        alias = repo.displayName,
                        isGitRepo = File(repo.path, ".git").exists(),
                        isRemote = false,
                        isActive = repo.path == currentPath,
                        lastModified = repo.lastAccessedAt,
                        size = currentSizes[repo.path] ?: 0L
                    )
                }
                _uiState.update { it.copy(savedRepos = repos, repoItems = items) }
                items.forEach { item ->
                    if (!currentSizes.containsKey(item.path)) {
                        calculateSizeAsync(item.path)
                    }
                }
            }
        }

        viewModelScope.launch {
            currentRepoManager.repoInfo.collectLatest { info ->
                val currentPath = info.repoPath
                if (currentPath != null) {
                    _uiState.update { state ->
                        state.copy(repoItems = state.repoItems.map { item ->
                            item.copy(isActive = item.path == currentPath)
                        })
                    }
                }
            }
        }
    }

    fun loadSavedRepos() {
        viewModelScope.launch {
            val repos = repoDataStore.getAllRepos()
            val currentPath = currentRepoManager.getRepoPath()
            val currentSizes = _uiState.value.repoSizes
            val items = repos.map { repo ->
                RepoFolderItem(
                    path = repo.path,
                    alias = repo.displayName,
                    isGitRepo = File(repo.path, ".git").exists(),
                    isRemote = false,
                    isActive = repo.path == currentPath,
                    lastModified = repo.lastAccessedAt,
                    size = currentSizes[repo.path] ?: 0L
                )
            }
            _uiState.update { it.copy(savedRepos = repos, repoItems = items) }
            items.forEach { item ->
                if (!currentSizes.containsKey(item.path)) {
                    calculateSizeAsync(item.path)
                }
            }
        }
    }

    fun initializeStorage(context: Context) {
        if (storageInitialized) return
        storageInitialized = true
    }

    private fun calculateSizeAsync(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val size = calculateFolderSize(path)
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    state.copy(
                        repoSizes = state.repoSizes + (path to size),
                        repoItems = state.repoItems.map { item ->
                            if (item.path == path) item.copy(size = size) else item
                        }
                    )
                }
            }
        }
    }

    suspend fun addRepo(path: String, alias: String? = null): Boolean {
        val repo = RepoData(path = path, alias = alias)
        val result = repoDataStore.addRepo(repo)
        if (result) {
            if (currentRepoManager.getRepoPath() == null) {
                currentRepoManager.setRepoPath(path)
            }
        }
        return result
    }

    suspend fun removeRepo(repo: RepoData): Boolean {
        return repoDataStore.removeRepo(repo.path)
    }

    suspend fun removeRepo(item: RepoFolderItem) {
        repoDataStore.removeRepo(item.path)
        if (currentRepoManager.getRepoPath() == item.path) {
            currentRepoManager.clearRepo()
        }
    }

    suspend fun setCurrentRepo(path: String?) {
        currentRepoManager.setRepoPath(path)
    }

    fun refreshRepoItems() {
        loadSavedRepos()
    }

    suspend fun cleanRepo(path: String, dryRun: Boolean = false): Result<String> {
        return gitRepo.clean(path, dryRun).map { result ->
            if (dryRun) {
                // 更新 untracked files 列表用于预览
                _uiState.update { it.copy(untrackedFilesForClean = result.files) }
            }
            result.toString()
        }
    }

    /**
     * 请求 Clean 预览：执行 dry-run 获取将要删除的文件列表
     */
    fun requestCleanPreview() {
        val path = currentRepoInfo.value.repoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isCleanPreviewing = true, cleanMessage = null, untrackedFilesForClean = emptyList()) }
            gitRepo.clean(path, dryRun = true).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isCleanPreviewing = false,
                            cleanMessage = if (result.files.isEmpty()) "No untracked files to clean" else null,
                            untrackedFilesForClean = result.files
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isCleanPreviewing = false,
                            cleanMessage = "Failed to get untracked files: ${e.message}",
                            untrackedFilesForClean = emptyList()
                        )
                    }
                }
            )
        }
    }

    /**
     * 确认执行 Clean 操作（实际删除文件）
     */
    fun confirmCleanUntracked() {
        val path = currentRepoInfo.value.repoPath ?: return
        val filesToClean = _uiState.value.untrackedFilesForClean

        viewModelScope.launch {
            _uiState.update { it.copy(isCleanPreviewing = true, cleanMessage = null) }
            gitRepo.clean(path, dryRun = true).fold(
                onSuccess = { dryRunResult ->
                    gitRepo.clean(path, dryRun = false).fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(
                                    isCleanPreviewing = false,
                                    cleanedFilesForResult = dryRunResult.files,
                                    untrackedFilesForClean = emptyList(),
                                    cleanMessage = null
                                )
                            }
                        },
                        onFailure = { e ->
                            _uiState.update {
                                it.copy(
                                    isCleanPreviewing = false,
                                    cleanMessage = "Failed to clean: ${e.message}",
                                    untrackedFilesForClean = emptyList()
                                )
                            }
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isCleanPreviewing = false,
                            cleanMessage = "Failed to get file list: ${e.message}",
                            untrackedFilesForClean = emptyList()
                        )
                    }
                }
            )
        }
    }

    /**
     * 清除 Clean 预览状态
     */
    fun clearCleanPreview() {
        _uiState.update { it.copy(untrackedFilesForClean = emptyList(), isCleanPreviewing = false, cleanMessage = null) }
    }

    /**
     * 清除 Clean 结果状态
     */
    fun clearCleanResult() {
        _uiState.update { it.copy(cleanedFilesForResult = emptyList(), cleanMessage = null) }
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
        return credential.getHttpsCredentials().getOrNull() ?: emptyList()
    }

    suspend fun getHttpsPassword(uuid: String): String? {
        return credential.getHttpsPassword(uuid).getOrNull()
    }

    suspend fun getSshKeys(): List<SshKey> {
        return credential.getSshKeys().getOrNull() ?: emptyList()
    }

    suspend fun getSshPrivateKey(uuid: String): String? {
        return credential.getSshPrivateKey(uuid).getOrNull()
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
        cloneOptions: CloneOptions = CloneOptions(),
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val httpsCreds = getHttpsCredentials()
            val sshKeys = getSshKeys()

            val credentials: CloneCredential? = credential.resolveCredentials(
                httpsCredentialUuid,
                sshKeyUuid,
                httpsCreds,
                sshKeys,
                uri
            )

            gitRepo.clone(uri, localPath, credentials, cloneOptions)
                .onSuccess { result ->
                    _uiState.update { it.copy(isLoading = false) }
                    addRepo(localPath, null)
                    onResult(Result.success(result))
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                    onResult(Result.failure(e))
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
