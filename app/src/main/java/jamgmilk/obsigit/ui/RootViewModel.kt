package jamgmilk.obsigit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import jamgmilk.obsigit.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class RootStatus {
    object Idle : RootStatus()
    object Checking : RootStatus()
    object Granted : RootStatus()
    object Denied : RootStatus()
}

class RootViewModel(private val repository: FileRepository = FileRepository()) : ViewModel() {

    private val _status = MutableStateFlow<RootStatus>(RootStatus.Idle)
    val status = _status.asStateFlow()

    private val _folderOwner = MutableStateFlow("Unknown")
    val folderOwner = _folderOwner.asStateFlow()

    fun checkRoot() {
        viewModelScope.launch {
            _status.value = RootStatus.Checking
            val isRooted = withContext(Dispatchers.IO) {
                Shell.getShell().isRoot
            }
            _status.value = if (isRooted) RootStatus.Granted else RootStatus.Denied
        }
    }

    // Renamed to match UI calls and unified the logic
    fun refreshOwner(path: String) {
        viewModelScope.launch {
            _folderOwner.value = "Loading..."
            repository.getFileOwner(path)
                .onSuccess { name -> _folderOwner.value = name }
                .onFailure { _folderOwner.value = "Error" }
        }
    }
}