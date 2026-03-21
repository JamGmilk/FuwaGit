package jamgmilk.obsigit.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import androidx.core.content.edit

enum class AppPage {
    GitTerminal,
    Vault,
    Settings
}

sealed class RootStatus {
    data object Idle : RootStatus()
    data object Checking : RootStatus()
    data object Granted : RootStatus()
    data object Denied : RootStatus()
}

data class VaultFolderItem(
    val name: String,
    val path: String,
    val owner: String,
    val isGitRepo: Boolean,
    val isDirectory: Boolean,
    val localPath: String?
)

data class PathScanItem(
    val id: String,
    val name: String,
    val path: String,
    val source: String,
    val permissionHint: String,
    val isActive: Boolean,
    val isRemovable: Boolean,
    val uriText: String? = null
)

class AppViewModel : ViewModel() {
    private companion object {
        const val PREFS_NAME = "obsigit_prefs"
        const val PREF_KEY_GRANTED_TREE_URIS = "granted_tree_uris"
    }

    private val _currentPage = MutableStateFlow(AppPage.GitTerminal)
    val currentPage: StateFlow<AppPage> = _currentPage.asStateFlow()

    private val _targetPath = MutableStateFlow<String?>(null)
    val targetPath: StateFlow<String?> = _targetPath.asStateFlow()

    private val _availableTargetPaths = MutableStateFlow<List<String>>(emptyList())

    private val _isGitRepo = MutableStateFlow(false)
    val isGitRepo: StateFlow<Boolean> = _isGitRepo.asStateFlow()

    private val _gitStatusText = MutableStateFlow("Select a target vault path")
    val gitStatusText: StateFlow<String> = _gitStatusText.asStateFlow()

    private val _terminalOutput = MutableStateFlow<List<String>>(emptyList())
    val terminalOutput: StateFlow<List<String>> = _terminalOutput.asStateFlow()

    private val _rootStatus = MutableStateFlow<RootStatus>(RootStatus.Idle)
    val rootStatus: StateFlow<RootStatus> = _rootStatus.asStateFlow()

    private val _vaultItems = MutableStateFlow<List<VaultFolderItem>>(emptyList())
    val vaultItems: StateFlow<List<VaultFolderItem>> = _vaultItems.asStateFlow()

    private val _grantedTreeUris = MutableStateFlow<List<String>>(emptyList())
    val grantedTreeUris: StateFlow<List<String>> = _grantedTreeUris.asStateFlow()

    private val _pathScanItems = MutableStateFlow<List<PathScanItem>>(emptyList())
    val pathScanItems: StateFlow<List<PathScanItem>> = _pathScanItems.asStateFlow()

    private val obsidianSandboxPath = "/storage/emulated/0/Android/data/md.obsidian/files/"
    private val documentsPath = "/storage/emulated/0/Documents/"
    // TODO: 这里的 "/storage/emulated/0/" 可以间接从系统调用路径吗？
    private var storageInitialized = false

    init {
        addObsidianSandboxIfExists()
    }

    fun switchPage(page: AppPage) {
        _currentPage.value = page
    }

    fun setTargetPath(path: String) {
        _targetPath.value = path
        checkRepoStatus()
    }

    fun addGrantedTreeUri(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        _grantedTreeUris.value = (_grantedTreeUris.value + uri.toString()).distinct()
        persistGrantedTreeUris(context)
        refreshVaultItems(context)
    }

    fun removePathScan(context: Context, item: PathScanItem) {
        val uriText = item.uriText ?: return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uriText.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        _grantedTreeUris.value = _grantedTreeUris.value.filterNot { it == uriText }
        persistGrantedTreeUris(context)
        refreshVaultItems(context)
    }

    fun initializeStorage(context: Context) {
        if (storageInitialized) return
        storageInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUris = prefs.getStringSet(PREF_KEY_GRANTED_TREE_URIS, emptySet()).orEmpty()
        val persistedUris = context.contentResolver.persistedUriPermissions
            .map { it.uri.toString() }
            .toSet()
        _grantedTreeUris.value = (savedUris + persistedUris).toList()
        persistGrantedTreeUris(context)
        refreshVaultItems(context)
    }

