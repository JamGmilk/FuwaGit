package jamgmilk.fuwagit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.CurrentRepoInfo
import jamgmilk.fuwagit.domain.CurrentRepoManager
import jamgmilk.fuwagit.domain.usecase.CurrentRepoUseCase
import jamgmilk.fuwagit.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val currentRepoUseCase: CurrentRepoUseCase,
    private val currentRepoManager: CurrentRepoManager
) : ViewModel() {

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Status)
    val currentScreenFlow: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _swipeEnabled = MutableStateFlow(true)
    val swipeEnabledFlow: StateFlow<Boolean> = _swipeEnabled.asStateFlow()

    val currentRepoInfo: StateFlow<CurrentRepoInfo> = currentRepoManager.currentRepoInfo

    private var storageInitialized = false

    var swipeEnabled: Boolean
        get() = _swipeEnabled.value
        set(value) { _swipeEnabled.value = value }

    var currentScreen: Screen
        get() = _currentScreen.value
        set(value) { _currentScreen.value = value }

    init {
        viewModelScope.launch {
            currentRepoManager.validationRequest.collect { path ->
                currentRepoUseCase.validateAndSetCurrentRepo(path)
            }
        }
    }

    fun initializeStorage() {
        if (storageInitialized) return
        storageInitialized = true

        viewModelScope.launch {
            currentRepoUseCase.initializeFromStorage()
        }
    }
}