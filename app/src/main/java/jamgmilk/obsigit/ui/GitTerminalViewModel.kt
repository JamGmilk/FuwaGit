package jamgmilk.obsigit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GitTerminalViewModel : ViewModel() {

    private val targetPath = "/storage/emulated/0/Documents/GitTest/"
    private val repoDir = File(targetPath)

    private val _isGitRepo = MutableStateFlow(false)
    val isGitRepo: StateFlow<Boolean> = _isGitRepo.asStateFlow()

    private val _gitStatusText = MutableStateFlow("Checking status...")
    val gitStatusText: StateFlow<String> = _gitStatusText.asStateFlow()

    private val _terminalOutput = MutableStateFlow(listOf<String>())
    val terminalOutput: StateFlow<List<String>> = _terminalOutput.asStateFlow()

    init {
        checkRepoStatus()
    }

    private fun logToTerminal(command: String, result: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = "[$time] > $command\n$result"
        _terminalOutput.value += newLog
    }

    fun showStatusInTerminal() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Git.open(repoDir).use { git ->
                    val status = git.status().call()
                    val result = "Branch: ${git.repository.branch}\n" +
                            "Added: ${status.added.size}\n" +
                            "Modified: ${status.modified.size}\n" +
                            "Untracked: ${status.untracked.size}"

                    // ONLY log to terminal when this specific function is called
                    logToTerminal("git status", result)
                }
            } catch (e: Exception) {
                logToTerminal("git status", "Error: ${e.message}")
            }
        }
    }

    fun checkRepoStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!repoDir.exists()) repoDir.mkdirs()

                Git.open(repoDir).use { git ->
                    _isGitRepo.value = true
                    val status = git.status().call()
                    val statusMsg = "Branch: ${git.repository.branch}\n" +
                            "Uncommitted: ${status.hasUncommittedChanges()}\n" +
                            "Untracked: ${status.untracked.size} files"

                    _gitStatusText.value = statusMsg
                }
            } catch (e: RepositoryNotFoundException) {
                _isGitRepo.value = false
                _gitStatusText.value = "Not a Git repository."
            } catch (e: Exception) {
                _gitStatusText.value = "Error: ${e.message}"
            }
        }
    }

    fun initRepo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!repoDir.exists()) repoDir.mkdirs()
                Git.init().setDirectory(repoDir).call().use {
                    logToTerminal("git init", "Initialized empty Git repository in $targetPath")
                    checkRepoStatus()
                }
            } catch (e: Exception) {
                logToTerminal("git init", "Error: ${e.message}")
            }
        }
    }

    fun stageAll() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Git.open(repoDir).use { git ->
                    git.add().addFilepattern(".").call()
                    logToTerminal("git add -A", "All files staged.")
                    checkRepoStatus()
                }
            } catch (e: Exception) {
                logToTerminal("git add -A", "Error: ${e.message}")
            }
        }
    }

    fun unstageAll() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Git.open(repoDir).use { git ->
                    git.reset().call() // Default is mixed reset, which unstages
                    logToTerminal("git reset", "Unstaged all changes.")
                    checkRepoStatus()
                }
            } catch (e: Exception) {
                logToTerminal("git reset", "Error: ${e.message}")
            }
        }
    }

    fun commitChanges(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Git.open(repoDir).use { git ->
                    git.commit().setMessage(message).call()
                    logToTerminal("git commit -m \"$message\"", "Commit successful.")
                    checkRepoStatus()
                }
            } catch (e: Exception) {
                logToTerminal("git commit", "Error: ${e.message}")
            }
        }
    }

    fun pullRepo() {
        viewModelScope.launch(Dispatchers.IO) {
            logToTerminal("git pull", "Attempting to pull... (Note: Auth needed for remote)")
            try {
                Git.open(repoDir).use { git ->
                    val result = git.pull().call()
                    logToTerminal("git pull result", result.isSuccessful.toString())
                    checkRepoStatus()
                }
            } catch (e: Exception) {
                logToTerminal("git pull", "Error: ${e.message}")
            }
        }
    }

    fun pushRepo() {
        viewModelScope.launch(Dispatchers.IO) {
            logToTerminal("git push", "Attempting to push... (Note: Auth needed for remote)")
            try {
                Git.open(repoDir).use { git ->
                    git.push().call()
                    logToTerminal("git push result", "Push command executed.")
                }
            } catch (e: Exception) {
                logToTerminal("git push", "Error: ${e.message}")
            }
        }
    }
}