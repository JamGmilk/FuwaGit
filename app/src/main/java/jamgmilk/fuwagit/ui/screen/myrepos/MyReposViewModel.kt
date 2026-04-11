package jamgmilk.fuwagit.ui.screen.myrepos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.data.jgit.GitConfigManager
import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.model.git.CloneOptions
import jamgmilk.fuwagit.domain.model.repo.RepoData
import jamgmilk.fuwagit.domain.usecase.credential.CredentialFacade
import jamgmilk.fuwagit.domain.usecase.git.GitRepoFacade
import jamgmilk.fuwagit.ui.state.RepoInfo
import jamgmilk.fuwagit.ui.state.RepoStateManager
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
    private val credential: CredentialFacade,
    private val gitConfigManager: GitConfigManager
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

    private fun buildRepoItems(repos: List<RepoData>, currentPath: String?, currentSizes: Map<String, Long>): List<RepoFolderItem> {
        return repos.map { repo ->
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
    }

    init {
        viewModelScope.launch {
            repoDataStore.getSavedReposFlow().collectLatest { repos ->
                val currentPath = currentRepoManager.getRepoPath()
                val currentSizes = _uiState.value.repoSizes
                val items = buildRepoItems(repos, currentPath, currentSizes)
                _uiState.update { it.copy(savedRepos = repos, repoItems = items) }
                items.filterNot { currentSizes.containsKey(it.path) }.forEach { calculateSizeAsync(it.path) }
            }
        }

        viewModelScope.launch {
            currentRepoManager.repoInfo.collectLatest { info ->
                info.repoPath?.let { currentPath ->
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
            val items = buildRepoItems(repos, currentPath, currentSizes)
            _uiState.update { it.copy(savedRepos = repos, repoItems = items) }
            items.filterNot { currentSizes.containsKey(it.path) }.forEach { calculateSizeAsync(it.path) }
        }
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
        if (result && currentRepoManager.getRepoPath() == null) {
            currentRepoManager.setRepoPath(path)
        }
        return result
    }

    suspend fun removeRepo(path: String): Boolean {
        val result = repoDataStore.removeRepo(path)
        if (result && currentRepoManager.getRepoPath() == path) {
            currentRepoManager.clearRepo()
        }
        return result
    }

    suspend fun setCurrentRepo(path: String?) {
        currentRepoManager.setRepoPath(path)
    }

    fun requestCleanPreview() {
        val path = currentRepoInfo.value.repoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isCleanPreviewing = true, cleanMessage = null, untrackedFilesForClean = emptyList()) }
            gitRepo.clean(path, dryRun = true).onSuccess { cleanResult ->
                _uiState.update {
                    it.copy(
                        isCleanPreviewing = false,
                        cleanMessage = if (cleanResult.files.isEmpty()) "No untracked files to clean" else null,
                        untrackedFilesForClean = cleanResult.files
                    )
                }
            }.onError { e ->
                _uiState.update {
                    it.copy(
                        isCleanPreviewing = false,
                        cleanMessage = "Failed to get untracked files: ${e.message}",
                        untrackedFilesForClean = emptyList()
                    )
                }
            }
        }
    }

    fun confirmCleanUntracked() {
        val path = currentRepoInfo.value.repoPath ?: return
        val originallyPreviewedFiles = _uiState.value.untrackedFilesForClean

        viewModelScope.launch {
            _uiState.update { it.copy(isCleanPreviewing = true, cleanMessage = null) }

            val latestDryRunResult = gitRepo.clean(path, dryRun = true)

            latestDryRunResult.onSuccess { latestDryRunResult ->
                val verifiedFiles = verifyUntrackedFilesStillExist(path, originallyPreviewedFiles, latestDryRunResult.files)

                if (verifiedFiles.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isCleanPreviewing = false,
                            cleanMessage = "No files to clean: all previously untracked files have been modified, staged, or already deleted",
                            untrackedFilesForClean = emptyList()
                        )
                    }
                    return@launch
                }

                gitRepo.clean(path, dryRun = false).onSuccess {
                    _uiState.update {
                        it.copy(
                            isCleanPreviewing = false,
                            cleanedFilesForResult = verifiedFiles,
                            untrackedFilesForClean = emptyList(),
                            cleanMessage = null
                        )
                    }
                }.onError { e ->
                    _uiState.update {
                        it.copy(
                            isCleanPreviewing = false,
                            cleanMessage = "Failed to clean: ${e.message}",
                            untrackedFilesForClean = emptyList()
                        )
                    }
                }
            }.onError { e ->
                _uiState.update {
                    it.copy(
                        isCleanPreviewing = false,
                        cleanMessage = "Failed to get file list: ${e.message}",
                        untrackedFilesForClean = emptyList()
                    )
                }
            }
        }
    }

    private fun verifyUntrackedFilesStillExist(
        repoPath: String,
        originalFiles: List<String>,
        latestFiles: List<String>
    ): List<String> {
        val latestFileSet = latestFiles.toSet()
        return originalFiles.filter { file ->
            file in latestFileSet && File(repoPath, file).exists()
        }
    }

    fun clearCleanPreview() {
        _uiState.update { it.copy(untrackedFilesForClean = emptyList(), isCleanPreviewing = false, cleanMessage = null) }
    }

    fun clearCleanResult() {
        _uiState.update { it.copy(cleanedFilesForResult = emptyList(), cleanMessage = null) }
    }

    suspend fun getRepoInfo(localPath: String): Map<String, String> = gitRepo.getRepoInfo(localPath)

    suspend fun getRemotes(localPath: String): List<Pair<String, String>> {
        return gitRepo.getRemotes(localPath).getOrNull()?.map { it.name to it.fetchUrl } ?: emptyList()
    }

    fun getRepoGitConfig(localPath: String): String = gitConfigManager.getAllRepoConfig(localPath)

    suspend fun getRemoteUrl(localPath: String, name: String = "origin"): String? = gitRepo.getRemoteUrl(localPath, name)

    suspend fun addLocalRepository(path: String, alias: String?, remoteUrl: String?) {
        withContext(Dispatchers.IO) {
            val gitDir = File(path, ".git")
            if (!gitDir.exists()) {
                gitRepo.initRepo(path).getOrNull() ?: throw Exception("Failed to initialize repository")
                if (!remoteUrl.isNullOrBlank()) {
                    gitRepo.configureRemote(path, "origin", remoteUrl).getOrNull() ?: throw Exception("Failed to configure remote")
                }
            }
        }
        addRepo(path, alias)
    }

    fun configureRemote(localPath: String, name: String, url: String, httpsCredentialUuid: String? = null, sshKeyUuid: String? = null) {
        viewModelScope.launch {
            gitRepo.configureRemote(localPath, name, url)
            val credentialId = httpsCredentialUuid ?: sshKeyUuid
            if (credentialId != null) {
                repoDataStore.updateRepo(localPath) { it.copy(credentialId = credentialId) }
            }
        }
    }

    fun loadCredentials() {
        viewModelScope.launch {
            val httpsCredentials = getHttpsCredentials()
            val sshKeyList = getSshKeys()
            _uiState.update { it.copy(httpsCredentials = httpsCredentials, sshKeys = sshKeyList) }
        }
    }

    suspend fun isDirectoryEmpty(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            val dir = File(path)
            !dir.exists() || dir.listFiles()?.isEmpty() != false
        }
    }

    suspend fun getHttpsCredentials(): List<HttpsCredential> = credential.getHttpsCredentials().getOrNull() ?: emptyList()

    suspend fun getSshKeys(): List<SshKey> = credential.getSshKeys().getOrNull() ?: emptyList()

    fun cloneWithCredentials(
        uri: String,
        localPath: String,
        branch: String? = null,
        httpsCredentialUuid: String? = null,
        sshKeyUuid: String? = null,
        cloneOptions: CloneOptions = CloneOptions(),
        onResult: (AppResult<String>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val httpsCredentials = getHttpsCredentials()
            val sshKeys = getSshKeys()

            val credentials = credential.resolveCredentials(httpsCredentialUuid, sshKeyUuid, httpsCredentials, sshKeys, uri)

            gitRepo.clone(uri, localPath, credentials, cloneOptions).onSuccess { cloneResult ->
                _uiState.update { it.copy(isLoading = false) }
                addRepo(localPath, null)
                onResult(AppResult.Success(cloneResult))
            }.onError { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                onResult(AppResult.Error(e))
            }
        }
    }
}
