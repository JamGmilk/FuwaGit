package jamgmilk.fuwagit.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import jamgmilk.fuwagit.ui.screen.branches.BranchesScreen
import jamgmilk.fuwagit.ui.screen.branches.BranchesViewModel
import jamgmilk.fuwagit.ui.screen.history.HistoryScreen
import jamgmilk.fuwagit.ui.screen.history.HistoryViewModel
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposScreen
import jamgmilk.fuwagit.ui.screen.myrepos.MyReposViewModel
import jamgmilk.fuwagit.ui.screen.settings.SettingsScreen
import jamgmilk.fuwagit.ui.screen.status.StatusScreen
import jamgmilk.fuwagit.ui.screen.status.StatusViewModel

object NavRoutes {
    const val STATUS = "status"
    const val HISTORY = "history"
    const val BRANCHES = "branches"
    const val MY_REPOS = "my_repos"
    const val SETTINGS = "settings"
}

data class NavItem(
    val route: String,
    val title: String,
    val icon: @Composable () -> Unit
)

@Composable
fun rememberNavItems(): List<NavItem> = remember {
    listOf(
        NavItem(NavRoutes.STATUS, "Status") { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Status") },
        NavItem(NavRoutes.HISTORY, "History") { Icon(Icons.Default.History, contentDescription = "History") },
        NavItem(NavRoutes.BRANCHES, "Branches") { Icon(Icons.Default.AccountTree, contentDescription = "Branches") },
        NavItem(NavRoutes.MY_REPOS, "My Repos") { Icon(Icons.Default.Folder, contentDescription = "My Repos") },
        NavItem(NavRoutes.SETTINGS, "Settings") { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    )
}

@Composable
fun FuwaGitNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.STATUS,
        modifier = modifier
    ) {
        composable(NavRoutes.STATUS) {
            val statusViewModel: StatusViewModel = hiltViewModel()
            StatusScreen(
                statusViewModel = statusViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(NavRoutes.HISTORY) {
            val historyViewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(
                historyViewModel = historyViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(NavRoutes.BRANCHES) {
            val branchesViewModel: BranchesViewModel = hiltViewModel()
            BranchesScreen(
                branchesViewModel = branchesViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(NavRoutes.MY_REPOS) {
            val myReposViewModel: MyReposViewModel = hiltViewModel()
            MyReposScreen(
                myReposViewModel = myReposViewModel,
                modifier = Modifier.fillMaxSize(),
                onNavigateToStatus = {
                    navController.navigate(NavRoutes.STATUS) {
                        popUpTo(NavRoutes.MY_REPOS) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}