package jamgmilk.fuwagit.ui.screen.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.content.res.Configuration
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import jamgmilk.fuwagit.ui.navigation.AddRepoTab
import jamgmilk.fuwagit.ui.navigation.DiffType
import jamgmilk.fuwagit.ui.navigation.NavRoutes
import jamgmilk.fuwagit.data.jgit.HostKeyAskHelper
import jamgmilk.fuwagit.ui.components.HostKeyAskDialog
import jamgmilk.fuwagit.ui.navigation.rememberNavItems
import jamgmilk.fuwagit.ui.screen.branches.BranchesScreen
import jamgmilk.fuwagit.ui.screen.branches.BranchesViewModel
import jamgmilk.fuwagit.ui.screen.credentials.CredentialScreen
import jamgmilk.fuwagit.ui.screen.credentials.CredentialStoreEvent
import jamgmilk.fuwagit.ui.screen.credentials.CredentialStoreViewModel
import jamgmilk.fuwagit.ui.screen.credentials.MasterPasswordScreen
import jamgmilk.fuwagit.ui.screen.credentials.UnlockDialog
import jamgmilk.fuwagit.ui.screen.filediff.FileDiffScreen
import jamgmilk.fuwagit.ui.screen.filediff.FileDiffViewModel
import jamgmilk.fuwagit.ui.screen.history.DiffViewRequest
import jamgmilk.fuwagit.ui.screen.history.HistoryScreen
import jamgmilk.fuwagit.ui.screen.history.HistoryViewModel
import jamgmilk.fuwagit.ui.screen.myrepos.AddRepositoryScreen
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposScreen
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposViewModel
import jamgmilk.fuwagit.ui.screen.onboarding.OnboardingScreen
import jamgmilk.fuwagit.ui.screen.permissions.PermissionsScreen
import jamgmilk.fuwagit.ui.screen.permissions.SshTestResult
import jamgmilk.fuwagit.ui.screen.settings.SettingsScreen
import jamgmilk.fuwagit.ui.screen.status.StatusScreen
import jamgmilk.fuwagit.ui.screen.status.StatusViewModel
import jamgmilk.fuwagit.ui.screen.tags.TagsViewModel
import kotlinx.coroutines.launch

private object TransitionDefaults {
    val Enter = slideInHorizontally(
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        initialOffsetX = { it }
    ) + fadeIn(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))

    val Exit = slideOutHorizontally(
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        targetOffsetX = { -it / 3 }
    ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))

    val PopEnter = slideInHorizontally(
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        initialOffsetX = { -it / 3 }
    ) + fadeIn(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))

    val PopExit = slideOutHorizontally(
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        targetOffsetX = { it }
    ) + fadeOut(animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
}

