package jamgmilk.fuwagit.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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

@Composable
fun FuwaGitNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val statusViewModel: StatusViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val branchesViewModel: BranchesViewModel = hiltViewModel()
    val myReposViewModel: MyReposViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.STATUS,
        modifier = modifier
    ) {
        composable(NavRoutes.STATUS) {
            StatusScreen(
                statusViewModel = statusViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(NavRoutes.HISTORY) {
            HistoryScreen(
                historyViewModel = historyViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(NavRoutes.BRANCHES) {
            BranchesScreen(
                branchesViewModel = branchesViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(NavRoutes.MY_REPOS) {
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
                myReposViewModel = myReposViewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
