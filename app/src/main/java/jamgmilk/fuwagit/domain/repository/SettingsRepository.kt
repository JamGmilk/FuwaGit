package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.domain.model.AppPreferences
import kotlinx.coroutines.flow.Flow

data class GitConfigSettings(
    val userName: String = "",
    val userEmail: String = "",
    val defaultBranch: String = "main",
    val setUpstreamOnPush: Boolean = true
)

interface SettingsRepository {
    fun gitConfigFlow(): Flow<GitConfigSettings>

    fun preferencesFlow(): Flow<AppPreferences>

    suspend fun saveUserConfig(name: String, email: String)

    suspend fun saveDefaultBranch(branch: String)

    suspend fun saveSetUpstreamOnPush(enabled: Boolean)

    suspend fun saveAutoSync(enabled: Boolean)

    suspend fun saveConflictSafeMode(enabled: Boolean)

    suspend fun saveBackupBeforeSync(enabled: Boolean)

    suspend fun saveVerboseLogging(enabled: Boolean)

    suspend fun saveDarkMode(mode: String)

    suspend fun saveLanguage(language: String)

    suspend fun saveAutoLockTimeout(timeout: String)

    suspend fun setFirstRunCompleted()

    suspend fun resetFirstRun()
}
