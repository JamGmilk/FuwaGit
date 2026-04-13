package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.data.local.prefs.AppPreferencesStore
import jamgmilk.fuwagit.data.local.prefs.GitConfigDataStore
import jamgmilk.fuwagit.domain.model.AppPreferences
import jamgmilk.fuwagit.domain.repository.GitConfigSettings
import jamgmilk.fuwagit.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val gitConfigDataStore: GitConfigDataStore,
    private val appPreferencesStore: AppPreferencesStore
) : SettingsRepository {

    override fun gitConfigFlow(): Flow<GitConfigSettings> {
        return gitConfigDataStore.configFlow.map { config ->
            GitConfigSettings(
                userName = config.userName,
                userEmail = config.userEmail,
                defaultBranch = config.defaultBranch,
                setUpstreamOnPush = config.setUpstreamOnPush
            )
        }
    }

    override fun preferencesFlow(): Flow<AppPreferences> {
        return appPreferencesStore.preferencesFlow.map { prefs ->
            AppPreferences(
                autoSync = prefs.autoSync,
                conflictSafeMode = prefs.conflictSafeMode,
                backupBeforeSync = prefs.backupBeforeSync,
                verboseLogging = prefs.verboseLogging,
                darkMode = prefs.darkMode,
                language = prefs.language,
                autoLockTimeout = prefs.autoLockTimeout,
                isFirstRun = prefs.isFirstRun
            )
        }
    }

    override suspend fun saveUserConfig(name: String, email: String) {
        gitConfigDataStore.setUserConfig(name, email)
    }

    override suspend fun saveDefaultBranch(branch: String) {
        gitConfigDataStore.setDefaultBranch(branch)
    }

    override suspend fun saveSetUpstreamOnPush(enabled: Boolean) {
        gitConfigDataStore.setSetUpstreamOnPush(enabled)
    }

    override suspend fun saveAutoSync(enabled: Boolean) {
        appPreferencesStore.setAutoSync(enabled)
    }

    override suspend fun saveConflictSafeMode(enabled: Boolean) {
        appPreferencesStore.setConflictSafeMode(enabled)
    }

    override suspend fun saveBackupBeforeSync(enabled: Boolean) {
        appPreferencesStore.setBackupBeforeSync(enabled)
    }

    override suspend fun saveVerboseLogging(enabled: Boolean) {
        appPreferencesStore.setVerboseLogging(enabled)
    }

    override suspend fun saveDarkMode(mode: String) {
        appPreferencesStore.setDarkMode(mode)
    }

    override suspend fun saveLanguage(language: String) {
        appPreferencesStore.setLanguage(language)
    }

    override suspend fun saveAutoLockTimeout(timeout: String) {
        appPreferencesStore.setAutoLockTimeout(timeout)
    }

    override suspend fun setFirstRunCompleted() {
        appPreferencesStore.setFirstRunCompleted()
    }

    override suspend fun resetFirstRun() {
        appPreferencesStore.resetFirstRun()
    }
}
