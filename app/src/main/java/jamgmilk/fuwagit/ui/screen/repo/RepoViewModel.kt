package jamgmilk.fuwagit.ui.screen.repo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jamgmilk.fuwagit.data.source.RepoPathUtils
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

data class RepoFolderItem(
    val id: String,
    val name: String,
    val path: String,
    val isGitRepo: Boolean,
    val isDirectory: Boolean = true,
    val localPath: String? = null,
    val source: String = "Folder picker grant",
    val permissionHint: String = "Granted",
    val isActive: Boolean,
    val isRemovable: Boolean = true,
    val uriText: String? = null,
    val lastModified: Long = 0L,
)

data class RepoUiState(
    val repoItems: List<RepoFolderItem> = emptyList(),
    val targetPath: String? = null,
    val grantedTreeUris: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class RepoViewModel(
    private val getRepoInfoUseCase: GetRepoInfoUseCase,
    private val getRemoteUrlUseCase: GetRemoteUrlUseCase,
    private val configureRemoteUseCase: ConfigureRemoteUseCase,
    private val hasGitDirUseCase: HasGitDirUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoUiState())
    val uiState: StateFlow<RepoUiState> = _uiState.asStateFlow()

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

    fun setTargetPath(context: Context, path: String?) {
        _targetPath.value = path
        saveTargetPath(context, path)
    }

    fun addGrantedTreeUri(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        rebuildGrantedFolders(context)
    }

    fun removeRepo(context: Context, item: RepoFolderItem) {
        val uriText = item.uriText ?: return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uriText.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        rebuildGrantedFolders(context)
    }

    fun initializeStorage(context: Context) {
        if (storageInitialized) return
        storageInitialized = true
        val path = loadTargetPath(context)
        _targetPath.value = path
        rebuildGrantedFolders(context)
    }

    fun refreshPersistedUris(context: Context) {
        _uiState.value = _uiState.value.copy(
            grantedTreeUris = context.contentResolver.persistedUriPermissions.asSequence()
                .filter { it.isReadPermission || it.isWritePermission }
                .map { it.uri.toString() }
                .distinct()
                .toList()
        )
    }

    fun refreshRepoItems(context: Context) {
        rebuildGrantedFolders(context)
    }

    private fun rebuildGrantedFolders(context: Context) {
        refreshPersistedUris(context)
        val grantedUrisSnapshot = _uiState.value.grantedTreeUris

        viewModelScope.launch(Dispatchers.IO) {
            val repoFolders = buildGrantedRepoFolders(grantedUrisSnapshot)
            _uiState.value = _uiState.value.copy(repoItems = repoFolders)

            val targetCandidates = repoFolders
                .mapNotNull { it.localPath }
                .filter { File(it).exists() && File(it).isDirectory }
                .distinct()
                .sorted()

            val selectedPath = _targetPath.value
            if (selectedPath != null && (selectedPath !in targetCandidates)) {
                _targetPath.value = null
                saveTargetPath(context, null)
            }
        }
    }

    private suspend fun buildGrantedRepoFolders(grantedUris: List<String>): List<RepoFolderItem> = coroutineScope {
        val uniqueByPath = linkedMapOf<String, String>()

        grantedUris.forEach { uriText ->
            val readablePath = RepoPathUtils.readablePathFromUri(uriText.toUri())
            val normalizedPath = RepoPathUtils.normalizeLocalPath(readablePath)
            if (normalizedPath.startsWith("/") && (normalizedPath !in uniqueByPath)) {
                uniqueByPath[normalizedPath] = uriText
            }
        }

        uniqueByPath.entries.map { (normalizedPath, uriText) ->
            async {
                val path = RepoPathUtils.ensureTrailingSlash(normalizedPath)
                val localPath = normalizedPath
                val dir = File(localPath)
                val isGitRepo = hasGitDirUseCase(localPath)
                RepoFolderItem(
                    id = "grant:$uriText",
                    uriText = uriText,
                    name = File(normalizedPath).name.ifBlank { "Picked Folder" },
                    path = path,
                    localPath = localPath,
                    isGitRepo = isGitRepo,
                    isActive = dir.exists() && dir.isDirectory,
                    lastModified = if (dir.exists()) dir.lastModified() else 0L,
                )
            }
        }.awaitAll().sortedBy { it.name.lowercase() }
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

    private fun appendTerminalLog(command: String, result: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] > $command\n$result"
        _terminalOutput.value += line
    }

    private fun saveTargetPath(context: Context, path: String?) {
        val prefs = context.getSharedPreferences("fuwagit_prefs", Context.MODE_PRIVATE)
        prefs.edit { putString("target_path", path) }
    }

    private fun loadTargetPath(context: Context): String? {
        val prefs = context.getSharedPreferences("fuwagit_prefs", Context.MODE_PRIVATE)
        return prefs.getString("target_path", null)
    }
}
