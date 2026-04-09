package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.GitTag
import jamgmilk.fuwagit.domain.model.git.GitTagType
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTag
import org.eclipse.jgit.revwalk.RevWalk
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Git tag operations.
 */
@Singleton
class JGitTagDataSource @Inject constructor(
    private val core: GitCoreDataSource
) : GitTagDataSource {

    /**
     * Lists all tags in the repository.
     */
    override fun getTags(repoPath: String): Result<List<GitTag>> =
        core.withGit(repoPath) { git ->
            val repository = git.repository
            val refList = repository.getRefDatabase().getRefsByPrefix("refs/tags/")
            val tags = mutableListOf<GitTag>()

            for (ref in refList) {
                val tagName = ref.name.removePrefix("refs/tags/")
                val peeledRef = repository.refDatabase.peel(ref)
                val targetObjectId = peeledRef.peeledObjectId ?: ref.objectId

                val tagType = determineTagType(repository, ref, peeledRef)
                val tagInfo = extractTagInfo(repository, ref, tagType)

                tags.add(
                    GitTag(
                        name = tagName,
                        fullRef = ref.name,
                        type = tagType,
                        targetHash = targetObjectId.name,
                        taggerName = tagInfo.taggerName,
                        taggerEmail = tagInfo.taggerEmail,
                        message = tagInfo.message,
                        timestamp = tagInfo.timestamp
                    )
                )
            }

            // Sort by timestamp descending (if timestamp available)
            tags.sortWith(compareByDescending<GitTag> { it.timestamp ?: 0L })
            tags
        }

    /**
     * Creates a lightweight tag.
     */
    override fun createLightweightTag(
        repoPath: String,
        tagName: String,
        commitHash: String?
    ): Result<String> = core.withGit(repoPath) { git ->
        val repository = git.repository
        val targetObject = if (commitHash != null) {
            repository.resolve(commitHash)
        } else {
            repository.resolve("HEAD")
        }

        require(targetObject != null) { "Cannot resolve target commit hash" }

        // Check if tag already exists
        val existingRef = repository.findRef("refs/tags/$tagName")
        require(existingRef == null) { "Tag '$tagName' already exists" }

        // Create tag - use tag command instead of directly manipulating RefUpdate
        RevWalk(repository).use { revWalk ->
            val revObject = revWalk.parseAny(targetObject)
            git.tag()
                .setName(tagName)
                .setObjectId(revObject)
                .call()
        }

        "Lightweight tag '$tagName' created successfully"
    }

    /**
     * Creates an annotated tag.
     */
    override fun createAnnotatedTag(
        repoPath: String,
        tagName: String,
        message: String,
        commitHash: String?
    ): Result<String> = core.withGit(repoPath) { git ->
        val repository = git.repository
        val targetCommit = if (commitHash != null) {
            repository.resolve(commitHash)
        } else {
            repository.resolve("HEAD")
        }

        require(targetCommit != null) { "Cannot resolve target commit hash" }

        // Check if tag already exists
        val existingRef = repository.findRef("refs/tags/$tagName")
        require(existingRef == null) { "Tag '$tagName' already exists" }

        // Use JGit tag command to create annotated tag
        RevWalk(repository).use { revWalk ->
            val revObject = revWalk.parseAny(targetCommit)
            git.tag()
                .setName(tagName)
                .setMessage(message)
                .setObjectId(revObject)
                .call()
        }

        "Annotated tag '$tagName' created successfully"
    }

    /**
     * Deletes a tag.
     */
    override fun deleteTag(repoPath: String, tagName: String): Result<Unit> =
        core.withGit(repoPath) { git ->
            val repository = git.repository
            val ref = repository.findRef("refs/tags/$tagName")
            require(ref != null) { "Tag '$tagName' not found" }

            // Delete tag reference
            git.tagDelete().setTags(tagName).call()

            Unit
        }

    /**
     * Pushes a specific tag to remote.
     */
    override fun pushTag(repoPath: String, tagName: String, remoteName: String): Result<String> =
        core.withGit(repoPath) { git ->
            val repository = git.repository
            val ref = repository.findRef("refs/tags/$tagName")
            require(ref != null) { "Tag '$tagName' not found" }

            val pushCommand = git.push()
                .setRemote(remoteName)
                .add("refs/tags/$tagName")

            pushCommand.call()
            "Tag '$tagName' pushed to $remoteName successfully"
        }

    /**
     * Pushes all tags to remote.
     */
    override fun pushAllTags(repoPath: String, remoteName: String): Result<String> =
        core.withGit(repoPath) { git ->
            val pushCommand = git.push()
                .setRemote(remoteName)
                .setPushTags()

            pushCommand.call()
            "All tags pushed to $remoteName successfully"
        }

    /**
     * Checks out a tag (creates detached HEAD state).
     */
    override fun checkoutTag(repoPath: String, tagName: String): Result<String> =
        core.withGit(repoPath) { git ->
            val repository = git.repository
            val ref = repository.findRef("refs/tags/$tagName")
            require(ref != null) { "Tag '$tagName' not found" }

            // Checkout tag (will enter detached HEAD state)
            git.checkout()
                .setName(tagName)
                .call()

            "Checked out tag '$tagName' (detached HEAD state)"
        }

    // ==================== Private helper methods ====================

    /**
     * Determine tag type (lightweight or annotated)
     */
    private fun determineTagType(
        repository: Repository,
        ref: Ref,
        peeledRef: Ref
    ): GitTagType {
        // If peeledObjectId is different from ref.objectId, it's an annotated tag
        return if (peeledRef.peeledObjectId != null && peeledRef.peeledObjectId != ref.objectId) {
            GitTagType.Annotated
        } else {
            GitTagType.Lightweight
        }
    }

    /**
     * Extract tag information (only annotated tags have full info)
     */
    private fun extractTagInfo(
        repository: Repository,
        ref: Ref,
        tagType: GitTagType
    ): TagInfo {
        if (tagType == GitTagType.Lightweight) {
            return TagInfo()
        }

        return try {
            RevWalk(repository).use { revWalk ->
                val obj = revWalk.parseAny(ref.objectId)
                if (obj is RevTag) {
                    TagInfo(
                        taggerName = obj.taggerIdent?.name,
                        taggerEmail = obj.taggerIdent?.emailAddress,
                        message = obj.shortMessage,
                        timestamp = obj.taggerIdent?.`when`?.time
                    )
                } else {
                    TagInfo()
                }
            }
        } catch (e: Exception) {
            TagInfo()
        }
    }

    /**
     * Tag information data class
     */
    private data class TagInfo(
        val taggerName: String? = null,
        val taggerEmail: String? = null,
        val message: String? = null,
        val timestamp: Long? = null
    )
}