    fun refreshPathScanItems(context: Context) {
        val appSandboxPath = appSandboxPath(context)
        val normalizedBuiltIns = setOf(
            normalizePath(appSandboxPath),
            normalizePath(obsidianSandboxPath),
            normalizePath(documentsPath)
        )
        val grantedPathByUri = _grantedTreeUris.value.associateWith { uriText ->
            AppVaultOps.readablePathFromUri(uriText.toUri())
        }

        val hasDocumentsGrant = grantedPathByUri.values.any {
            normalizePath(it) == normalizePath(documentsPath)
        }

        val manualItems = grantedPathByUri.mapNotNull { (uriText, grantedPath) ->
            val normalized = normalizePath(grantedPath)
            if (normalized in normalizedBuiltIns) {
                null
            } else {
                PathScanItem(
                    id = "manual:$uriText",
                    name = File(normalized).name.ifBlank { "Picked Folder" },
                    path = ensureTrailingSlash(grantedPath),
                    source = "Manually picked",
                    permissionHint = "Folder picker grant",
                    isActive = true,
                    isRemovable = true,
                    uriText = uriText
                )
            }
        }.sortedBy { it.name.lowercase() }

        val builtIns = listOf(
            PathScanItem(
                id = "builtin:app",
                name = "App Sandbox",
                path = appSandboxPath,
                source = "No permission needed",
                permissionHint = "Built-in",
                isActive = File(appSandboxPath).exists(),
                isRemovable = false
            ),
            PathScanItem(
                id = "builtin:obsidian",
                name = "Obsidian Sandbox",
                path = obsidianSandboxPath,
                source = "Need root granted",
                permissionHint = "Root required for full access",
                isActive = File(obsidianSandboxPath).exists(),
                isRemovable = false
            ),
            PathScanItem(
                id = "builtin:documents",
                name = "Documents",
                path = documentsPath,
                source = "Need pick \"Documents\" folder",
                permissionHint = if (hasDocumentsGrant) "Folder grant active" else "Grant required",
                isActive = hasDocumentsGrant,
                isRemovable = false
            )
        )

        _pathScanItems.value = builtIns + manualItems
    }

