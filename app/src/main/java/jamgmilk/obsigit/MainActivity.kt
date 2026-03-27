package jamgmilk.obsigit

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
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
import jamgmilk.obsigit.di.AppContainer
import jamgmilk.obsigit.ui.AppViewModel
import jamgmilk.obsigit.ui.navigation.ObsiGitNavHost
import jamgmilk.obsigit.ui.navigation.Screen
import jamgmilk.obsigit.ui.theme.ObsiGitExtraColors
import jamgmilk.obsigit.ui.theme.ObsiGitTheme
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras
import jamgmilk.obsigit.ui.theme.appBackgroundBrush

class MainActivity : ComponentActivity() {

    private val appViewModel by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.initCredentialRepository(this)
        enableEdgeToEdge()
        setContent {
            ObsiGitTheme {
                AppRoot(viewModel = appViewModel)
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val uiColors = ObsiGitThemeExtras.colors

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

    LaunchedEffect(Unit) {
        viewModel.initializeStorage(context)
    }

    Box(
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
        } else {
            PortraitLayout(
                currentScreen = currentScreen,
                screens = screens,
                onScreenChange = { viewModel.currentScreen = it },
                uiColors = uiColors
            )
        }

        ObsiGitNavHost(
            currentScreen = currentScreen,
            onScreenChange = { viewModel.currentScreen = it },
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .then(
                    if (isLandscape) {
                        Modifier.padding(start = 80.dp)
                    } else {
                        Modifier.padding(bottom = 80.dp)
                    }
                )
        )
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun PortraitLayout(
    currentScreen: Screen,
    screens: List<Screen>,
    onScreenChange: (Screen) -> Unit,
    uiColors: ObsiGitExtraColors
) {
    val colors = MaterialTheme.colorScheme

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        bottomBar = {
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
    ) { }
}

@Composable
private fun LandscapeLayout(
    currentScreen: Screen,
    screens: List<Screen>,
    onScreenChange: (Screen) -> Unit,
    uiColors: ObsiGitExtraColors
) {
    Row(modifier = Modifier.fillMaxSize()) {
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
    ObsiGitTheme {
        AppRoot(viewModel = AppViewModel())
    }
}
