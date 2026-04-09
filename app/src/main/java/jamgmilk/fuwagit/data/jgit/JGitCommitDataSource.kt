package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import jamgmilk.fuwagit.domain.model.git.GitCommitFileChange
import jamgmilk.fuwagit.domain.model.git.GitResetMode
import kotlinx.coroutines.flow.first
import org.eclipse.jgit.errors.LockFailedException
import org.eclipse.jgit.api.errors.JGitInternalException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JGitCommitDataSource @Inject constructor(
    private val core: GitCoreDataSource
) : GitCommitDataSource {

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

    override fun getCommitFileChanges(repoPath: String, commitHash: String): Result<GitCommitDetail> =
        core.withGit(repoPath) { git ->
            val repository = git.repository
            val objectId = repository.resolve(commitHash)
                ?: throw Exception("Commit not found: $commitHash")

            val revCommit = repository.parseCommit(objectId)
            val fileChanges = mutableListOf<GitCommitFileChange>()
            var totalAdditions = 0
            var totalDeletions = 0

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

                            var additions = 0
                            var deletions = 0
                            when (changeType) {
                                GitChangeType.Added -> {
                                    val newLoader = repository.open(diffEntry.newId.toObjectId())
                                    additions = String(newLoader.bytes).lines().size
                                }
                                GitChangeType.Removed -> {
                                    val oldLoader = repository.open(diffEntry.oldId.toObjectId())
                                    deletions = String(oldLoader.bytes).lines().size
                                }
                                GitChangeType.Modified -> {
                                    val newLoader = repository.open(diffEntry.newId.toObjectId())
                                    val oldLoader = repository.open(diffEntry.oldId.toObjectId())
                                    val newLines = String(newLoader.bytes).lines().size
                                    val oldLines = String(oldLoader.bytes).lines().size
                                    additions = maxOf(0, newLines - oldLines)
                                    deletions = maxOf(0, oldLines - newLines)
                                }
                                GitChangeType.Renamed,
                                GitChangeType.Untracked,
                                GitChangeType.Conflicting -> {
                                }
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
                    val walk = org.eclipse.jgit.treewalk.TreeWalk(repository)
                    try {
                        walk.addTree(tree)
                        walk.isRecursive = true
                        while (walk.next()) {
                            val path = walk.pathString
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
                        }
                    } finally {
                        walk.close()
                    }
                }
            } finally {
                revWalk.dispose()
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
        } catch (e: LockFailedException) {
            Result.failure(Exception("Cannot commit: repository lock failed."))
        } catch (e: JGitInternalException) {
            Result.failure(Exception("Git error: ${e.message}"))
        }
    }

    override fun reset(repoPath: String, commitHash: String, mode: GitResetMode): Result<String> {
        return try {
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
        } catch (e: LockFailedException) {
            Result.failure(Exception("Cannot reset: repository lock failed."))
        } catch (e: JGitInternalException) {
            Result.failure(Exception("Git error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Reset failed: ${e.message}"))
        }
    }
}
