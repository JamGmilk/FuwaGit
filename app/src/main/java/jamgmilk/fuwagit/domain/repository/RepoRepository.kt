package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.domain.model.repo.RepoData
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for repository listing and management.
 * Extracted to prevent Domain → Data layer dependency violations.
 */
interface RepoRepository {
    /**
     * Get the list of saved repositories as a Flow.
     */
    fun getSavedReposFlow(): Flow<List<RepoData>>

    /**
     * Get the list of saved repositories (one-time).
     */
    suspend fun getAllRepos(): List<RepoData>

    /**
     * Set the current active repository path.
     */
    suspend fun setCurrentRepo(path: String?)

    /**
     * Get the current active repository path.
     */
    suspend fun getCurrentRepoPath(): String?

    /**
     * Add a repository to the list.
     */
    suspend fun addRepo(repo: RepoData): Boolean

    /**
     * Remove a repository from the list.
     */
    suspend fun removeRepo(path: String): Boolean

    /**
     * Update the last accessed timestamp for a repository.
     */
    suspend fun updateLastAccessed(path: String)

    /**
     * Toggle favorite status of a repository.
     */
    suspend fun toggleFavorite(path: String)
}
