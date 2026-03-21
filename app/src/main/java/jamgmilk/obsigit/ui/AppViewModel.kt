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

class AppViewModel : ViewModel() {

    private val _currentPage = MutableStateFlow(AppPage.GitTerminal)
    val currentPage: StateFlow<AppPage> = _currentPage.asStateFlow()

    private val _targetPath = MutableStateFlow<String?>(null)
    val targetPath: StateFlow<String?> = _targetPath.asStateFlow()

    private val _availableTargetPaths = MutableStateFlow<List<String>>(emptyList())
    val availableTargetPaths: StateFlow<List<String>> = _availableTargetPaths.asStateFlow()

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

    private val obsidianSandboxPath = "/storage/emulated/0/Android/data/md.obsidian"

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
        refreshVaultItems(context)
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
        val sandboxPath = "/storage/emulated/0/Android/data/${context.packageName}"
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
