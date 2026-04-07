package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.GitTag

/**
 * Data source interface for Git tag operations.
 */
interface GitTagDataSource {
    /**
     * Lists all tags in the repository.
     */
    fun getTags(repoPath: String): Result<List<GitTag>>

    /**
     * Creates a lightweight tag.
     */
    fun createLightweightTag(repoPath: String, tagName: String, commitHash: String? = null): Result<String>

    /**
     * Creates an annotated tag.
     */
    fun createAnnotatedTag(
        repoPath: String,
        tagName: String,
        message: String,
        commitHash: String? = null
    ): Result<String>

    /**
     * Deletes a tag.
     */
    fun deleteTag(repoPath: String, tagName: String): Result<Unit>

    /**
     * Pushes a specific tag to remote.
     */
    fun pushTag(repoPath: String, tagName: String, remoteName: String = "origin"): Result<String>

    /**
     * Pushes all tags to remote.
     */
    fun pushAllTags(repoPath: String, remoteName: String = "origin"): Result<String>

    /**
     * Checks out a tag (creates detached HEAD state).
     */
    fun checkoutTag(repoPath: String, tagName: String): Result<String>
}
