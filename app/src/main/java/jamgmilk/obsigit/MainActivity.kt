package jamgmilk.obsigit

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jamgmilk.obsigit.ui.FolderInfoScreen
import jamgmilk.obsigit.ui.GitTerminalScreen
import jamgmilk.obsigit.ui.GitTerminalViewModel
import jamgmilk.obsigit.ui.KawaiiFileManager
import jamgmilk.obsigit.ui.LavenderMist
import jamgmilk.obsigit.ui.RootCheckScreen
import jamgmilk.obsigit.ui.RootViewModel
import jamgmilk.obsigit.ui.SakuraPink
import jamgmilk.obsigit.ui.theme.ObsiGitTheme

class MainActivity : ComponentActivity() {

    private val rootViewModel by viewModels<RootViewModel>()

    private val gitTerminalViewModel by viewModels<GitTerminalViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            ObsiGitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(viewModel = rootViewModel, modifier = Modifier.padding(innerPadding))
                }

            }
        }

    }

}

@Composable
fun MainScreen(viewModel: RootViewModel, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LavenderMist, SakuraPink)))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RootCheckScreen(viewModel = viewModel)
            Spacer(Modifier.height(16.dp))
            FolderInfoScreen(viewModel = viewModel)
            Spacer(Modifier.height(16.dp))
            KawaiiFileManager()
        }
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ObsiGitTheme {
        MainScreen(viewModel = RootViewModel())
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun RootCheckScreenPreview() {
    ObsiGitTheme {
        RootCheckScreen(viewModel = RootViewModel())
    }
}
