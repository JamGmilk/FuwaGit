package jamgmilk.fuwagit.ui.screen.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import jamgmilk.fuwagit.ui.navigation.NavRoutes
import jamgmilk.fuwagit.ui.navigation.rememberNavItems
import jamgmilk.fuwagit.ui.screen.branches.BranchesScreen
import jamgmilk.fuwagit.ui.screen.branches.BranchesViewModel
import jamgmilk.fuwagit.ui.screen.credentials.CredentialScreen
import jamgmilk.fuwagit.ui.screen.credentials.CredentialStoreViewModel
import jamgmilk.fuwagit.ui.screen.history.HistoryScreen
import jamgmilk.fuwagit.ui.screen.history.HistoryViewModel
import jamgmilk.fuwagit.ui.screen.myrepos.AddRepositoryScreen
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposScreen
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposViewModel
import jamgmilk.fuwagit.ui.screen.permissions.PermissionsScreen
import jamgmilk.fuwagit.ui.screen.settings.SettingsScreen
import jamgmilk.fuwagit.ui.screen.settings.SettingsViewModel
import jamgmilk.fuwagit.ui.screen.status.StatusScreen
import jamgmilk.fuwagit.ui.screen.status.StatusViewModel
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    val uiColors = FuwaGitThemeExtras.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(uiColors.backgroundBrush)
    ) {
        NavHost(
            navController = navController,
            startDestination = NavRoutes.MAIN,
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(220),
                    initialOffsetX = { it }
                ) + fadeIn(animationSpec = tween(220))
            },
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(220),
                    targetOffsetX = { -it / 3 }
                ) + fadeOut(animationSpec = tween(150))
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = tween(220),
                    initialOffsetX = { -it / 3 }
                ) + fadeIn(animationSpec = tween(220))
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(220),
                    targetOffsetX = { it }
                ) + fadeOut(animationSpec = tween(150))
            }
        ) {
            composable(NavRoutes.MAIN) {
                MainScreen(
                    onNavigateToAddRepository = { navController.navigate(NavRoutes.ADD_REPOSITORY) },
                    onNavigateToPermissions = { navController.navigate(NavRoutes.PERMISSIONS) },
                    onNavigateToCredentials = { navController.navigate(NavRoutes.CREDENTIALS) }
                )
            }

            composable(NavRoutes.ADD_REPOSITORY) {
                val myReposViewModel: MyReposViewModel = hiltViewModel()
                val scope = rememberCoroutineScope()
                AddRepositoryScreen(
                    onBack = { navController.popBackStack() },
                    onCloneComplete = { path ->
                        scope.launch {
                            myReposViewModel.setCurrentRepo(path)
                        }
                        navController.popBackStack()
                    },
                    onAddRepository = { path, alias ->
                        scope.launch {
                            myReposViewModel.addRepo(path, alias)
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable(NavRoutes.PERMISSIONS) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                PermissionsScreen(
                    savedReposCount = settingsUiState.savedReposCount,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.CREDENTIALS) {
                val credentialsViewModel: CredentialStoreViewModel = hiltViewModel()
                CredentialScreen(
                    viewModel = credentialsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    onNavigateToAddRepository: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToCredentials: () -> Unit
) {
    // Hoist ViewModels to MainScreen scope so they are created once
    // and reused across page swipes, rather than being recreated inside the Pager.
    val statusViewModel: StatusViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val branchesViewModel: BranchesViewModel = hiltViewModel()
    val myReposViewModel: MyReposViewModel = hiltViewModel()

    val navItems = rememberNavItems()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { navItems.size })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    val navigateToPage: (Int) -> Unit = remember {
        { index ->
            scope.launch {
                pagerState.animateScrollToPage(index)
            }
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
                        selected = currentPage == index,
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
                beyondViewportPageCount = 1,
                key = { it },
                modifier = Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .then(
                        if (isLandscape) Modifier.navigationBarsPadding() else Modifier
                    )
            ) { page ->
                when (page) {
                    0 -> StatusScreen(
                        statusViewModel = statusViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> HistoryScreen(
                        historyViewModel = historyViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> BranchesScreen(
                        branchesViewModel = branchesViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                    3 -> MyReposScreen(
                        myReposViewModel = myReposViewModel,
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToAddRepository = onNavigateToAddRepository
                    )
                    4 -> SettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToPermissions = onNavigateToPermissions,
                        onNavigateToCredentials = onNavigateToCredentials
                    )
                }
            }

            if (!isLandscape) {
                NavigationBar(
                    windowInsets = NavigationBarDefaults.windowInsets,
                    containerColor = FuwaGitThemeExtras.colors.navBarContainer
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = currentPage == index,
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
