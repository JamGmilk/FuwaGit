package jamgmilk.obsigit.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
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
import androidx.core.content.edit

enum class AppPage {
    GitTerminal,
    Repo,
    Settings,
    Workspace
}

enum class WorkspaceTab {
    Status, History, Branches
}

sealed class RootStatus {
    data object Idle : RootStatus()
    data object Checking : RootStatus()
    data object Granted : RootStatus()
    data object Denied : RootStatus()
}

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

class AppViewModel : ViewModel() {
    private val _currentPage = MutableStateFlow(AppPage.GitTerminal)
    val currentPage: StateFlow<AppPage> = _currentPage.asStateFlow()

    private val _targetPath = MutableStateFlow<String?>(null)
    val targetPath: StateFlow<String?> = _targetPath.asStateFlow()

    private val _availableTargetPaths = MutableStateFlow<List<String>>(emptyList())

    private val _isGitRepo = MutableStateFlow(value = false)
    val isGitRepo: StateFlow<Boolean> = _isGitRepo.asStateFlow()

    private val _gitStatusText = MutableStateFlow("Select a target repo")
    val gitStatusText: StateFlow<String> = _gitStatusText.asStateFlow()

    private val _terminalOutput = MutableStateFlow<List<String>>(emptyList())
    val terminalOutput: StateFlow<List<String>> = _terminalOutput.asStateFlow()

    private val _rootStatus = MutableStateFlow<RootStatus>(RootStatus.Idle)
    val rootStatus: StateFlow<RootStatus> = _rootStatus.asStateFlow()

    private val _repoItems = MutableStateFlow<List<RepoFolderItem>>(emptyList())
    val repoItems: StateFlow<List<RepoFolderItem>> = _repoItems.asStateFlow()

    private val _grantedTreeUris = MutableStateFlow<List<String>>(emptyList())
    val grantedTreeUris: StateFlow<List<String>> = _grantedTreeUris.asStateFlow()

    private val _workspaceFiles = MutableStateFlow<List<GitFileStatus>>(emptyList())
    val workspaceFiles: StateFlow<List<GitFileStatus>> = _workspaceFiles.asStateFlow()

    private val _commitHistory = MutableStateFlow<List<GitCommit>>(emptyList())
    val commitHistory: StateFlow<List<GitCommit>> = _commitHistory.asStateFlow()

    private val _branches = MutableStateFlow<List<GitBranch>>(emptyList())
    val branches: StateFlow<List<GitBranch>> = _branches.asStateFlow()

    private val _workspaceTab = MutableStateFlow(WorkspaceTab.Status)
    val workspaceTab: StateFlow<WorkspaceTab> = _workspaceTab.asStateFlow()

    private var storageInitialized = false

    fun switchPage(page: AppPage) {
        _currentPage.value = page
    }

    fun setTargetPath(context: Context, path: String?) {
        _targetPath.value = path
        saveTargetPath(context, path)
        checkRepoStatus()
        if (path != null) {
            refreshWorkspace()
        }
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
        if (path != null) {
            refreshWorkspace()
        }
    }

    fun refreshPersistedUris(context: Context) {
        _grantedTreeUris.value = context.contentResolver.persistedUriPermissions.asSequence()
            .filter { it.isReadPermission || it.isWritePermission }
            .map { it.uri.toString() }
            .distinct()
            .toList()
    }

    fun refreshRepoItems(context: Context) {
        rebuildGrantedFolders(context)
    }

    private fun rebuildGrantedFolders(context: Context) {
        refreshPersistedUris(context)
        val grantedUrisSnapshot = _grantedTreeUris.value

        viewModelScope.launch(Dispatchers.IO) {
            val repoFolders = buildGrantedRepoFolders(grantedUrisSnapshot)
            _repoItems.value = repoFolders

            val targetCandidates = repoFolders
                .mapNotNull { it.localPath }
                .filter { File(it).exists() && File(it).isDirectory }
                .distinct()
                .sorted()
            _availableTargetPaths.value = targetCandidates

            val selectedPath = _targetPath.value
            if (selectedPath != null && (selectedPath !in targetCandidates)) {
                _targetPath.value = null
                saveTargetPath(context, null)
            }

            withContext(Dispatchers.Main) {
                checkRepoStatus()
            }
        }
    }

    private fun buildGrantedRepoFolders(grantedUris: List<String>): List<RepoFolderItem> {
        val uniqueByPath = linkedMapOf<String, String>()

        grantedUris.forEach { uriText ->
            val readablePath = AppRepoOps.readablePathFromUri(uriText.toUri())
            val normalizedPath = AppRepoOps.normalizeLocalPath(readablePath)
            if (normalizedPath.startsWith("/") && (normalizedPath !in uniqueByPath)) {
                uniqueByPath[normalizedPath] = uriText
            }
        }

        return uniqueByPath.entries.asSequence().map { (normalizedPath, uriText) ->
            val path = AppRepoOps.ensureTrailingSlash(normalizedPath)
            val localPath = normalizedPath
            val dir = File(localPath)
            RepoFolderItem(
                id = "grant:$uriText",
                uriText = uriText,
                name = File(normalizedPath).name.ifBlank { "Picked Folder" },
                path = path,
                localPath = localPath,
                isGitRepo = AppGitOps.hasGitDir(localPath),
                isActive = dir.exists() && dir.isDirectory,
                lastModified = if (dir.exists()) dir.lastModified() else 0L,
            )
        }.sortedBy { it.name.lowercase() }.toList()
    }

