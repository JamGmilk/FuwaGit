package jamgmilk.fuwagit.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore instance - must be at top level outside class
private val Context.gitConfigDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "git_config"
)

// Preference Keys
object GitConfigKeys {
    val USER_NAME = stringPreferencesKey("user.name")
    val USER_EMAIL = stringPreferencesKey("user.email")
    val DEFAULT_BRANCH = stringPreferencesKey("init.defaultBranch")
}

// Git config data class (for type-safe access)
data class GitConfig(
    val userName: String = "",
    val userEmail: String = "",
    val defaultBranch: String = "main"
)

@Singleton
class GitConfigDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Expose config as Flow<GitConfig>
    val configFlow: Flow<GitConfig> = context.gitConfigDataStore.data.map { prefs ->
        GitConfig(
            userName = prefs[GitConfigKeys.USER_NAME] ?: "",
            userEmail = prefs[GitConfigKeys.USER_EMAIL] ?: "",
            defaultBranch = prefs[GitConfigKeys.DEFAULT_BRANCH] ?: "main"
        )
    }

    suspend fun setUserConfig(name: String, email: String) {
        context.gitConfigDataStore.edit { prefs ->
            prefs[GitConfigKeys.USER_NAME] = name
            prefs[GitConfigKeys.USER_EMAIL] = email
        }
    }

    suspend fun setDefaultBranch(branch: String) {
        context.gitConfigDataStore.edit { prefs ->
            prefs[GitConfigKeys.DEFAULT_BRANCH] = branch
        }
    }
}
