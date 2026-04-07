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

            // 按时间戳降序排序（如果有时间戳的话）
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

        // 检查标签是否已存在
        val existingRef = repository.findRef("refs/tags/$tagName")
        require(existingRef == null) { "Tag '$tagName' already exists" }

        // 创建标签 - 使用 tag 命令而不是直接操作 RefUpdate
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

        // 检查标签是否已存在
        val existingRef = repository.findRef("refs/tags/$tagName")
        require(existingRef == null) { "Tag '$tagName' already exists" }

        // 使用 JGit 的 tag 命令创建附注标签
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

            // 删除标签引用
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

            // Checkout tag（会进入 detached HEAD 状态）
            git.checkout()
                .setName(tagName)
                .call()

            "Checked out tag '$tagName' (detached HEAD state)"
        }

    // ==================== 私有辅助方法 ====================

    /**
     * 确定标签类型（轻量或附注）
     */
    private fun determineTagType(
        repository: Repository,
        ref: Ref,
        peeledRef: Ref
    ): GitTagType {
        // 如果 peeledObjectId 与 ref.objectId 不同，说明是附注标签
        return if (peeledRef.peeledObjectId != null && peeledRef.peeledObjectId != ref.objectId) {
            GitTagType.Annotated
        } else {
            GitTagType.Lightweight
        }
    }

    /**
     * 提取标签信息（仅附注标签有完整信息）
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
     * 标签信息数据类
     */
    private data class TagInfo(
        val taggerName: String? = null,
        val taggerEmail: String? = null,
        val message: String? = null,
        val timestamp: Long? = null
    )
}
