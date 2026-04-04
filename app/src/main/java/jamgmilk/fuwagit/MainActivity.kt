package jamgmilk.fuwagit

import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import jamgmilk.fuwagit.ui.screen.main.AppNavHost
import jamgmilk.fuwagit.ui.screen.settings.SettingsViewModel
import jamgmilk.fuwagit.ui.theme.MizuiroTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val darkMode = settingsUiState.darkMode

            val isDarkTheme = when (darkMode) {
                "always_on" -> true
                "always_off" -> false
                else -> isSystemInDarkTheme()
            }

            MizuiroTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                AppNavHost(navController)
            }
        }
    }
}
