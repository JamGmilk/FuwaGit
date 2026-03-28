package jamgmilk.fuwagit

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.di.AppContainer
import jamgmilk.fuwagit.ui.AppViewModel
import jamgmilk.fuwagit.ui.navigation.FuwaGitNavHost
import jamgmilk.fuwagit.ui.navigation.Screen
import jamgmilk.fuwagit.ui.theme.FuwaGitExtraColors
import jamgmilk.fuwagit.ui.theme.FuwaGitTheme
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import jamgmilk.fuwagit.ui.theme.appBackgroundBrush

class MainActivity : ComponentActivity() {

    private val appViewModel by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            FuwaGitTheme {
                AppRoot(viewModel = appViewModel)
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val uiColors = FuwaGitThemeExtras.colors

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentScreen by viewModel.currentScreenFlow.collectAsState()
    val screens = listOf(
        Screen.Status,
        Screen.History,
        Screen.Branches,
        Screen.Repo,
        Screen.Settings
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackgroundBrush(darkTheme = darkTheme))
    ) {
        if (isLandscape) {
            LandscapeLayout(
                currentScreen = currentScreen,
                screens = screens,
                onScreenChange = { viewModel.currentScreen = it },
                uiColors = uiColors
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(1f)) {
                FuwaGitNavHost(
                    currentScreen = currentScreen,
                    onScreenChange = { viewModel.currentScreen = it },
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .then(
                            if (isLandscape) Modifier.navigationBarsPadding() else Modifier
                        )
                )
            }

            if (!isLandscape) {
                PortraitLayout(
                    currentScreen = currentScreen,
                    screens = screens,
                    onScreenChange = { viewModel.currentScreen = it },
                    uiColors = uiColors
                )
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    currentScreen: Screen,
    screens: List<Screen>,
    onScreenChange: (Screen) -> Unit,
    uiColors: FuwaGitExtraColors
) {
    NavigationBar(
        windowInsets = NavigationBarDefaults.windowInsets,
        containerColor = uiColors.navBarContainer
    ) {
        screens.forEach { screen ->
            val selected = currentScreen == screen

            NavigationBarItem(
                selected = selected,
                onClick = { onScreenChange(screen) },
                icon = {
                    when (screen) {
                        Screen.Status -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Status")
                        Screen.History -> Icon(Icons.Default.History, contentDescription = "History")
                        Screen.Branches -> Icon(Icons.Default.AccountTree, contentDescription = "Branches")
                        Screen.Repo -> Icon(Icons.Default.Folder, contentDescription = "Repo")
                        Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                label = {
                    Text(
                        when (screen) {
                            Screen.Status -> "Status"
                            Screen.History -> "History"
                            Screen.Branches -> "Branches"
                            Screen.Repo -> "Repo"
                            Screen.Settings -> "Settings"
                        }
                    )
                },
                alwaysShowLabel = false
            )
        }
    }
}

@Composable
private fun LandscapeLayout(
    currentScreen: Screen,
    screens: List<Screen>,
    onScreenChange: (Screen) -> Unit,
    uiColors: FuwaGitExtraColors
) {
    Row(modifier = Modifier.fillMaxHeight()) {
        NavigationRail(
            containerColor = uiColors.navBarContainer,
            modifier = Modifier.fillMaxHeight()
        ) {
            screens.forEach { screen ->
                val selected = currentScreen == screen

                NavigationRailItem(
                    selected = selected,
                    onClick = { onScreenChange(screen) },
                    icon = {
                        when (screen) {
                            Screen.Status -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Status")
                            Screen.History -> Icon(Icons.Default.History, contentDescription = "History")
                            Screen.Branches -> Icon(Icons.Default.AccountTree, contentDescription = "Branches")
                            Screen.Repo -> Icon(Icons.Default.Folder, contentDescription = "Repo")
                            Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    label = {
                        Text(
                            when (screen) {
                                Screen.Status -> "Status"
                                Screen.History -> "History"
                                Screen.Branches -> "Branches"
                                Screen.Repo -> "Repo"
                                Screen.Settings -> "Settings"
                            }
                        )
                    },
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
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun AppRootPreview() {
    FuwaGitTheme {
        AppRoot(viewModel = AppViewModel())
    }
}
