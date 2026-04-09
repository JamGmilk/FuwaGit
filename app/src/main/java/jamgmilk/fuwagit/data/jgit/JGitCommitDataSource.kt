package jamgmilk.fuwagit.data.jgit

import android.util.Log
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import jamgmilk.fuwagit.domain.model.git.GitCommitFileChange
import jamgmilk.fuwagit.domain.model.git.GitResetMode
import kotlinx.coroutines.flow.first
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.LockFailedException
import org.eclipse.jgit.api.errors.JGitInternalException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Git commit and history operations.
 */
@Singleton
class JGitCommitDataSource @Inject constructor(
    private val core: GitCoreDataSource
) : GitCommitDataSource {
    /**
     * Gets the commit log.
     */
    override fun getLog(repoPath: String, maxCount: Int): Result<List<GitCommit>> =
        core.withGit(repoPath) { git ->
            git.log().setMaxCount(maxCount).call().map { revCommit ->
                val author = revCommit.authorIdent
                GitCommit(
                    hash = revCommit.id.name(),
                    shortHash = revCommit.id.abbreviate(7).name(),
                    message = revCommit.fullMessage,
                    authorName = author.name,
                    authorEmail = author.emailAddress,
                    timestamp = author.`when`.time,
                    parentHashes = revCommit.parents.map { it.name() }
                )
            }
        }

    /**
     * Gets detailed information about a specific commit including file changes.
     */
    override fun getCommitFileChanges(repoPath: String, commitHash: String): Result<GitCommitDetail> =
        core.withGit(repoPath) { git ->
            val repository = git.repository
            val objectId = repository.resolve(commitHash)
                ?: throw Exception("Commit not found: $commitHash")

            val revCommit = repository.parseCommit(objectId)
            val fileChanges = mutableListOf<GitCommitFileChange>()
            var totalAdditions = 0
            var totalDeletions = 0

            try {
                val revWalk = org.eclipse.jgit.revwalk.RevWalk(repository)
                try {
                    val commit = revWalk.parseCommit(objectId)
                    val tree = commit.tree
                    val parentCommit = if (commit.parentCount > 0) {
                        revWalk.parseCommit(commit.getParent(0).id)
                    } else {
                        null
                    }
                    val parentTree = parentCommit?.tree

                    if (parentTree != null) {
                        val outputStream = java.io.ByteArrayOutputStream()
                        val diffFormatter = org.eclipse.jgit.diff.DiffFormatter(outputStream)
                        try {
                            diffFormatter.setRepository(repository)
                            val diffEntries = diffFormatter.scan(parentTree.id, tree.id)
                            for (diffEntry in diffEntries) {
                                val changeType = when (diffEntry.changeType.name) {
                                    "ADD" -> GitChangeType.Added
                                    "DELETE" -> GitChangeType.Removed
                                    "MODIFY" -> GitChangeType.Modified
                                    "RENAME" -> GitChangeType.Renamed
                                    else -> GitChangeType.Modified
                                }
                                val path = diffEntry.newPath.ifBlank { diffEntry.oldPath }

                                // Calculate line changes - simplified: compare file size and content directly
                                var additions = 0
                                var deletions = 0
                                try {
                                    if (changeType == GitChangeType.Added && diffEntry.newId != null) {
                                        val newLoader = repository.open(diffEntry.newId.toObjectId())
                                        additions = String(newLoader.bytes).lines().size
                                    } else if (changeType == GitChangeType.Removed && diffEntry.oldId != null) {
                                        val oldLoader = repository.open(diffEntry.oldId.toObjectId())
                                        deletions = String(oldLoader.bytes).lines().size
                                    } else if (changeType == GitChangeType.Modified) {
                                        // For modifications, try to calculate the diff
                                        if (diffEntry.newId != null && diffEntry.oldId != null) {
                                            val newLoader = repository.open(diffEntry.newId.toObjectId())
                                            val oldLoader = repository.open(diffEntry.oldId.toObjectId())
                                            val newLines = String(newLoader.bytes).lines().size
                                            val oldLines = String(oldLoader.bytes).lines().size
                                            additions = maxOf(0, newLines - oldLines)
                                            deletions = maxOf(0, oldLines - newLines)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // If failed, use default values
                                }

                                fileChanges.add(
                                    GitCommitFileChange(
                                        path = path,
                                        name = java.io.File(path).name,
                                        changeType = changeType,
                                        additions = additions,
                                        deletions = deletions
                                    )
                                )
                                totalAdditions += additions
                                totalDeletions += deletions
                            }
                        } finally {
                            diffFormatter.close()
                        }
                    } else {
                        // Initial commit - all files are added
                        val walk = org.eclipse.jgit.treewalk.TreeWalk(repository)
                        try {
                            walk.addTree(tree)
                            walk.isRecursive = true
                            while (walk.next()) {
                                val path = walk.pathString
                                try {
                                    val objectId = walk.getObjectId(0)
                                    val loader = repository.open(objectId)
                                    val lineCount = String(loader.bytes).lines().size
                                    totalAdditions += lineCount

                                    fileChanges.add(
                                        GitCommitFileChange(
                                            path = path,
                                            name = java.io.File(path).name,
                                            changeType = GitChangeType.Added,
                                            additions = lineCount,
                                            deletions = 0
                                        )
                                    )
                                } catch (e: Exception) {
                                    fileChanges.add(
                                        GitCommitFileChange(
                                            path = path,
                                            name = java.io.File(path).name,
                                            changeType = GitChangeType.Added,
                                            additions = 0,
                                            deletions = 0
                                        )
                                    )
                                }
                            }
                        } finally {
                            walk.close()
                        }
                    }
                } finally {
                    revWalk.dispose()
                }
            } catch (e: Exception) {
                Log.w("JGitCommitDataSource", "Failed to get commit file changes: ${e.message}")
            }

            val author = revCommit.authorIdent
            val commit = GitCommit(
                hash = revCommit.id.name(),
                shortHash = revCommit.id.abbreviate(7).name(),
                message = revCommit.fullMessage,
                authorName = author.name,
                authorEmail = author.emailAddress,
                timestamp = author.`when`.time,
                parentHashes = revCommit.parents.map { it.name() }
            )

            GitCommitDetail(
                commit = commit,
                fileChanges = fileChanges,
                totalAdditions = totalAdditions,
                totalDeletions = totalDeletions,
                totalFiles = fileChanges.size
            )
        }

    /**
     * Creates a commit with the given message.
     */
    override suspend fun commit(repoPath: String, message: String): Result<String> {
        return try {
            val config = core.gitConfigDataStore.configFlow.first()
            core.withGit(repoPath) { git ->
                val storedConfig = git.repository.config
                if (config.userName.isNotBlank() || config.userEmail.isNotBlank()) {
                    if (config.userName.isNotBlank()) {
                        storedConfig.setString("user", null, "name", config.userName)
                    }
                    if (config.userEmail.isNotBlank()) {
                        storedConfig.setString("user", null, "email", config.userEmail)
                    }
                    storedConfig.save()
                }

                val commit = git.commit()
                    .setMessage(message)
                    .setAllowEmpty(false)
                    .call()
                commit.id.name()
            }
        } catch (e: org.eclipse.jgit.errors.LockFailedException) {
            Result.failure(Exception("Cannot commit: repository lock failed."))
        } catch (e: org.eclipse.jgit.api.errors.JGitInternalException) {
            Result.failure(Exception("Git error: ${e.message}"))
        }
    }

    /**
     * Resets the repository to a specific commit.
     */
    override fun reset(repoPath: String, commitHash: String, mode: GitResetMode): Result<String> =
        core.withGit(repoPath) { git ->
            val resetCommand = git.reset().setRef(commitHash)
            when (mode) {
                GitResetMode.SOFT -> resetCommand.setMode(org.eclipse.jgit.api.ResetCommand.ResetType.SOFT)
                GitResetMode.MIXED -> resetCommand.setMode(org.eclipse.jgit.api.ResetCommand.ResetType.MIXED)
                GitResetMode.HARD -> resetCommand.setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
            }
            resetCommand.call()

            when (mode) {
                GitResetMode.SOFT -> "Reset to $commitHash (soft): HEAD moved, changes kept staged"
                GitResetMode.MIXED -> "Reset to $commitHash (mixed): HEAD moved, changes unstaged"
                GitResetMode.HARD -> "Reset to $commitHash (hard): All changes discarded"
            }
        }
}
