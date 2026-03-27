package jamgmilk.obsigit.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jamgmilk.obsigit.ui.AppViewModel
import jamgmilk.obsigit.ui.StatusModule
import jamgmilk.obsigit.ui.screen.branches.BranchesModule
import jamgmilk.obsigit.ui.screen.history.HistoryModule
import jamgmilk.obsigit.ui.screen.repo.RepoScreen
import jamgmilk.obsigit.ui.screen.settings.SettingsScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

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

@Composable
fun ObsiGitNavHost(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val screens = Screen.screens
    
    val pagerState = rememberPagerState(
        initialPage = currentScreen.index,
        pageCount = { screens.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .drop(1)
            .collect { pageIndex ->
                if (pageIndex in screens.indices) {
                    val targetScreen = Screen.fromIndex(pageIndex)
                    if (targetScreen != currentScreen) {
                        onScreenChange(targetScreen)
                    }
                }
            }
    }

    LaunchedEffect(currentScreen) {
        val targetIndex = currentScreen.index
        if (pagerState.settledPage != targetIndex) {
            pagerState.animateScrollToPage(
                page = targetIndex,
                animationSpec = tween(300)
            )
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondViewportPageCount = 1,
        pageSpacing = 0.dp
    ) { pageIndex ->
        val screen = Screen.fromIndex(pageIndex)
        
        when (screen) {
            Screen.Status -> StatusModule(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
            Screen.History -> HistoryModule(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
            Screen.Branches -> BranchesModule(
                viewModel = viewModel,
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
            else -> StatusModule(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
