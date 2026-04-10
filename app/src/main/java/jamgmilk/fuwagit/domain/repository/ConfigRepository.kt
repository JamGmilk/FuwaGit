package jamgmilk.fuwagit.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for Git configuration operations.
 * Extracted to prevent Domain → Data layer dependency violations.
 */
interface ConfigRepository {
    /**
     * Get global Git user name.
     */
    suspend fun getGlobalUserName(): String?

    /**
     * Get global Git user email.
     */
    suspend fun getGlobalUserEmail(): String?

    /**
     * Set global Git user name and email.
     */
    suspend fun setGlobalUserConfig(name: String, email: String): Result<Unit>

    /**
     * Set repo-level username and email.
     */
    fun setRepoUserConfig(repoPath: String, name: String, email: String): Result<Unit>

    /**
     * Remove repo-level local user config (fall back to global).
     */
    fun removeRepoUserConfig(repoPath: String): Result<Unit>

    /**
     * Get effective user config for a repo (local if set, otherwise global).
     */
    suspend fun getEffectiveUserConfig(repoPath: String): Pair<String?, String?>
}
