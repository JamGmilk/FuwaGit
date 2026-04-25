package jamgmilk.fuwagit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import jamgmilk.fuwagit.R

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val ADD_REPOSITORY_WITH_TAB = "add_repository/{tab}"
    const val PERMISSIONS = "permissions"
    const val CREDENTIALS = "credentials"
    const val MASTER_PASSWORD = "master_password"

    const val ADD_REPOSITORY = "add_repository"
    const val STATUS = "status"
    const val HISTORY = "history"
    const val BRANCHES = "branches"
    const val MY_REPOS = "my_repos"
    const val SETTINGS = "settings"

    const val FILE_DIFF = "file_diff"
    const val FILE_DIFF_ROUTE = "file_diff?filePath={filePath}&diffType={diffType}&oldCommit={oldCommit}&newCommit={newCommit}"
}

enum class AddRepoTab {
    Clone,
    Local
}

enum class DiffType {
    WORKING_TREE,
    STAGED,
    COMMIT
}

data class NavItem(
    val route: String,
    val titleRes: Int,
    val icon: @Composable () -> Unit
)

@Composable
fun rememberNavItems(): List<NavItem> = remember {
    listOf(
        NavItem(NavRoutes.STATUS, R.string.nav_status) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.nav_status)) },
        NavItem(NavRoutes.HISTORY, R.string.nav_history) { Icon(Icons.Default.History, contentDescription = stringResource(R.string.nav_history)) },
        NavItem(NavRoutes.BRANCHES, R.string.nav_branches) { Icon(Icons.Default.AccountTree, contentDescription = stringResource(R.string.nav_branches)) },
        NavItem(NavRoutes.MY_REPOS, R.string.nav_my_repos) { Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.nav_my_repos)) },
        NavItem(NavRoutes.SETTINGS, R.string.nav_settings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) }
    )
}
