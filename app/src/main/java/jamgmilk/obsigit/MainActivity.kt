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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import jamgmilk.obsigit.ui.AppPage
import jamgmilk.obsigit.ui.AppViewModel
import jamgmilk.obsigit.ui.GitTerminalScreen
import jamgmilk.obsigit.ui.SettingsScreen
import jamgmilk.obsigit.ui.VaultScreen
import jamgmilk.obsigit.ui.theme.ObsiGitTheme
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
    val currentPage by viewModel.currentPage.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentPage == AppPage.GitTerminal,
                    onClick = { viewModel.switchPage(AppPage.GitTerminal) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Git") },
                    label = { Text("Git") }
                )
                NavigationBarItem(
                    selected = currentPage == AppPage.Vault,
                    onClick = { viewModel.switchPage(AppPage.Vault) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Vault") },
                    label = { Text("Vault") }
                )
                NavigationBarItem(
                    selected = currentPage == AppPage.Settings,
                    onClick = { viewModel.switchPage(AppPage.Settings) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .background(appBackgroundBrush())
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
                AppPage.GitTerminal -> GitTerminalScreen(viewModel = viewModel, modifier = contentModifier)
                AppPage.Vault -> VaultScreen(viewModel = viewModel, modifier = contentModifier)
                AppPage.Settings -> SettingsScreen(viewModel = viewModel, modifier = contentModifier)
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
