package jamgmilk.fuwagit

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import jamgmilk.fuwagit.data.local.prefs.AppPreferencesStore
import jamgmilk.fuwagit.ui.navigation.NavRoutes
import jamgmilk.fuwagit.ui.screen.main.AppNavHost
import jamgmilk.fuwagit.ui.screen.settings.SettingsViewModel
import jamgmilk.fuwagit.ui.theme.MizuiroTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appPreferencesStore: AppPreferencesStore

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
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

        applyStoredLanguage()
    }

    private fun applyStoredLanguage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val language = runBlocking {
                appPreferencesStore.preferencesFlow.map { it.language }.first()
            }
            val localeManager = getSystemService(LocaleManager::class.java)
            val targetLocaleList = when (language) {
                "zh_CN" -> LocaleList.forLanguageTags("zh-CN")
                "en" -> LocaleList.forLanguageTags("en")
                else -> LocaleList.getEmptyLocaleList()
            }
            if (localeManager.applicationLocales != targetLocaleList) {
                localeManager.applicationLocales = targetLocaleList
            }
        }
    }
}
