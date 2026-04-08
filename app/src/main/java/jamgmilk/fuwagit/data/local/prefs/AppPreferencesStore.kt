package jamgmilk.fuwagit.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore instance - must be at top level outside class
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

// Preference Keys
object PreferencesKeys {
    val AUTO_SYNC = booleanPreferencesKey("sync.auto")
    val CONFLICT_SAFE_MODE = booleanPreferencesKey("sync.conflictSafeMode")
    val BACKUP_BEFORE_SYNC = booleanPreferencesKey("sync.backupBeforeSync")
    val VERBOSE_LOGGING = booleanPreferencesKey("developer.verboseLogging")
    val DARK_MODE = stringPreferencesKey("appearance.darkMode")
    val LANGUAGE = stringPreferencesKey("appearance.language")
    val AUTO_LOCK_TIMEOUT = stringPreferencesKey("security.autoLockTimeout")
    val IS_FIRST_RUN = booleanPreferencesKey("app.isFirstRun")
}

// App preferences data class (for type-safe access)
data class AppPreferences(
    val autoSync: Boolean = false,
    val conflictSafeMode: Boolean = true,
    val backupBeforeSync: Boolean = true,
    val verboseLogging: Boolean = false,
    val darkMode: String = "system",
    val language: String = "system",
    val autoLockTimeout: String = "300",
    val isFirstRun: Boolean = true
)

@Singleton
class AppPreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Expose preferences as Flow<AppPreferences>
    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            autoSync = prefs[PreferencesKeys.AUTO_SYNC] ?: false,
            conflictSafeMode = prefs[PreferencesKeys.CONFLICT_SAFE_MODE] ?: true,
            backupBeforeSync = prefs[PreferencesKeys.BACKUP_BEFORE_SYNC] ?: true,
            verboseLogging = prefs[PreferencesKeys.VERBOSE_LOGGING] ?: false,
            darkMode = prefs[PreferencesKeys.DARK_MODE] ?: "system",
            language = prefs[PreferencesKeys.LANGUAGE] ?: "system",
            autoLockTimeout = prefs[PreferencesKeys.AUTO_LOCK_TIMEOUT] ?: "300",
            isFirstRun = prefs[PreferencesKeys.IS_FIRST_RUN] ?: true
        )
    }

    suspend fun setAutoLockTimeout(timeout: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_LOCK_TIMEOUT] = timeout
        }
    }

    suspend fun setAutoSync(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AUTO_SYNC] = enabled
        }
    }

    suspend fun setConflictSafeMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CONFLICT_SAFE_MODE] = enabled
        }
    }

    suspend fun setBackupBeforeSync(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.BACKUP_BEFORE_SYNC] = enabled
        }
    }

    suspend fun setVerboseLogging(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.VERBOSE_LOGGING] = enabled
        }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DARK_MODE] = mode
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LANGUAGE] = language
        }
    }

    suspend fun setFirstRunCompleted() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_FIRST_RUN] = false
        }
    }

    suspend fun resetFirstRun() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_FIRST_RUN] = true
        }
    }
}

