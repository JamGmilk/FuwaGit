package jamgmilk.fuwagit

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import jamgmilk.fuwagit.ui.navigation.FuwaGitNavHost
import jamgmilk.fuwagit.ui.navigation.NavRoutes
import jamgmilk.fuwagit.ui.theme.FuwaGitTheme
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import jamgmilk.fuwagit.ui.theme.appBackgroundBrush

data class NavItem(
    val route: String,
    val title: String,
    val icon: @Composable () -> Unit
)

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

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val darkTheme = isSystemInDarkTheme()
    val uiColors = FuwaGitThemeExtras.colors

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val navItems = listOf(
        NavItem(NavRoutes.STATUS, "Status") { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Status") },
        NavItem(NavRoutes.HISTORY, "History") { Icon(Icons.Default.History, contentDescription = "History") },
        NavItem(NavRoutes.BRANCHES, "Branches") { Icon(Icons.Default.AccountTree, contentDescription = "Branches") },
        NavItem(NavRoutes.MY_REPOS, "My Repos") { Icon(Icons.Default.Folder, contentDescription = "My Repos") },
        NavItem(NavRoutes.SETTINGS, "Settings") { Icon(Icons.Default.Settings, contentDescription = "Settings") }
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navigateToItem: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackgroundBrush(darkTheme = darkTheme))
    ) {
        if (isLandscape) {
            NavigationRail(
                containerColor = uiColors.navBarContainer,
                modifier = Modifier.fillMaxHeight()
            ) {
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                    NavigationRailItem(
                        selected = selected,
                        onClick = { navigateToItem(item.route) },
                        icon = item.icon,
                        label = { Text(item.title) },
                        alwaysShowLabel = true
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = uiColors.cardBorder
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                FuwaGitNavHost(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .then(
                            if (isLandscape) Modifier.navigationBarsPadding() else Modifier
                        )
                )
            }

            if (!isLandscape) {
                NavigationBar(
                    windowInsets = NavigationBarDefaults.windowInsets,
                    containerColor = uiColors.navBarContainer
                ) {
                    navItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToItem(item.route) },
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
