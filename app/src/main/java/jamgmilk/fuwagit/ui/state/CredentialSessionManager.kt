package jamgmilk.fuwagit.ui.state

import jamgmilk.fuwagit.data.local.prefs.AppPreferencesStore
import jamgmilk.fuwagit.data.local.security.MasterKeyManager
import jamgmilk.fuwagit.data.local.security.SecureCredentialStore
import jamgmilk.fuwagit.data.local.security.VaultStateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class CredentialSessionState(
    val isUnlocked: Boolean = false,
    val isMasterPasswordSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val passwordHint: String? = null,
    val showUnlockDialog: Boolean = false
)

@Singleton
class CredentialSessionManager @Inject constructor(
    private val secureCredentialStore: SecureCredentialStore,
    private val masterKeyManager: MasterKeyManager,
    private val appPreferencesStore: AppPreferencesStore
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var cachedIsMasterPasswordSet: Boolean = false
    private var cachedIsBiometricEnabled: Boolean = false
    private var cachedPasswordHint: String? = null

    private val _sessionState = MutableStateFlow(CredentialSessionState())
    val sessionState: StateFlow<CredentialSessionState> = _sessionState.asStateFlow()

    private var autoLockJob: Job? = null

    init {
        loadCachedConfig()
        refreshSessionState()
        observeVaultState()
    }

    private fun observeVaultState() {
        scope.launch {
            secureCredentialStore.vaultStateEvents.collect { event ->
                when (event) {
                    is VaultStateEvent.Unlocked -> onVaultUnlocked()
                    is VaultStateEvent.Locked -> onVaultLocked()
                }
            }
        }
    }

    private fun loadCachedConfig() {
        cachedIsMasterPasswordSet = masterKeyManager.isMasterPasswordSet()
        cachedIsBiometricEnabled = masterKeyManager.isBiometricEnabled()
        cachedPasswordHint = masterKeyManager.getPasswordHint()
    }

    fun refreshSessionState() {
        scope.launch {
            val isUnlocked = isVaultUnlockedInternal()
            _sessionState.value = _sessionState.value.copy(
                isUnlocked = isUnlocked,
                isMasterPasswordSet = cachedIsMasterPasswordSet,
                isBiometricEnabled = cachedIsBiometricEnabled,
                passwordHint = cachedPasswordHint
            )
        }
    }

    fun reloadConfig() {
        loadCachedConfig()
        refreshSessionState()
    }

    private suspend fun getSessionTimeoutMillis(): Long {
        val timeoutSeconds = appPreferencesStore.preferencesFlow
            .first { true }
            .autoLockTimeout
            .toLongOrNull() ?: 600L

        return when {
            timeoutSeconds < 0 -> 0L
            timeoutSeconds == 0L -> 0L
            timeoutSeconds < 30 -> 30L
            timeoutSeconds > 86400 -> 86400L
            else -> timeoutSeconds
        } * 1000L
    }

    private suspend fun isVaultUnlockedInternal(): Boolean {
        val timeout = getSessionTimeoutMillis()
        return secureCredentialStore.getCachedMasterKey(timeout) != null
    }

    @Suppress("UNUSED")
    suspend fun isVaultUnlocked(): Boolean {
        return isVaultUnlockedInternal()
    }

    private fun onVaultUnlocked() {
        _sessionState.value = _sessionState.value.copy(
            showUnlockDialog = false,
            isUnlocked = true
        )
        startAutoLockTimer()
    }

    private fun onVaultLocked() {
        cancelAutoLockTimer()
        _sessionState.value = _sessionState.value.copy(
            showUnlockDialog = false,
            isUnlocked = false
        )
    }

    private fun startAutoLockTimer() {
        cancelAutoLockTimer()
        scope.launch {
            val timeout = getSessionTimeoutMillis()
            if (timeout > 0) {
                autoLockJob = scope.launch {
                    delay(timeout)
                    secureCredentialStore.clearCachedMasterKey()
                }
            }
        }
    }

    private fun cancelAutoLockTimer() {
        autoLockJob?.cancel()
        autoLockJob = null
    }

    fun showUnlockDialog() {
        _sessionState.value = _sessionState.value.copy(showUnlockDialog = true)
    }

    fun dismissUnlockDialog() {
        _sessionState.value = _sessionState.value.copy(showUnlockDialog = false)
    }
}