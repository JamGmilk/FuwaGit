package jamgmilk.fuwagit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

object NavRoutes {
    const val MAIN = "main"
    const val ADD_REPOSITORY = "add_repository"
    const val PERMISSIONS = "permissions"
    const val CREDENTIALS = "credentials"
    const val FILE_DIFF = "file_diff"
}

data class NavItem(
    val route: String,
    val title: String,
    val icon: @Composable () -> Unit
)

@Composable
fun rememberNavItems(): List<NavItem> = remember {
    listOf(
        NavItem("status", "Status") { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Status") },
        NavItem("history", "History") { Icon(Icons.Default.History, contentDescription = "History") },
        NavItem("branches", "Branches") { Icon(Icons.Default.AccountTree, contentDescription = "Branches") },
        NavItem("tags", "Tags") { Icon(Icons.Default.Label, contentDescription = "Tags") },
        NavItem("my_repos", "My Repos") { Icon(Icons.Default.Folder, contentDescription = "My Repos") },
        NavItem("settings", "Settings") { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    )
}
