package jamgmilk.fuwagit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appPreferencesStore: AppPreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            val isDarkTheme = when (settingsUiState.darkMode) {
                "always_on" -> true
                "always_off" -> false
                else -> isSystemInDarkTheme()
            }

            MizuiroTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    startDestination = if (appPreferencesStore.isFirstRun) NavRoutes.ONBOARDING else NavRoutes.MAIN
                )
            }
        }
    }
}