    fun checkRoot() {
        viewModelScope.launch {
            _rootStatus.value = RootStatus.Checking
            val isRooted = withContext(Dispatchers.IO) { Shell.getShell().isRoot }
            _rootStatus.value = if (isRooted) RootStatus.Granted else RootStatus.Denied
        }
    }

    private fun currentRepoDirForGit(): File? {
        val path = _targetPath.value ?: return null
        return File(path)
    }

    private fun appendTerminalLog(command: String, result: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$time] > $command\n$result"
        _terminalOutput.value += line
    }

    fun showStatusInTerminal() {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AppGitOps.terminalStatus(dir)
                appendTerminalLog("git status", result)
            } catch (e: Exception) {
                appendTerminalLog("git status", "Error: ${e.message}")
            }
        }
    }

    fun checkRepoStatus() {
        val dir = currentRepoDirForGit()
        if (dir == null) {
            _isGitRepo.value = false
            _gitStatusText.value = "Select a target repo path"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = AppGitOps.readRepoStatus(dir)
                _isGitRepo.value = status.isGitRepo
                _gitStatusText.value = status.message
            } catch (e: Exception) {
                _isGitRepo.value = false
                _gitStatusText.value = "Path: ${AppRepoOps.shortDisplayPath(dir)}\nError: ${e.message}"
            }
        }
    }

    fun initRepo() {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AppGitOps.initRepo(dir)
                appendTerminalLog("git init", result)
                checkRepoStatus()
                refreshRepoFlagsFromDisk()
            } catch (e: Exception) {
                appendTerminalLog("git init", "Error: ${e.message}")
            }
        }
    }

    fun stageAll() {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AppGitOps.stageAll(dir)
                appendTerminalLog("git add -A", result)
                checkRepoStatus()
            } catch (e: Exception) {
                appendTerminalLog("git add -A", "Error: ${e.message}")
            }
        }
    }

    fun unstageAll() {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AppGitOps.unstageAll(dir)
                appendTerminalLog("git reset", result)
                checkRepoStatus()
            } catch (e: Exception) {
                appendTerminalLog("git reset", "Error: ${e.message}")
            }
        }
    }

    fun commitChanges(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            appendTerminalLog("git commit", "Commit message cannot be empty")
            return
        }

        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = AppGitOps.commit(dir, trimmed)
                appendTerminalLog("git commit -m \"$trimmed\"", result)
                checkRepoStatus()
            } catch (e: Exception) {
                appendTerminalLog("git commit", "Error: ${e.message}")
            }
        }
    }

    fun pullRepo() {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            appendTerminalLog("git pull", "Attempting pull. Remote auth may be required")
            try {
                val result = AppGitOps.pull(dir)
                appendTerminalLog("git pull", result)
                checkRepoStatus()
            } catch (e: Exception) {
                appendTerminalLog("git pull", "Error: ${e.message}")
            }
        }
    }

    fun pushRepo() {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            appendTerminalLog("git push", "Attempting push. Remote auth may be required")
            try {
                val result = AppGitOps.push(dir)
                appendTerminalLog("git push", result)
            } catch (e: Exception) {
                appendTerminalLog("git push", "Error: ${e.message}")
            }
        }
    }

    fun refreshWorkspace() {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _workspaceFiles.value = AppGitOps.getDetailedStatus(dir)
            _commitHistory.value = AppGitOps.getLog(dir)
            _branches.value = AppGitOps.getBranches(dir)
        }
    }

    fun setWorkspaceTab(tab: WorkspaceTab) {
        _workspaceTab.value = tab
    }

    fun stageFile(path: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            AppGitOps.stageFile(dir, path)
            refreshWorkspace()
        }
    }

    fun unstageFile(path: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            AppGitOps.unstageFile(dir, path)
            refreshWorkspace()
        }
    }

    fun discardChanges(path: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            AppGitOps.discardChanges(dir, path)
            refreshWorkspace()
        }
    }

    fun checkoutBranch(name: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppGitOps.checkoutBranch(dir, name)
                refreshWorkspace()
                checkRepoStatus()
            } catch (e: Exception) {
                appendTerminalLog("git checkout $name", "Error: ${e.message}")
            }
        }
    }
    
    fun mergeBranch(name: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppGitOps.mergeBranch(dir, name)
                refreshWorkspace()
                checkRepoStatus()
            } catch (e: Exception) {
                appendTerminalLog("git merge $name", "Error: ${e.message}")
            }
        }
    }

    fun rebaseBranch(name: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppGitOps.rebaseBranch(dir, name)
                refreshWorkspace()
                checkRepoStatus()
            } catch (e: Exception) {
                appendTerminalLog("git rebase $name", "Error: ${e.message}")
            }
        }
    }

    fun deleteBranch(name: String, force: Boolean = false) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppGitOps.deleteBranch(dir, name, force)
                refreshWorkspace()
            } catch (e: Exception) {
                appendTerminalLog("git branch -d $name", "Error: ${e.message}")
            }
        }
    }

    private fun refreshRepoFlagsFromDisk() {
        _repoItems.value = _repoItems.value.map { item ->
            val localPath = item.localPath
            if (localPath != null) {
                item.copy(isGitRepo = AppGitOps.hasGitDir(localPath))
            } else {
                item
            }
        }
    }

    private fun saveTargetPath(context: Context, path: String?) {
        val prefs = context.getSharedPreferences("obsigit_prefs", Context.MODE_PRIVATE)
        prefs.edit { putString("target_path", path) }
    }

    private fun loadTargetPath(context: Context): String? {
        val prefs = context.getSharedPreferences("obsigit_prefs", Context.MODE_PRIVATE)
        return prefs.getString("target_path", null)
    }
}
