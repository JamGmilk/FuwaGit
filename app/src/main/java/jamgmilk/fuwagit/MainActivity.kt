package jamgmilk.fuwagit

import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import jamgmilk.fuwagit.data.local.prefs.AppPreferencesStore
import jamgmilk.fuwagit.ui.navigation.NavRoutes
import jamgmilk.fuwagit.ui.screen.main.AppNavHost
import jamgmilk.fuwagit.ui.screen.settings.SettingsViewModel
import jamgmilk.fuwagit.ui.theme.MizuiroTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var appPreferencesStore: AppPreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            val isDarkTheme = when (settingsUiState.darkMode) {
                "always_on" -> true
                "always_off" -> false
                else -> isSystemInDarkTheme()
            }

            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme

            MizuiroTheme(
                darkTheme = isDarkTheme,
                dynamicColor = settingsUiState.dynamicColor
            ) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    startDestination = if (appPreferencesStore.isFirstRun) NavRoutes.ONBOARDING else NavRoutes.MAIN
                )
            }
        }
    }
}
