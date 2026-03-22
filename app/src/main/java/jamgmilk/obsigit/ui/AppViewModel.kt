package jamgmilk.obsigit.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
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
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val isGitRepo: Boolean,
    val isDirectory: Boolean,
    val localPath: String?
)

data class PathScanItem(
    val id: String,
    val name: String,
    val path: String,
    val isGitRepo: Boolean,
    val source: String,
    val permissionHint: String,
    val isActive: Boolean,
    val isRemovable: Boolean,
    val uriText: String? = null
)

private data class GrantedRepoFolder(
    val uriText: String,
    val name: String,
    val path: String,
    val localPath: String,
    val isGitRepo: Boolean
)

class AppViewModel : ViewModel() {
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

    private var storageInitialized = false
    private var appContext: Context? = null
    private var mirrorPathToUri: Map<String, String> = emptyMap()

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
        rebuildGrantedFolders(context)
    }

    fun removePathScan(context: Context, item: PathScanItem) {
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
        rebuildGrantedFolders(context)
    }

    fun refreshPersistedUris(context: Context) {
        _grantedTreeUris.value = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission || it.isWritePermission }
            .map { it.uri.toString() }
            .distinct()
    }

    fun refreshVaultItems(context: Context) {
        rebuildGrantedFolders(context)
    }

    private fun rebuildGrantedFolders(context: Context) {
        appContext = context.applicationContext
        refreshPersistedUris(context)
        val grantedUrisSnapshot = _grantedTreeUris.value

        viewModelScope.launch(Dispatchers.IO) {
            val grantedFolders = buildGrantedRepoFolders(context, grantedUrisSnapshot)
            mirrorPathToUri = grantedFolders.associate { it.localPath to it.uriText }

            val vaultFolders = grantedFolders.map { folder ->
                VaultFolderItem(
                    name = folder.name,
                    path = folder.path,
                    isGitRepo = folder.isGitRepo,
                    isDirectory = true,
                    localPath = folder.localPath
                )
            }

            val pathScans = grantedFolders.map { folder ->
                PathScanItem(
                    id = "grant:${folder.uriText}",
                    name = folder.name,
                    path = folder.path,
                    isGitRepo = folder.isGitRepo,
                    source = "Folder picker grant",
                    permissionHint = "Granted",
                    isActive = true,
                    isRemovable = true,
                    uriText = folder.uriText
                )
            }

            _vaultItems.value = vaultFolders
            _pathScanItems.value = pathScans

            val targetCandidates = vaultFolders
                .mapNotNull { it.localPath }
                .distinct()
                .sorted()
            _availableTargetPaths.value = targetCandidates

            val selectedPath = _targetPath.value
            if (selectedPath == null && targetCandidates.isNotEmpty()) {
                _targetPath.value = targetCandidates.first()
            } else if (selectedPath != null && selectedPath !in targetCandidates) {
                _targetPath.value = targetCandidates.firstOrNull()
            }

            withContext(Dispatchers.Main) {
                checkRepoStatus()
            }
        }
    }

    private fun buildGrantedRepoFolders(context: Context, grantedUris: List<String>): List<GrantedRepoFolder> {
        val uniqueByPath = linkedMapOf<String, String>()

        grantedUris.forEach { uriText ->
            val readablePath = AppVaultOps.readablePathFromUri(uriText.toUri())
            val normalizedPath = normalizePath(readablePath)
            if (normalizedPath.isNotEmpty() && normalizedPath !in uniqueByPath) {
                uniqueByPath[normalizedPath] = uriText
            }
        }

        return uniqueByPath.entries.map { (normalizedPath, uriText) ->
            val path = ensureTrailingSlash(normalizedPath)
            val localPath = ensureMirrorPath(context, uriText)
            GrantedRepoFolder(
                uriText = uriText,
                name = File(normalizedPath).name.ifBlank { "Picked Folder" },
                path = path,
                localPath = localPath,
                isGitRepo = hasGitDirViaSaf(context, uriText)
            )
        }.sortedBy { it.name.lowercase() }
    }

    private fun ensureMirrorPath(context: Context, uriText: String): String {
        val baseDir = File(context.filesDir, "saf_repo_mirrors")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        val folderId = sha256(uriText).take(16)
        val mirrorDir = File(baseDir, folderId)
        if (!mirrorDir.exists()) {
            mirrorDir.mkdirs()
        }
        return mirrorDir.absolutePath
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun hasGitDirViaSaf(context: Context, uriText: String): Boolean {
        return runCatching {
            val tree = DocumentFile.fromTreeUri(context, uriText.toUri()) ?: return@runCatching false
            val git = tree.findFile(".git") ?: return@runCatching false
            git.isDirectory
        }.getOrDefault(false)
    }

    private fun normalizePath(path: String): String {
        return path.trim().trimEnd('/')
    }

    private fun ensureTrailingSlash(path: String): String {
        val trimmed = path.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
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

    private fun resolveRepoContext(dir: File): Pair<Context, String>? {
        val context = appContext ?: return null
        val uriText = mirrorPathToUri[dir.absolutePath] ?: return null
        return context to uriText
    }

    private fun logToTerminal(command: String, result: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] > $command\n$result"
        _terminalOutput.value += line
    }

    fun showStatusInTerminal() {
        runGitOperation(
            command = "git status",
            writeBackToSaf = false,
            action = { dir -> AppGitOps.terminalStatus(dir) }
        )
    }

    fun checkRepoStatus() {
        val dir = currentRepoDir()
        if (dir == null) {
            _isGitRepo.value = false
            _gitStatusText.value = "Select a target vault path"
            return
        }

        val repoContext = resolveRepoContext(dir)
        if (repoContext == null) {
            _isGitRepo.value = false
            _gitStatusText.value = "Selected repo is unavailable. Reopen Path Scans and grant the folder again."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val (context, uriText) = repoContext
            try {
                AppVaultOps.syncSafTreeToLocal(context, uriText, dir)
                val status = AppGitOps.readRepoStatus(dir)
                _isGitRepo.value = status.isGitRepo
                _gitStatusText.value = status.message
            } catch (e: Exception) {
                _isGitRepo.value = false
                _gitStatusText.value = "Path: ${AppVaultOps.readablePathFromUri(uriText.toUri())}\nError: ${e.message}"
            }
        }
    }

    private fun runGitOperation(
        command: String,
        writeBackToSaf: Boolean,
        action: (File) -> String
    ) {
        val dir = currentRepoDir() ?: return
        val repoContext = resolveRepoContext(dir)
        if (repoContext == null) {
            logToTerminal(command, "Error: Selected repo is unavailable. Reopen Path Scans and grant the folder again.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val (context, uriText) = repoContext
            try {
                AppVaultOps.syncSafTreeToLocal(context, uriText, dir)
                val result = action(dir)
                if (writeBackToSaf) {
                    AppVaultOps.syncLocalToSafTree(context, dir, uriText)
                    rebuildGrantedFolders(context)
                } else {
                    checkRepoStatus()
                }
                logToTerminal(command, result)
            } catch (e: Exception) {
                logToTerminal(command, "Error: ${e.message}")
            }
        }
    }

    fun initRepo() {
        runGitOperation(
            command = "git init",
            writeBackToSaf = true,
            action = { dir -> AppGitOps.initRepo(dir) }
        )
    }

    fun stageAll() {
        runGitOperation(
            command = "git add -A",
            writeBackToSaf = true,
            action = { dir -> AppGitOps.stageAll(dir) }
        )
    }

    fun unstageAll() {
        runGitOperation(
            command = "git reset",
            writeBackToSaf = true,
            action = { dir -> AppGitOps.unstageAll(dir) }
        )
    }

    fun commitChanges(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            logToTerminal("git commit", "Commit message cannot be empty")
            return
        }

        runGitOperation(
            command = "git commit -m \"$trimmed\"",
            writeBackToSaf = true,
            action = { dir -> AppGitOps.commit(dir, trimmed) }
        )
    }

    fun pullRepo() {
        logToTerminal("git pull", "Attempting pull. Remote auth may be required")
        runGitOperation(
            command = "git pull",
            writeBackToSaf = true,
            action = { dir -> AppGitOps.pull(dir) }
        )
    }

    fun pushRepo() {
        logToTerminal("git push", "Attempting push. Remote auth may be required")
        runGitOperation(
            command = "git push",
            writeBackToSaf = true,
            action = { dir -> AppGitOps.push(dir) }
        )
    }
}
