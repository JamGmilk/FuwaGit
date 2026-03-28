package jamgmilk.fuwagit.ui

import androidx.lifecycle.ViewModel
import jamgmilk.fuwagit.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppPage {
    Status,
    History,
    Branches,
    Repo,
    Settings
}

sealed class RootStatus {
    data object Idle : RootStatus()
    data object Checking : RootStatus()
    data object Granted : RootStatus()
    data object Denied : RootStatus()
}

class AppViewModel : ViewModel() {
    private val _currentPage = MutableStateFlow(AppPage.Status)
    val currentPage: StateFlow<AppPage> = _currentPage.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Status)
    val currentScreenFlow: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _swipeEnabled = MutableStateFlow(true)
    val swipeEnabledFlow: StateFlow<Boolean> = _swipeEnabled.asStateFlow()

    var swipeEnabled: Boolean
        get() = _swipeEnabled.value
        set(value) { _swipeEnabled.value = value }

    var currentScreen: Screen
        get() = _currentScreen.value
        set(value) { _currentScreen.value = value }

    private val _targetPath = MutableStateFlow<String?>(null)
    val targetPath: StateFlow<String?> = _targetPath.asStateFlow()

    private val _rootStatus = MutableStateFlow<RootStatus>(RootStatus.Idle)
    val rootStatus: StateFlow<RootStatus> = _rootStatus.asStateFlow()

    fun switchPage(page: AppPage) {
        _currentPage.value = page
    }

    fun updateTargetPath(path: String?) {
        _targetPath.value = path
    }
}
