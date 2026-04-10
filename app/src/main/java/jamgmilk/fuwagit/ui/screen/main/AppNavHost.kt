package jamgmilk.fuwagit.ui.screen.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import jamgmilk.fuwagit.ui.navigation.AddRepoTab
import jamgmilk.fuwagit.ui.navigation.NavRoutes
import jamgmilk.fuwagit.ui.navigation.rememberNavItems
import jamgmilk.fuwagit.ui.screen.branches.BranchesScreen
import jamgmilk.fuwagit.ui.screen.branches.BranchesViewModel
import jamgmilk.fuwagit.ui.screen.credentials.CredentialScreen
import jamgmilk.fuwagit.ui.screen.credentials.CredentialStoreViewModel
import jamgmilk.fuwagit.ui.screen.filediff.FileDiffScreen
import jamgmilk.fuwagit.ui.screen.filediff.FileDiffViewModel
import jamgmilk.fuwagit.ui.screen.history.HistoryScreen
import jamgmilk.fuwagit.ui.screen.history.HistoryViewModel
import jamgmilk.fuwagit.ui.screen.myrepos.AddRepositoryScreen
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposScreen
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposViewModel
import jamgmilk.fuwagit.ui.screen.onboarding.OnboardingScreen
import jamgmilk.fuwagit.ui.screen.permissions.PermissionsScreen
import jamgmilk.fuwagit.ui.screen.permissions.SshTestResult
import jamgmilk.fuwagit.ui.screen.settings.SettingsScreen
import jamgmilk.fuwagit.ui.screen.settings.SettingsViewModel
import jamgmilk.fuwagit.ui.screen.status.StatusScreen
import jamgmilk.fuwagit.ui.screen.status.StatusViewModel
import jamgmilk.fuwagit.ui.screen.tags.TagsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String = NavRoutes.MAIN) {

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
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
            composable(NavRoutes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(NavRoutes.MAIN) {
                            popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                        }
                    },
                    onAddRepository = { tab ->
                        navController.navigate("add_repository/${tab.name}")
                    }
                )
            }

            composable(NavRoutes.MAIN) {
                MainScreen(
                    onNavigateToAddRepository = { navController.navigate(NavRoutes.ADD_REPOSITORY) },
                    onNavigateToPermissions = { navController.navigate(NavRoutes.PERMISSIONS) },
                    onNavigateToCredentials = { navController.navigate(NavRoutes.CREDENTIALS) },
                    onViewFileDiff = { filePath, diffType ->
                        val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
                        navController.navigate("${NavRoutes.FILE_DIFF}?filePath=$encodedPath&diffType=$diffType")
                    },
                    onViewCommitDiff = { filePath, oldCommit, newCommit ->
                        val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
                        val encodedOld = java.net.URLEncoder.encode(oldCommit, "UTF-8")
                        val encodedNew = java.net.URLEncoder.encode(newCommit, "UTF-8")
                        navController.navigate("${NavRoutes.FILE_DIFF}?filePath=$encodedPath&diffType=COMMIT&oldCommit=$encodedOld&newCommit=$encodedNew")
                    }
                )
            }

            composable(
                route = NavRoutes.ADD_REPOSITORY,
                arguments = listOf()
            ) {
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

            composable(
                route = NavRoutes.ADD_REPOSITORY_WITH_TAB,
                arguments = listOf(
                    navArgument("tab") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val tabName = backStackEntry.arguments?.getString("tab") ?: "Clone"
                val selectedTab = try { AddRepoTab.valueOf(tabName) } catch (e: Exception) { AddRepoTab.Clone }
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
                    },
                    selectedTab = selectedTab
                )
            }

            composable(NavRoutes.PERMISSIONS) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val credentialStoreViewModel: CredentialStoreViewModel = hiltViewModel()
                val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                val credentialUiState by credentialStoreViewModel.uiState.collectAsStateWithLifecycle()
                var sshTestResult by remember { mutableStateOf<SshTestResult>(SshTestResult.Idle) }

                PermissionsScreen(
                    sshKeys = credentialUiState.sshKeys,
                    onTestSshConnection = { host, sshKeyUuid ->
                        credentialStoreViewModel.testSshConnection(host, sshKeyUuid) { result ->
                            sshTestResult = result
                        }
                    },
                    sshTestResult = sshTestResult,
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

            composable(
                route = "${NavRoutes.FILE_DIFF}?filePath={filePath}&diffType={diffType}&oldCommit={oldCommit}&newCommit={newCommit}",
                arguments = listOf(
                    navArgument("filePath") { type = androidx.navigation.NavType.StringType },
                    navArgument("diffType") {
                        type = NavType.StringType
                        defaultValue = "WORKING_TREE"
                    },
                    navArgument("oldCommit") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("newCommit") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val fileDiffViewModel: FileDiffViewModel = hiltViewModel()
                FileDiffScreen(
                    fileDiffViewModel = fileDiffViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToAddRepository: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onViewFileDiff: ((String, String) -> Unit)? = null,
    onViewCommitDiff: ((String, String, String) -> Unit)? = null
) {
    // Hoist ViewModels to MainScreen scope so they are created once
    // and reused across page swipes, rather than being recreated inside the Pager.
    val statusViewModel: StatusViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val branchesViewModel: BranchesViewModel = hiltViewModel()
    val tagsViewModel: TagsViewModel = hiltViewModel()
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

    Row(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight()
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationRailItem(
                        selected = currentPage == index,
                        onClick = { navigateToPage(index) },
                        icon = item.icon,
                        label = { Text(stringResource(item.titleRes)) }
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
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
                        modifier = Modifier.fillMaxSize(),
                        onViewDiff = { filePath, isStaged ->
                            val diffType = if (isStaged) "STAGED" else "WORKING_TREE"
                            onViewFileDiff?.invoke(filePath, diffType)
                        }
                    )
                    1 -> HistoryScreen(
                        historyViewModel = historyViewModel,
                        modifier = Modifier.fillMaxSize(),
                        onViewCommitDiff = { filePath, oldCommit, newCommit ->
                            onViewCommitDiff?.invoke(filePath, oldCommit, newCommit)
                        }
                    )
                    2 -> BranchesScreen(
                        branchesViewModel = branchesViewModel,
                        tagsViewModel = tagsViewModel,
                        modifier = Modifier.fillMaxSize(),
                        onCreateTag = { branchName ->
                            tagsViewModel.showCreateDialog()
                        },
                        onShowInHistory = { branchName ->
                            navigateToPage(1)
                        }
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
                    windowInsets = NavigationBarDefaults.windowInsets
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = currentPage == index,
                            onClick = { navigateToPage(index) },
                            icon = item.icon,
                            label = { Text(stringResource(item.titleRes)) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    }
}
