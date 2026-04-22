package jamgmilk.fuwagit.ui.screen.history

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.ui.components.ResetConfirmDialog
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.theme.AppShapes
import kotlinx.coroutines.flow.collectLatest

data class DiffViewRequest(
    val filePath: String,
    val oldCommitHash: String,
    val newCommitHash: String
)

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    modifier: Modifier = Modifier,
    onViewCommitDiff: ((DiffViewRequest) -> Unit)? = null
) {
    val uiState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val pagination = uiState.pagination
    val history = uiState.commits
    val snackbarHostState = remember { SnackbarHostState() }
    val resetSuccessMsg = stringResource(R.string.history_reset_completed)

    LaunchedEffect(Unit) {
        historyViewModel.events.collectLatest { event ->
            when (event) {
                is HistoryEvent.ResetSuccess -> {
                    snackbarHostState.showSnackbar(resetSuccessMsg)
                }
                is HistoryEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            if (history.isNotEmpty()) {
                snackbarHostState.showSnackbar(it)
                historyViewModel.clearError()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        ScreenTemplate(
            title = stringResource(R.string.screen_history),
            modifier = modifier.fillMaxSize(),
            actions = {
                Text(
                    text = stringResource(R.string.history_commits_count_format, history.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShapes.medium),
                shape = AppShapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (history.isEmpty()) {
                        when {
                            pagination.isLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            uiState.error != null -> {
                                HistoryErrorState(
                                    error = uiState.error!!,
                                    onRetry = { historyViewModel.loadCommitHistory() }
                                )
                            }
                            else -> {
                                EmptyHistoryState()
                            }
                        }
                    } else {
                        CommitTimelineList(
                            commits = history,
                            details = uiState.details,
                            pagination = pagination,
                            viewModel = historyViewModel,
                            modifier = Modifier.fillMaxSize(),
                            onViewCommitDiff = onViewCommitDiff
                        )

                        if (pagination.isLoading) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val resetDialog = uiState.resetDialog
    if (resetDialog.pendingCommit != null && resetDialog.pendingMode != null) {
        ResetConfirmDialog(
            commit = resetDialog.pendingCommit,
            mode = resetDialog.pendingMode,
            onConfirm = { historyViewModel.confirmReset() },
            onDismiss = { historyViewModel.cancelReset() }
        )
    }
}

@Composable
private fun CommitTimelineList(
    commits: List<GitCommit>,
    details: CommitDetailsState,
    pagination: PaginationState,
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier,
    onViewCommitDiff: ((DiffViewRequest) -> Unit)? = null
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - 5 && pagination.hasMore && !pagination.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    LazyColumn(
        modifier = modifier.padding(vertical = 8.dp),
        state = listState,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(
            items = commits,
            key = { _, commit -> commit.hash },
            contentType = { _, _ -> "commit_timeline_item" }
        ) { index, commit ->
            CommitTimelineItem(
                commit = commit,
                details = details,
                isLast = index == commits.lastIndex,
                viewModel = viewModel,
                onViewDiff = onViewCommitDiff
            )
        }
        if (pagination.isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
