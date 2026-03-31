package jamgmilk.fuwagit

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import jamgmilk.fuwagit.ui.navigation.rememberNavItems
import jamgmilk.fuwagit.ui.screen.branches.BranchesScreen
import jamgmilk.fuwagit.ui.screen.branches.BranchesViewModel
import jamgmilk.fuwagit.ui.screen.history.HistoryScreen
import jamgmilk.fuwagit.ui.screen.history.HistoryViewModel
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposScreen
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposViewModel
import jamgmilk.fuwagit.ui.screen.settings.SettingsScreen
import jamgmilk.fuwagit.ui.screen.status.StatusScreen
import jamgmilk.fuwagit.ui.screen.status.StatusViewModel
import jamgmilk.fuwagit.ui.theme.FuwaGitTheme
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuwaGitTheme {
                AppRoot()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRoot() {
    val navItems = rememberNavItems()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()

    var isSubPageVisible by remember { mutableStateOf(false) }
    var resetKey by remember { mutableStateOf(0) }

    val pagerState = rememberPagerState(
        pageCount = { navItems.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 4 && isSubPageVisible) {
            isSubPageVisible = false
            resetKey++
        }
    }

    fun navigateToPage(index: Int) {
        scope.launch {
            pagerState.animateScrollToPage(index)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FuwaGitThemeExtras.colors.backgroundBrush)
    ) {
        if (isLandscape) {
            NavigationRail(
                containerColor = FuwaGitThemeExtras.colors.navBarContainer,
                modifier = Modifier.fillMaxHeight()
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = pagerState.currentPage == index && !isSubPageVisible,
                        onClick = { navigateToPage(index) },
                        icon = item.icon,
                        label = { Text(item.title) }
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = FuwaGitThemeExtras.colors.cardBorder
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                userScrollEnabled = !isSubPageVisible,
                key = { it },
                modifier = Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .then(
                        if (isLandscape) Modifier.navigationBarsPadding() else Modifier
                    )
            ) { page ->
                when (page) {
                    0 -> {
                        val statusViewModel: StatusViewModel = hiltViewModel()
                        StatusScreen(
                            statusViewModel = statusViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    1 -> {
                        val historyViewModel: HistoryViewModel = hiltViewModel()
                        HistoryScreen(
                            historyViewModel = historyViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    2 -> {
                        val branchesViewModel: BranchesViewModel = hiltViewModel()
                        BranchesScreen(
                            branchesViewModel = branchesViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    3 -> {
                        val myReposViewModel: MyReposViewModel = hiltViewModel()
                        MyReposScreen(
                            myReposViewModel = myReposViewModel,
                            modifier = Modifier.fillMaxSize(),
                            onNavigateToStatus = {
                                navigateToPage(0)
                            },
                            onSubPageVisibleChange = { visible -> isSubPageVisible = visible }
                        )
                    }
                    4 -> {
                        SettingsScreen(
                            modifier = Modifier.fillMaxSize(),
                            resetKey = resetKey,
                            onSubPageVisibleChange = { visible -> isSubPageVisible = visible }
                        )
                    }
                }
            }

            if (!isLandscape && !isSubPageVisible) {
                NavigationBar(
                    windowInsets = NavigationBarDefaults.windowInsets,
                    containerColor = FuwaGitThemeExtras.colors.navBarContainer
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index && !isSubPageVisible,
                            onClick = { navigateToPage(index) },
                            icon = item.icon,
                            label = { Text(item.title) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    }
}