private fun Context?.requireActivity(): FragmentActivity {
    return (this as? FragmentActivity) ?: throw IllegalStateException("FragmentActivity is required")
}

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String = NavRoutes.MAIN) {
    val context = LocalContext.current
    val activity = context.requireActivity()
    val pendingRequests = remember { mutableStateListOf<HostKeyAskHelper.HostKeyRequest>() }
    val credentialStoreViewModel: CredentialStoreViewModel = hiltViewModel(
        viewModelStoreOwner = activity
    )

    LaunchedEffect(Unit) {
        HostKeyAskHelper.requests.collect { request ->
            pendingRequests.add(request)
        }
    }

    val currentRequest = pendingRequests.firstOrNull()

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        currentRequest?.let { request ->
            HostKeyAskDialog(
                host = request.host,
                keyType = request.keyType,
                fingerprint = request.fingerprint,
                onAccept = {
                    request.future.complete(true)
                    pendingRequests.removeAt(0)
                },
                onReject = {
                    request.future.complete(false)
                    pendingRequests.removeAt(0)
                }
            )
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { TransitionDefaults.Enter },
            exitTransition = { TransitionDefaults.Exit },
            popEnterTransition = { TransitionDefaults.PopEnter },
            popExitTransition = { TransitionDefaults.PopExit }
        ) {
            composable(NavRoutes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(NavRoutes.MAIN) {
                            popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                        }
                    },
                    onAddRepository = { tab ->
                        navController.navigate("${NavRoutes.ADD_REPOSITORY}/${tab.name}")
                    }
                )
            }

            composable(NavRoutes.MAIN) {
                MainScreen(
                    credentialStoreViewModel = credentialStoreViewModel,
                    onNavigateToAddRepository = {
                        navController.navigate("${NavRoutes.ADD_REPOSITORY}/${AddRepoTab.Clone.name}")
                    },
                    onNavigateToPermissions = {
                        navController.navigate(NavRoutes.PERMISSIONS)
                    },
                    onNavigateToCredentials = {
                        navController.navigate(NavRoutes.CREDENTIALS)
                    },
                    onNavigateToMasterPassword = {
                        navController.navigate(NavRoutes.MASTER_PASSWORD) {
                            popUpTo(NavRoutes.MAIN) { saveState = true }
                        }
                    },
                    onMasterPasswordSuccess = {
                        navController.popBackStack()
                    },
                    onViewFileDiff = { filePath, diffType ->
                        val encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8.name())
                        navController.navigate("${NavRoutes.FILE_DIFF}?filePath=$encodedPath&diffType=${diffType.name}")
                    },
                    onViewCommitDiff = { request ->
                        val encodedPath = URLEncoder.encode(request.filePath, StandardCharsets.UTF_8.name())
                        val encodedOld = URLEncoder.encode(request.oldCommitHash, StandardCharsets.UTF_8.name())
                        val encodedNew = URLEncoder.encode(request.newCommitHash, StandardCharsets.UTF_8.name())
                        navController.navigate("${NavRoutes.FILE_DIFF}?filePath=$encodedPath&diffType=${DiffType.COMMIT.name}&oldCommit=$encodedOld&newCommit=$encodedNew")
                    }
                )
            }

            composable(
                route = NavRoutes.ADD_REPOSITORY_WITH_TAB,
                arguments = listOf(
                    navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = "Clone"
                    }
                )
            ) { backStackEntry ->
                val tabName = backStackEntry.arguments?.getString("tab") ?: AddRepoTab.Clone.name
                val selectedTab = try { AddRepoTab.valueOf(tabName) } catch (_: Exception) { AddRepoTab.Clone }
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
                val activity = LocalContext.current.requireActivity()
                val credentialUiState by credentialStoreViewModel.uiState.collectAsStateWithLifecycle()
                var sshTestResult by remember { mutableStateOf<SshTestResult>(SshTestResult.Idle) }

                PermissionsScreen(
                    sshKeys = credentialUiState.sshKeys,
                    onTestSshConnection = { host, sshKeyUuid ->
                        if (!credentialUiState.isDecryptionUnlocked) {
                            credentialStoreViewModel.showUnlockDialog()
                            return@PermissionsScreen
                        }
                        credentialStoreViewModel.testSshConnection(host, sshKeyUuid) { result ->
                            sshTestResult = result
                        }
                    },
                    sshTestResult = sshTestResult,
                    onBack = { navController.popBackStack() }
                )

                if (credentialUiState.showUnlockDialog) {
                    UnlockDialog(
                        onDismiss = { credentialStoreViewModel.dismissUnlockDialog() },
                        onUnlock = { password ->
                            credentialStoreViewModel.unlockWithPassword(password)
                        },
                        biometricEnabled = credentialUiState.isBiometricEnabled,
                        onUnlockWithBiometric = {
                            credentialStoreViewModel.unlockWithBiometric(activity)
                        }
                    )
                }
            }

            composable(NavRoutes.CREDENTIALS) {
                val activity = LocalContext.current.requireActivity()
                val credentialsViewModel: CredentialStoreViewModel = hiltViewModel(
                    viewModelStoreOwner = activity
                )
                CredentialScreen(
                    viewModel = credentialsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.MASTER_PASSWORD) {
                MasterPasswordScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            composable(
                route = NavRoutes.FILE_DIFF_ROUTE,
                arguments = listOf(
                    navArgument("filePath") { type = NavType.StringType },
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
            ) {
                val fileDiffViewModel: FileDiffViewModel = hiltViewModel()
                FileDiffScreen(
                    fileDiffViewModel = fileDiffViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private enum class MainPage {
    Status,
    History,
    Branches,
    MyRepos,
    Settings
}

@Composable
fun MainScreen(
    credentialStoreViewModel: CredentialStoreViewModel,
    onNavigateToAddRepository: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToMasterPassword: () -> Unit,
    onMasterPasswordSuccess: () -> Unit = {},
    onViewFileDiff: ((String, DiffType) -> Unit)? = null,
    onViewCommitDiff: ((DiffViewRequest) -> Unit)? = null
) {
    val statusViewModel: StatusViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val branchesViewModel: BranchesViewModel = hiltViewModel()
    val tagsViewModel: TagsViewModel = hiltViewModel()
    val myReposViewModel: MyReposViewModel = hiltViewModel()

    val navItems = rememberNavItems()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
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

    LaunchedEffect(Unit) {
        credentialStoreViewModel.events.collect { event ->
            when (event) {
                is CredentialStoreEvent.UnlockSuccess -> {
                    statusViewModel.onCredentialUnlocked()
                    myReposViewModel.onCredentialUnlocked()
                }
                else -> { }
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
                beyondViewportPageCount = navItems.size - 1,
                key = { it },
                modifier = Modifier
                    .weight(1f)
                    .statusBarsPadding()
                    .then(
                        if (isLandscape) Modifier.navigationBarsPadding() else Modifier
                    )
            ) { page ->
                when (MainPage.entries.getOrNull(page)) {
                    MainPage.Status -> StatusScreen(
                        statusViewModel = statusViewModel,
                        modifier = Modifier.fillMaxSize(),
                        onViewDiff = { filePath, isStaged ->
                            val diffType = if (isStaged) DiffType.STAGED else DiffType.WORKING_TREE
                            onViewFileDiff?.invoke(filePath, diffType)
                        }
                    )
                    MainPage.History -> HistoryScreen(
                        historyViewModel = historyViewModel,
                        modifier = Modifier.fillMaxSize(),
                        onViewCommitDiff = onViewCommitDiff
                    )
                    MainPage.Branches -> BranchesScreen(
                        branchesViewModel = branchesViewModel,
                        tagsViewModel = tagsViewModel,
                        modifier = Modifier.fillMaxSize(),
                        onCreateTag = { branchName ->
                            tagsViewModel.showCreateDialog(branchName)
                        },
                        onShowInHistory = { branchName ->
                            historyViewModel.filterByBranch(branchName)
                            navigateToPage(MainPage.History.ordinal)
                        }
                    )
                    MainPage.MyRepos -> MyReposScreen(
                        myReposViewModel = myReposViewModel,
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToAddRepository = onNavigateToAddRepository
                    )
                    MainPage.Settings -> SettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToPermissions = onNavigateToPermissions,
                        onNavigateToCredentials = onNavigateToCredentials,
                        onNavigateToMasterPassword = onNavigateToMasterPassword,
                        onMasterPasswordSuccess = onMasterPasswordSuccess
                    )
                    null -> { }
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
