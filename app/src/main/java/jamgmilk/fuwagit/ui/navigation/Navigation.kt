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
    const val ADD_REPOSITORY = "add_repository"
    const val ADD_REPOSITORY_WITH_TAB = "add_repository/{tab}"
    const val PERMISSIONS = "permissions"
    const val CREDENTIALS = "credentials"
    const val MASTER_PASSWORD = "master_password"
    const val FILE_DIFF = "file_diff"
}

enum class AddRepoTab {
    Clone,
    Local
}

data class NavItem(
    val route: String,
    val titleRes: Int,
    val icon: @Composable () -> Unit
)

@Composable
fun rememberNavItems(): List<NavItem> = remember {
    listOf(
        NavItem("status", R.string.nav_status) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.nav_status)) },
        NavItem("history", R.string.nav_history) { Icon(Icons.Default.History, contentDescription = stringResource(R.string.nav_history)) },
        NavItem("branches", R.string.nav_branches) { Icon(Icons.Default.AccountTree, contentDescription = stringResource(R.string.nav_branches)) },
        NavItem("my_repos", R.string.nav_my_repos) { Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.nav_my_repos)) },
        NavItem("settings", R.string.nav_settings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) }
    )
}
