package jamgmilk.obsigit.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import jamgmilk.obsigit.ui.navigation.Screen
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
import jamgmilk.obsigit.domain.model.GitBranch
import jamgmilk.obsigit.domain.model.GitCommit
import jamgmilk.obsigit.domain.model.GitFileStatus
import jamgmilk.obsigit.data.source.JGitDataSource
import jamgmilk.obsigit.data.source.RepoPathUtils

private const val TAG = "ObsiGit"

enum class AppPage {
    Status,
    History,
    Branches,
    Repo,
    Settings
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
    private val _currentPage = MutableStateFlow(AppPage.Status)
    val currentPage: StateFlow<AppPage> = _currentPage.asStateFlow()

    private val _currentScreen: MutableStateFlow<Screen> = MutableStateFlow(Screen.Status)
    val currentScreenFlow: StateFlow<Screen> = _currentScreen.asStateFlow()

    var currentScreen: Screen
        get() = _currentScreen.value
        set(value) { _currentScreen.value = value }

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
            val readablePath = RepoPathUtils.readablePathFromUri(uriText.toUri())
            val normalizedPath = RepoPathUtils.normalizeLocalPath(readablePath)
            if (normalizedPath.startsWith("/") && (normalizedPath !in uniqueByPath)) {
                uniqueByPath[normalizedPath] = uriText
            }
        }

        return uniqueByPath.entries.asSequence().map { (normalizedPath, uriText) ->
            val path = RepoPathUtils.ensureTrailingSlash(normalizedPath)
            val localPath = normalizedPath
            val dir = File(localPath)
            RepoFolderItem(
                id = "grant:$uriText",
                uriText = uriText,
                name = File(normalizedPath).name.ifBlank { "Picked Folder" },
                path = path,
                localPath = localPath,
                isGitRepo = JGitDataSource.hasGitDir(localPath),
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
                val result = JGitDataSource.withGitLock { JGitDataSource.terminalStatus(dir) }
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
                val status = JGitDataSource.withGitLock { JGitDataSource.readRepoStatus(dir) }
                _isGitRepo.value = status.isGitRepo
                _gitStatusText.value = status.message
            } catch (e: Exception) {
                _isGitRepo.value = false
                _gitStatusText.value = "Path: ${RepoPathUtils.shortDisplayPath(dir)}\nError: ${e.message}"
            }
        }
    }

    fun initRepo() {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = JGitDataSource.withGitLock { JGitDataSource.initRepo(dir) }
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
                val result = JGitDataSource.withGitLock {
                    val res = JGitDataSource.stageAll(dir)
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                    res
                }
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
                val result = JGitDataSource.withGitLock {
                    val res = JGitDataSource.unstageAll(dir)
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                    res
                }
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
                val result = JGitDataSource.withGitLock {
                    val res = JGitDataSource.commit(dir, trimmed)
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                    _commitHistory.value = JGitDataSource.getLog(dir)
                    res
                }
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
                val result = JGitDataSource.withGitLock {
                    val res = JGitDataSource.pull(dir)
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                    _commitHistory.value = JGitDataSource.getLog(dir)
                    res
                }
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
                val result = JGitDataSource.withGitLock { JGitDataSource.push(dir) }
                appendTerminalLog("git push", result)
            } catch (e: Exception) {
                appendTerminalLog("git push", "Error: ${e.message}")
            }
        }
    }

    fun refreshWorkspace() {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                JGitDataSource.withGitLock {
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                    _commitHistory.value = JGitDataSource.getLog(dir)
                    _branches.value = JGitDataSource.getBranches(dir)
                }
            } catch (e: Exception) {
                appendTerminalLog("refresh", "Error during workspace refresh: ${e.message}")
            }
        }
    }

    fun stageFile(path: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                JGitDataSource.withGitLock {
                    JGitDataSource.stageFile(dir, path)
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                }
            } catch (e: Exception) {
                appendTerminalLog("git add $path", "Error: ${e.message}")
            }
        }
    }

    fun unstageFile(path: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                JGitDataSource.withGitLock {
                    JGitDataSource.unstageFile(dir, path)
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                }
            } catch (e: Exception) {
                appendTerminalLog("git reset $path", "Error: ${e.message}")
            }
        }
    }

    fun discardChanges(path: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                JGitDataSource.withGitLock {
                    JGitDataSource.discardChanges(dir, path)
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                }
            } catch (e: Exception) {
                appendTerminalLog("git checkout -- $path", "Error: ${e.message}")
            }
        }
    }

    fun checkoutBranch(name: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                JGitDataSource.withGitLock {
                    JGitDataSource.checkoutBranch(dir, name)
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                    _branches.value = JGitDataSource.getBranches(dir)
                }
                checkRepoStatus()
            } catch (e: Exception) {
                appendTerminalLog("git checkout $name", "Error: ${e.message}")
            }
        }
    }
    
    fun createBranch(name: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                JGitDataSource.withGitLock {
                    JGitDataSource.createBranch(dir, name)
                    _branches.value = JGitDataSource.getBranches(dir)
                }
                appendTerminalLog("git branch $name", "Branch created successfully")
            } catch (e: Exception) {
                appendTerminalLog("git branch $name", "Error: ${e.message}")
            }
        }
    }
    
    fun mergeBranch(name: String) {
        val dir = currentRepoDirForGit() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                JGitDataSource.withGitLock {
                    JGitDataSource.mergeBranch(dir, name)
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                    _commitHistory.value = JGitDataSource.getLog(dir)
                }
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
                JGitDataSource.withGitLock {
                    JGitDataSource.rebaseBranch(dir, name)
                    _workspaceFiles.value = JGitDataSource.getDetailedStatus(dir)
                    _commitHistory.value = JGitDataSource.getLog(dir)
                }
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
                JGitDataSource.withGitLock {
                    JGitDataSource.deleteBranch(dir, name, force)
                    _branches.value = JGitDataSource.getBranches(dir)
                }
            } catch (e: Exception) {
                appendTerminalLog("git branch -d $name", "Error: ${e.message}")
            }
        }
    }

    private fun refreshRepoFlagsFromDisk() {
        _repoItems.value = _repoItems.value.map { item ->
            val localPath = item.localPath
            if (localPath != null) {
                item.copy(isGitRepo = JGitDataSource.hasGitDir(localPath))
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
