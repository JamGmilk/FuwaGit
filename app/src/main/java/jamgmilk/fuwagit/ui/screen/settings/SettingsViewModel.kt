package jamgmilk.fuwagit.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.data.local.prefs.GitConfigStore
import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repoDataStore: RepoDataStore,
    private val gitConfigStore: GitConfigStore
) : ViewModel() {

    val savedReposCount: StateFlow<Int> = repoDataStore.getSavedReposFlow()
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val userName: StateFlow<String> = gitConfigStore.configFlow
        .map { it.userName }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val userEmail: StateFlow<String> = gitConfigStore.configFlow
        .map { it.userEmail }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val defaultBranch: StateFlow<String> = gitConfigStore.configFlow
        .map { it.defaultBranch }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "main"
        )

    fun saveUserConfig(name: String, email: String) {
        viewModelScope.launch {
            gitConfigStore.setUserConfig(name, email)
        }
    }

    fun saveDefaultBranch(branch: String) {
        viewModelScope.launch {
            gitConfigStore.setDefaultBranch(branch)
        }
    }

    suspend fun reloadUserConfig() {
        gitConfigStore.reloadFromFile()
    }
}