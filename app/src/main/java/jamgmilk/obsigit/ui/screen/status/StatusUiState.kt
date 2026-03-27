package jamgmilk.obsigit.ui.screen.status

import androidx.compose.runtime.Immutable

@Immutable
data class StatusUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val repoPath: String? = null,
    val branch: String = "",
    val hasUncommittedChanges: Boolean = false,
    val untrackedCount: Int = 0,
    val statusMessage: String = "Select a target repo"
)
