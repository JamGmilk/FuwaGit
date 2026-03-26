package jamgmilk.obsigit

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import jamgmilk.obsigit.ui.AppPage
import jamgmilk.obsigit.ui.AppViewModel
import jamgmilk.obsigit.ui.BranchesModule
import jamgmilk.obsigit.ui.HistoryModule
import jamgmilk.obsigit.ui.RepoScreen
import jamgmilk.obsigit.ui.SettingsScreen
import jamgmilk.obsigit.ui.StatusModule
import jamgmilk.obsigit.ui.theme.ObsiGitTheme
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras
import jamgmilk.obsigit.ui.theme.appBackgroundBrush

class MainActivity : ComponentActivity() {

    private val appViewModel by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val currentPage by viewModel.currentPage.collectAsState()
    val darkTheme = isSystemInDarkTheme()
    val uiColors = ObsiGitThemeExtras.colors

    LaunchedEffect(Unit) {
        viewModel.initializeStorage(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                //modifier = Modifier.height(64.dp),
                windowInsets = NavigationBarDefaults.windowInsets,
                containerColor = uiColors.navBarContainer
            ) {
                NavigationBarItem(
                    selected = currentPage == AppPage.Status,
                    onClick = { viewModel.switchPage(AppPage.Status) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Status") },
                    label = { Text("Status") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentPage == AppPage.History,
                    onClick = { viewModel.switchPage(AppPage.History) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentPage == AppPage.Branches,
                    onClick = { viewModel.switchPage(AppPage.Branches) },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = "Branches") },
                    label = { Text("Branches") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentPage == AppPage.Repo,
                    onClick = { viewModel.switchPage(AppPage.Repo) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Repo") },
                    label = { Text("Repo") },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    selected = currentPage == AppPage.Settings,
                    onClick = { viewModel.switchPage(AppPage.Settings) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    alwaysShowLabel = false
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(darkTheme = darkTheme))
        ) {
            val contentModifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(innerPadding)

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "page_transition"
            ) { page ->
                when (page) {
                    AppPage.Status -> StatusModule(viewModel = viewModel, modifier = contentModifier)
                    AppPage.History -> HistoryModule(viewModel = viewModel, modifier = contentModifier)
                    AppPage.Branches -> BranchesModule(viewModel = viewModel, modifier = contentModifier)
                    AppPage.Repo -> RepoScreen(viewModel = viewModel, modifier = contentModifier)
                    AppPage.Settings -> SettingsScreen(viewModel = viewModel, modifier = contentModifier)
                }
            }
        }
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