    fun refreshVaultItems(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val collected = mutableListOf<VaultFolderItem>()

            _grantedTreeUris.value.forEach { uriText ->
                val uri = uriText.toUri()
                val treeRoot = DocumentFile.fromTreeUri(context, uri) ?: return@forEach

                AppVaultOps.collectFolderCandidates(treeRoot).forEach { folder ->
                    collected += buildFolderItem(folder)
                }
            }

            val allItems = (collected + buildSandboxItems(context))
                .distinctBy { it.path }
                .sortedBy { it.name.lowercase() }

            _vaultItems.value = allItems

            val targetCandidates = allItems.mapNotNull { it.localPath }.distinct().sorted()
            _availableTargetPaths.value = targetCandidates

            val selectedPath = _targetPath.value
            if (selectedPath == null && targetCandidates.isNotEmpty()) {
                _targetPath.value = targetCandidates.first()
            } else if (selectedPath != null && selectedPath !in targetCandidates) {
                _targetPath.value = targetCandidates.firstOrNull()
            }

            withContext(Dispatchers.Main) {
                checkRepoStatus()
                refreshPathScanItems(context)
            }
        }
    }

    private fun buildFolderItem(folder: DocumentFile): VaultFolderItem {
        val path = AppVaultOps.readablePathFromUri(folder.uri)
        val localPath = AppVaultOps.pathToLocal(path)

        return VaultFolderItem(
            name = folder.name ?: "Unnamed folder",
            path = path,
            owner = AppVaultOps.readOwner(path),
            isGitRepo = AppGitOps.hasGitDir(localPath),
            isDirectory = folder.isDirectory,
            localPath = localPath
        )
    }

    private fun getObsidianItem(): VaultFolderItem {
        val dir = File(obsidianSandboxPath)
        val isAvailable = dir.exists()
        val localPath = if (isAvailable) obsidianSandboxPath else null
        return VaultFolderItem(
            name = "Obsidian Sandbox",
            path = obsidianSandboxPath,
            owner = if (isAvailable) AppVaultOps.readOwner(obsidianSandboxPath) else "Unavailable",
            isGitRepo = AppGitOps.hasGitDir(localPath),
            isDirectory = true,
            localPath = localPath
        )
    }

    private fun getOwnSandboxItem(context: Context): VaultFolderItem {
        val sandboxPath = appSandboxPath(context)
        val dir = File(sandboxPath)
        val isAvailable = dir.exists()
        val localPath = if (isAvailable) sandboxPath else null
        return VaultFolderItem(
            name = "App Sandbox (${context.packageName})",
            path = sandboxPath,
            owner = if (isAvailable) AppVaultOps.readOwner(sandboxPath) else "Unavailable",
            isGitRepo = AppGitOps.hasGitDir(localPath),
            isDirectory = true,
            localPath = localPath
        )
    }

    private fun buildSandboxItems(context: Context): List<VaultFolderItem> {
        return listOf(getObsidianItem(), getOwnSandboxItem(context))
    }

    private fun addObsidianSandboxIfExists() {
        val obsidian = getObsidianItem()
        _vaultItems.value = listOf(obsidian)
        _availableTargetPaths.value = listOfNotNull(obsidian.localPath)
        if (obsidian.localPath != null) {
            _targetPath.value = obsidian.localPath
        }
        checkRepoStatus()
    }

    private fun appSandboxPath(context: Context): String {
        return "/storage/emulated/0/Android/data/${context.packageName}/files/"
    }

    private fun normalizePath(path: String): String {
        return path.trim().trimEnd('/')
    }

    private fun ensureTrailingSlash(path: String): String {
        val trimmed = path.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private fun persistGrantedTreeUris(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putStringSet(PREF_KEY_GRANTED_TREE_URIS, _grantedTreeUris.value.toSet())
            }
    }

    fun checkRoot() {
        viewModelScope.launch {
            _rootStatus.value = RootStatus.Checking
            val isRooted = withContext(Dispatchers.IO) { Shell.getShell().isRoot }
            _rootStatus.value = if (isRooted) RootStatus.Granted else RootStatus.Denied
        }
    }

    private fun currentRepoDir(): File? {
        val path = _targetPath.value ?: return null
        return File(path)
    }

    private fun logToTerminal(command: String, result: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] > $command\n$result"
        _terminalOutput.value += line
    }

    fun showStatusInTerminal() {
        val dir = currentRepoDir() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AppGitOps.terminalStatus(dir)
                logToTerminal("git status", result)
            } catch (e: Exception) {
                logToTerminal("git status", "Error: ${e.message}")
            }
        }
    }

    fun checkRepoStatus() {
        val dir = currentRepoDir()
        if (dir == null) {
            _isGitRepo.value = false
            _gitStatusText.value = "Select a target vault path"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = AppGitOps.readRepoStatus(dir)
                _isGitRepo.value = status.isGitRepo
                _gitStatusText.value = status.message
            } catch (e: Exception) {
                _isGitRepo.value = false
                _gitStatusText.value = "Path: ${dir.absolutePath}\nError: ${e.message}"
            }
        }
    }

    fun initRepo() {
        val dir = currentRepoDir() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AppGitOps.initRepo(dir)
                logToTerminal("git init", result)
                checkRepoStatus()
            } catch (e: Exception) {
                logToTerminal("git init", "Error: ${e.message}")
            }
        }
    }

    fun stageAll() {
        val dir = currentRepoDir() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AppGitOps.stageAll(dir)
                logToTerminal("git add -A", result)
                checkRepoStatus()
            } catch (e: Exception) {
                logToTerminal("git add -A", "Error: ${e.message}")
            }
        }
    }

    fun unstageAll() {
        val dir = currentRepoDir() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AppGitOps.unstageAll(dir)
                logToTerminal("git reset", result)
                checkRepoStatus()
            } catch (e: Exception) {
                logToTerminal("git reset", "Error: ${e.message}")
            }
        }
    }

    fun commitChanges(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            logToTerminal("git commit", "Commit message cannot be empty")
            return
        }

        val dir = currentRepoDir() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AppGitOps.commit(dir, trimmed)
                logToTerminal("git commit -m \"$trimmed\"", result)
                checkRepoStatus()
            } catch (e: Exception) {
                logToTerminal("git commit", "Error: ${e.message}")
            }
        }
    }

    fun pullRepo() {
        val dir = currentRepoDir() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            logToTerminal("git pull", "Attempting pull. Remote auth may be required")
            try {
                val result = AppGitOps.pull(dir)
                logToTerminal("git pull", result)
                checkRepoStatus()
            } catch (e: Exception) {
                logToTerminal("git pull", "Error: ${e.message}")
            }
        }
    }

    fun pushRepo() {
        val dir = currentRepoDir() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            logToTerminal("git push", "Attempting push. Remote auth may be required")
            try {
                val result = AppGitOps.push(dir)
                logToTerminal("git push", result)
            } catch (e: Exception) {
                logToTerminal("git push", "Error: ${e.message}")
            }
        }
    }
}
