package jamgmilk.fuwagit.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain model for Git config settings.
 */
data class GitConfigSettings(
    val userName: String = "",
    val userEmail: String = "",
    val defaultBranch: String = "main"
)

/**
 * Domain model for app preferences.
 */
data class AppPreferences(
    val autoSync: Boolean = false,
    val conflictSafeMode: Boolean = true,
    val backupBeforeSync: Boolean = true,
    val verboseLogging: Boolean = false,
    val darkMode: String = "system",
    val language: String = "system",
    val autoLockTimeout: String = "300"
)

/**
 * Domain interface for app settings persistence.
 * Abstracts GitConfigDataStore and AppPreferencesStore.
 */
interface SettingsRepository {
    /**
     * Observe Git config settings as a Flow.
     */
    fun gitConfigFlow(): Flow<GitConfigSettings>

    /**
     * Observe app preferences as a Flow.
     */
    fun preferencesFlow(): Flow<AppPreferences>

    /**
     * Save Git user config (name and email together).
     */
    suspend fun saveUserConfig(name: String, email: String)

    /**
     * Save default branch.
     */
    suspend fun saveDefaultBranch(branch: String)

    /**
     * Save auto-sync preference.
     */
    suspend fun saveAutoSync(enabled: Boolean)

    /**
     * Save conflict safe mode preference.
     */
    suspend fun saveConflictSafeMode(enabled: Boolean)

    /**
     * Save backup before sync preference.
     */
    suspend fun saveBackupBeforeSync(enabled: Boolean)

    /**
     * Save verbose logging preference.
     */
    suspend fun saveVerboseLogging(enabled: Boolean)

    /**
     * Save dark mode preference.
     */
    suspend fun saveDarkMode(mode: String)

    /**
     * Save language preference.
     */
    suspend fun saveLanguage(language: String)

    /**
     * Save auto-lock timeout preference.
     */
    suspend fun saveAutoLockTimeout(timeout: String)
}
