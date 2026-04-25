package jamgmilk.fuwagit.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import jamgmilk.fuwagit.domain.model.git.GitResetMode
import jamgmilk.fuwagit.ui.state.RepoStateManager
import jamgmilk.fuwagit.domain.usecase.git.GetCommitFileChangesUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetCommitHistoryUseCase
import jamgmilk.fuwagit.domain.usecase.git.ResetUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

import androidx.compose.runtime.Stable

private class LruCache<K, V>(private val maxSize: Int) {
    private val delegate = LinkedHashMap<K, V>(maxSize, 0.75f, true)

    @Synchronized
    fun put(key: K, value: V): Map<K, V> {
        delegate[key] = value
        while (delegate.size > maxSize) {
            val eldest = delegate.keys.first()
            delegate.remove(eldest)
        }
        return delegate.toMap()
    }

    @Synchronized
    fun remove(key: K): Map<K, V> {
        delegate.remove(key)
        return delegate.toMap()
    }

    @Synchronized
    fun clear(): Map<K, V> {
        delegate.clear()
        return emptyMap()
    }
}

sealed class HistoryEvent {
    data object ResetSuccess : HistoryEvent()
    data class Error(val message: String) : HistoryEvent()
}

@Stable
data class PaginationState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true
)

@Stable
data class ResetDialogState(
    val pendingCommit: GitCommit? = null,
    val pendingMode: GitResetMode? = null,
    val isResetting: Boolean = false
)

@Stable
data class CommitDetailsState(
    val commitDetails: Map<String, GitCommitDetail> = emptyMap(),
    val loadingCommitDetails: Set<String> = emptySet(),
    val expandedCommitHashes: Set<String> = emptySet(),
    val commitLanes: Map<String, Int> = emptyMap(),
    val detailLoadErrors: Set<String> = emptySet()
)

