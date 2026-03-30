package jamgmilk.fuwagit.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

sealed class Screen(val route: String, val index: Int) {
    data object Status : Screen("status", 0)
    data object History : Screen("history", 1)
    data object Branches : Screen("branches", 2)
    data object MyRepos : Screen("my_repos", 3)
    data object Settings : Screen("settings", 4)

    companion object {
        val screens: List<Screen> = listOf(Status, History, Branches, MyRepos, Settings)
        fun fromIndex(index: Int): Screen = screens.getOrNull(index) ?: Status
    }
}

@Composable
fun FuwaGitNavHost(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = currentScreen.index,
        pageCount = { Screen.screens.size }
    )

    LaunchedEffect(currentScreen) {
        if (pagerState.currentPage != currentScreen.index) {
            pagerState.scrollToPage(currentScreen.index)
        }
    }

    val statusViewModel: StatusViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val branchesViewModel: BranchesViewModel = hiltViewModel()
    val myReposViewModel: MyReposViewModel = hiltViewModel()

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondViewportPageCount = 1,
        pageSpacing = 0.dp,
        userScrollEnabled = false
    ) { pageIndex ->
        val screen = Screen.fromIndex(pageIndex)

        when (screen) {
            Screen.Status -> StatusScreen(
                statusViewModel = statusViewModel,
                appViewModel = viewModel,
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
