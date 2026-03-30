package jamgmilk.fuwagit.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import jamgmilk.fuwagit.ui.AppViewModel
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposScreen
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposViewModel
import jamgmilk.fuwagit.ui.screen.branches.BranchesScreen
import jamgmilk.fuwagit.ui.screen.branches.BranchesViewModel
import jamgmilk.fuwagit.ui.screen.history.HistoryScreen
import jamgmilk.fuwagit.ui.screen.history.HistoryViewModel
import jamgmilk.fuwagit.ui.screen.status.StatusScreen
import jamgmilk.fuwagit.ui.screen.status.StatusViewModel
import jamgmilk.fuwagit.ui.screen.settings.SettingsScreen
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val index: Int) {
    data object Status : Screen("status", 0)
    data object History : Screen("history", 1)
    data object Branches : Screen("branches", 2)
    data object MyRepos : Screen("my_repos", 3)
    data object Settings : Screen("settings", 4)

    companion object {
        val screens: List<Screen> by lazy { listOf(Status, History, Branches, MyRepos, Settings) }
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

    val statusViewModel: StatusViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val branchesViewModel: BranchesViewModel = hiltViewModel()
    val myReposViewModel: MyReposViewModel = hiltViewModel()
    
    val currentScreenState by viewModel.currentScreenFlow.collectAsState()
    val swipeEnabled by viewModel.swipeEnabledFlow.collectAsState()

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

    val uiState by myReposViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.targetPath) {
        viewModel.updateTargetPath(uiState.targetPath)
    }

    LaunchedEffect(currentScreen) {
        if (pagerState.currentPage != currentScreen.index) {
            pagerState.animateScrollToPage(
                currentScreen.index,
                animationSpec = tween(250)
            )
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
                myReposViewModel = myReposViewModel,
                modifier = Modifier.fillMaxSize()
            )
            Screen.History -> HistoryScreen(
                historyViewModel = historyViewModel,
                myReposViewModel = myReposViewModel,
                modifier = Modifier.fillMaxSize()
            )
            Screen.Branches -> BranchesScreen(
                branchesViewModel = branchesViewModel,
                myReposViewModel = myReposViewModel,
                modifier = Modifier.fillMaxSize()
            )
            Screen.MyRepos -> MyReposScreen(
                myReposViewModel = myReposViewModel,
                appViewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                onNavigateToStatus = { onScreenChange(Screen.Status) }
            )
            Screen.Settings -> SettingsScreen(
                viewModel = viewModel,
                myReposViewModel = myReposViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