@Stable
data class HistoryUiState(
    val repoPath: String? = null,
    val commits: List<GitCommit> = emptyList(),
    val pagination: PaginationState = PaginationState(),
    val resetDialog: ResetDialogState = ResetDialogState(),
    val details: CommitDetailsState = CommitDetailsState(),
    val error: String? = null,
    val filterBranch: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val currentRepoManager: RepoStateManager,
    private val getCommitHistoryUseCase: GetCommitHistoryUseCase,
    private val resetUseCase: ResetUseCase,
    private val getCommitFileChangesUseCase: GetCommitFileChangesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HistoryEvent>()
    val events: SharedFlow<HistoryEvent> = _events.asSharedFlow()

    private var loadHistoryJob: Job? = null
    private var loadMoreJob: Job? = null
    private val detailJobs = ConcurrentHashMap<String, Job>()
    private val detailLruCache = LruCache<String, GitCommitDetail>(MAX_CACHED_DETAILS)

    companion object {
        private const val PAGE_SIZE = 50
        private const val MAX_CACHED_DETAILS = 20
    }

    init {
        viewModelScope.launch {
            currentRepoManager.repoInfo.collectLatest { info ->
                _uiState.update { it.copy(repoPath = info.repoPath) }
                if (info.isValidGit) {
                    loadCommitHistory()
                } else {
                    cancelInFlightLoads()
                    _uiState.update {
                        it.copy(
                            commits = emptyList(),
                            pagination = PaginationState(),
                            details = CommitDetailsState()
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            currentRepoManager.refreshEvents.collectLatest {
                loadCommitHistory()
            }
        }
    }

    fun loadCommitHistory() {
        val path = _uiState.value.repoPath
        if (path == null) {
            cancelInFlightLoads()
            _uiState.update {
                it.copy(
                    commits = emptyList(),
                    pagination = PaginationState(),
                    details = CommitDetailsState()
                )
            }
            return
        }

        cancelInFlightLoads()
        val job = viewModelScope.launch {
            _uiState.update { it.copy(pagination = it.pagination.copy(isLoading = true, isLoadingMore = false)) }
            try {
                getCommitHistoryUseCase(path, maxCount = PAGE_SIZE, skip = 0)
                    .onSuccess { commits ->
                        coroutineContext.ensureActive()
                        val lanes = computeLanes(commits)
                        _uiState.update {
                            it.copy(
                                commits = commits,
                                error = null,
                                pagination = it.pagination.copy(
                                    isLoading = false,
                                    hasMore = commits.size >= PAGE_SIZE
                                ),
                                details = CommitDetailsState(commitLanes = lanes)
                            )
                        }
                    }
                    .onError { e ->
                        coroutineContext.ensureActive()
                        _uiState.update {
                            it.copy(
                                error = e.message,
                                pagination = it.pagination.copy(isLoading = false)
                            )
                        }
                    }
            } finally {
                if (loadHistoryJob === coroutineContext[Job]) {
                    loadHistoryJob = null
                }
            }
        }
        loadHistoryJob = job
    }

    fun filterByBranch(branchName: String?) {
        _uiState.update { it.copy(filterBranch = branchName) }
        loadCommitHistory()
    }

    private fun computeLanes(commits: List<GitCommit>): Map<String, Int> {
        val lanes = mutableMapOf<String, Int>()
        val childToLane = mutableMapOf<String, Int>()
        var nextLane = 0

        for (commit in commits) {
            if (commit.hash in lanes) continue

            val lane = childToLane[commit.hash] ?: (nextLane++ % 10)
            lanes[commit.hash] = lane
            if (commit.parentHashes.isNotEmpty()) {
                if (!childToLane.containsKey(commit.parentHashes[0])) {
                    childToLane[commit.parentHashes[0]] = lane
                }
            }
        }
        return lanes
    }

    fun loadMore() {
        val path = _uiState.value.repoPath ?: return
        val state = _uiState.value
        if (loadMoreJob?.isActive == true || state.pagination.isLoadingMore || !state.pagination.hasMore) return

        loadMoreJob?.cancel()
        val job = viewModelScope.launch {
            val skip = state.commits.size
            _uiState.update { it.copy(pagination = it.pagination.copy(isLoadingMore = true)) }
            try {
                getCommitHistoryUseCase(path, maxCount = PAGE_SIZE, skip = skip)
                    .onSuccess { newCommits ->
                        coroutineContext.ensureActive()
                        val updatedCommits = state.commits + newCommits
                        val lanes = computeLanes(updatedCommits)
                        _uiState.update {
                            it.copy(
                                commits = updatedCommits,
                                pagination = it.pagination.copy(
                                    isLoadingMore = false,
                                    hasMore = newCommits.size >= PAGE_SIZE
                                ),
                                details = it.details.copy(commitLanes = lanes)
                            )
                        }
                    }
                    .onError { e ->
                        coroutineContext.ensureActive()
                        _uiState.update {
                            it.copy(
                                error = e.message,
                                pagination = it.pagination.copy(isLoadingMore = false)
                            )
                        }
                    }
            } finally {
                if (loadMoreJob === coroutineContext[Job]) {
                    loadMoreJob = null
                }
            }
        }
        loadMoreJob = job
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun requestReset(commit: GitCommit, mode: GitResetMode) {
        _uiState.update {
            it.copy(resetDialog = it.resetDialog.copy(pendingCommit = commit, pendingMode = mode))
        }
    }

    fun confirmReset() {
        val path = _uiState.value.repoPath ?: return
        val state = _uiState.value.resetDialog
        val commit = state.pendingCommit ?: return
        val mode = state.pendingMode ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(resetDialog = it.resetDialog.copy(isResetting = true)) }
            resetUseCase(path, commit.hash, mode)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            resetDialog = ResetDialogState(),
                            error = null
                        )
                    }
                    _events.emit(HistoryEvent.ResetSuccess)
                    loadCommitHistory()
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(resetDialog = ResetDialogState())
                    }
                    _events.emit(HistoryEvent.Error(e.message ?: "Unknown error"))
                }
        }
    }

    fun cancelReset() {
        _uiState.update {
            it.copy(resetDialog = ResetDialogState())
        }
    }

    fun loadCommitDetailIfNeeded(commit: GitCommit) {
        val path = _uiState.value.repoPath ?: return
        val details = _uiState.value.details
        if (details.commitDetails.containsKey(commit.hash)) return
        if (detailJobs[commit.hash]?.isActive == true || commit.hash in details.loadingCommitDetails) return

        detailJobs[commit.hash]?.cancel()
        val job = viewModelScope.launch {
            _uiState.update {
                it.copy(details = it.details.copy(
                    loadingCommitDetails = it.details.loadingCommitDetails + commit.hash,
                    detailLoadErrors = it.details.detailLoadErrors - commit.hash
                ))
            }
            try {
                getCommitFileChangesUseCase(path, commit.hash)
                    .onSuccess { detail ->
                        coroutineContext.ensureActive()
                        _uiState.update {
                            it.copy(details = it.details.copy(
                                commitDetails = detailLruCache.put(commit.hash, detail),
                                loadingCommitDetails = it.details.loadingCommitDetails - commit.hash
                            ))
                        }
                    }
                    .onError { e ->
                        coroutineContext.ensureActive()
                        _uiState.update {
                            it.copy(details = it.details.copy(
                                loadingCommitDetails = it.details.loadingCommitDetails - commit.hash,
                                detailLoadErrors = it.details.detailLoadErrors + commit.hash
                            ))
                        }
                    }
            } finally {
                _uiState.update {
                    it.copy(details = it.details.copy(
                        loadingCommitDetails = it.details.loadingCommitDetails - commit.hash
                    ))
                }
                if (detailJobs[commit.hash] === coroutineContext[Job]) {
                    detailJobs.remove(commit.hash)
                }
            }
        }
        detailJobs[commit.hash] = job
    }

    fun retryCommitDetail(commit: GitCommit) {
        _uiState.update {
            it.copy(details = it.details.copy(detailLoadErrors = it.details.detailLoadErrors - commit.hash))
        }
        loadCommitDetailIfNeeded(commit)
    }

    fun clearCommitDetail(commitHash: String) {
        detailJobs.remove(commitHash)?.cancel()
        _uiState.update {
            it.copy(details = it.details.copy(
                commitDetails = detailLruCache.remove(commitHash),
                loadingCommitDetails = it.details.loadingCommitDetails - commitHash
            ))
        }
    }

    fun toggleCommitExpanded(commitHash: String) {
        _uiState.update {
            val isExpanding = commitHash !in it.details.expandedCommitHashes
            val nextExpanded = if (isExpanding) {
                it.details.expandedCommitHashes + commitHash
            } else {
                detailJobs.remove(commitHash)?.cancel()
                it.details.expandedCommitHashes - commitHash
            }

            val nextDetails = if (isExpanding) {
                it.details.commitDetails
            } else {
                detailLruCache.remove(commitHash)
            }

            it.copy(details = it.details.copy(
                expandedCommitHashes = nextExpanded,
                commitDetails = nextDetails
            ))
        }
    }

    private fun cancelInFlightLoads() {
        loadHistoryJob?.cancel()
        loadHistoryJob = null
        loadMoreJob?.cancel()
        loadMoreJob = null
        detailJobs.values.forEach { it.cancel() }
        detailJobs.clear()
        detailLruCache.clear()
    }
}
