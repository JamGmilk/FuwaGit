package jamgmilk.fuwagit.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.di.AppContainer
import jamgmilk.fuwagit.ui.AppViewModel
import jamgmilk.fuwagit.ui.screen.branches.BranchesScreen
import jamgmilk.fuwagit.ui.screen.history.HistoryScreen
import jamgmilk.fuwagit.ui.screen.repo.RepoScreen
import jamgmilk.fuwagit.ui.screen.settings.SettingsScreen
import jamgmilk.fuwagit.ui.screen.status.StatusScreen
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val index: Int) {
    data object Status : Screen("status", 0)
    data object History : Screen("history", 1)
    data object Branches : Screen("branches", 2)
    data object Repo : Screen("repo", 3)
    data object Settings : Screen("settings", 4)

    companion object {
        val screens: List<Screen> by lazy { listOf(Status, History, Branches, Repo, Settings) }
        fun fromIndex(index: Int): Screen = screens.getOrNull(index) ?: Status
    }
}

@OptIn(FlowPreview::class)
@Composable
fun FuwaGitNavHost(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val screens = Screen.screens
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    val pagerState = rememberPagerState(
        initialPage = currentScreen.index,
        pageCount = { screens.size }
    )

    val statusViewModel = remember { AppContainer.createStatusViewModel() }
    val historyViewModel = remember { AppContainer.createHistoryViewModel() }
    val branchesViewModel = remember { AppContainer.createBranchesViewModel() }
    val targetPath by viewModel.targetPath.collectAsState()
    val currentScreenState by viewModel.currentScreenFlow.collectAsState()
    val swipeEnabled by viewModel.swipeEnabledFlow.collectAsState()
    
    LaunchedEffect(targetPath) {
        statusViewModel.setRepoPath(targetPath)
        historyViewModel.setRepoPath(targetPath)
        branchesViewModel.setRepoPath(targetPath)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                if (pageIndex in screens.indices) {
                    val targetScreen = Screen.fromIndex(pageIndex)
                    if (targetScreen != currentScreenState) {
                        focusManager.clearFocus()
                        onScreenChange(targetScreen)
                    }
                }
            }
    }

    LaunchedEffect(currentScreenState) {
        val targetIndex = currentScreenState.index
        if (pagerState.settledPage != targetIndex) {
            focusManager.clearFocus()
            scope.launch {
                pagerState.animateScrollToPage(
                    page = targetIndex,
                    animationSpec = tween(250)
                )
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondViewportPageCount = 1,
        pageSpacing = 0.dp,
        userScrollEnabled = swipeEnabled
    ) { pageIndex ->
        val screen = Screen.fromIndex(pageIndex)
        
        when (screen) {
            Screen.Status -> StatusScreen(
                statusViewModel = statusViewModel,
                modifier = Modifier.fillMaxSize()
            )
            Screen.History -> HistoryScreen(
                historyViewModel = historyViewModel,
                modifier = Modifier.fillMaxSize()
            )
            Screen.Branches -> BranchesScreen(
                branchesViewModel = branchesViewModel,
                modifier = Modifier.fillMaxSize()
            )
            Screen.Repo -> RepoScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                onNavigateToStatus = { onScreenChange(Screen.Status) }
            )
            Screen.Settings -> SettingsScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
