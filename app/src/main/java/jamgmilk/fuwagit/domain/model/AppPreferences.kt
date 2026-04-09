package jamgmilk.fuwagit.domain.model

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
