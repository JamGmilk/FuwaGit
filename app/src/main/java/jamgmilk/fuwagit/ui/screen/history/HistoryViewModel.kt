package jamgmilk.fuwagit.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.git.CommitStats
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.usecase.GitOperationUseCases
import jamgmilk.fuwagit.domain.usecase.GitQueryUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val repoPath: String? = null,
    val commits: List<GitCommit> = emptyList(),
    val selectedCommit: GitCommit? = null,
    val searchQuery: String = "",
    val commitStats: CommitStats = CommitStats(0, 0, 0, 0, 0),
    val terminalOutput: List<String> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val gitQueryUseCases: GitQueryUseCases,
    private val gitOperationUseCases: GitOperationUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var currentRepoPath: String? = null

    fun setRepoPath(path: String?) {
        currentRepoPath = path
        _uiState.update { it.copy(repoPath = path) }
        if (path != null) {
            loadCommitHistory()
        } else {
            _uiState.update { it.copy(commits = emptyList(), commitStats = CommitStats(0, 0, 0, 0, 0)) }
        }
    }

    fun loadCommitHistory() {
        val path = currentRepoPath
        if (path == null) {
            _uiState.update { it.copy(commits = emptyList()) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            gitQueryUseCases.getCommitHistory(path)
                .onSuccess { commits ->
                    val stats = calculateStats(commits)
                    _uiState.update { 
                        it.copy(
                            commits = commits,
                            commitStats = stats,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { 
                        it.copy(
                            error = e.message,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun selectCommit(commit: GitCommit?) {
        _uiState.update { it.copy(selectedCommit = commit) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun getFilteredCommits(): List<GitCommit> {
        val query = _uiState.value.searchQuery.trim().lowercase()
        if (query.isEmpty()) return _uiState.value.commits
        
        return _uiState.value.commits.filter { commit ->
            commit.message.lowercase().contains(query) ||
            commit.authorName.lowercase().contains(query) ||
            commit.shortHash.lowercase().contains(query)
        }
    }

    private fun calculateStats(commits: List<GitCommit>): CommitStats {
        if (commits.isEmpty()) return CommitStats(0, 0, 0, 0, 0)

        val uniqueAuthors = commits.map { it.authorName }.distinct().size
        
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startOfWeek = calendar.timeInMillis
        
        calendar.timeInMillis = now
        calendar.add(Calendar.MONTH, -1)
        val startOfMonth = calendar.timeInMillis

        val commitsToday = commits.count { it.timestamp >= startOfDay }
        val commitsThisWeek = commits.count { it.timestamp >= startOfWeek }
        val commitsThisMonth = commits.count { it.timestamp >= startOfMonth }

        return CommitStats(
            totalCommits = commits.size,
            uniqueAuthors = uniqueAuthors,
            commitsToday = commitsToday,
            commitsThisWeek = commitsThisWeek,
            commitsThisMonth = commitsThisMonth
        )
    }

    fun revertCommit(commitHash: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                gitOperationUseCases.revertCommit(path, commitHash)
            }.fold(
                onSuccess = { result ->
                    appendTerminalLog("git revert", result)
                    loadCommitHistory()
                },
                onFailure = { e ->
                    appendTerminalLog("git revert", "Error: ${e.message}")
                    _uiState.update { it.copy(error = "Revert failed: ${e.message}") }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun cherryPick(commitHash: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                gitOperationUseCases.cherryPick(path, commitHash)
            }.fold(
                onSuccess = { result ->
                    appendTerminalLog("git cherry-pick", result)
                    loadCommitHistory()
                },
                onFailure = { e ->
                    appendTerminalLog("git cherry-pick", "Error: ${e.message}")
                    _uiState.update { it.copy(error = "Cherry-pick failed: ${e.message}") }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun appendTerminalLog(command: String, message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
        _uiState.update { state ->
            val newOutput = (state.terminalOutput + "[$timestamp] $command: $message").takeLast(100)
            state.copy(terminalOutput = newOutput)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
