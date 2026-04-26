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

    override fun getLog(repoPath: String, maxCount: Int, skip: Int): Result<List<GitCommit>> =
        core.withGit(repoPath) { git ->
            git.repository.resolve("HEAD") ?: return@withGit emptyList()
            git.log().setMaxCount(maxCount).setSkip(skip).call().map { revCommit ->
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

            val fileChanges = mutableListOf<GitCommitFileChange>()
            var totalAdditions = 0
            var totalDeletions = 0

            org.eclipse.jgit.revwalk.RevWalk(repository).use { revWalk ->
                val commit = revWalk.parseCommit(objectId)
                val tree = commit.tree
                val parentCommit = if (commit.parentCount > 0) {
                    revWalk.parseCommit(commit.getParent(0).id)
                } else {
                    null
                }
                val parentTree = parentCommit?.tree

                if (parentTree != null) {
                    org.eclipse.jgit.diff.DiffFormatter(org.eclipse.jgit.util.io.NullOutputStream.INSTANCE).use { diffFormatter ->
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
                            val fileHeader = diffFormatter.toFileHeader(diffEntry)
                            for (edit in fileHeader.toEditList()) {
                                additions += edit.lengthB - edit.beginB
                                deletions += edit.lengthA - edit.beginA
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
                    }
                } else {
                    org.eclipse.jgit.treewalk.TreeWalk(repository).use { walk ->
                        walk.addTree(tree)
                        walk.isRecursive = true
                        while (walk.next()) {
                            val path = walk.pathString
                            val objectId = walk.getObjectId(0)
                            val loader = repository.open(objectId)
                            val lineCount = countLines(loader)
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
                    }
                }

                GitCommitDetail(
                    fileChanges = fileChanges,
                    totalAdditions = totalAdditions,
                    totalDeletions = totalDeletions,
                    totalFiles = fileChanges.size
                )
            }
        }

    override suspend fun commit(repoPath: String, message: String): Result<String> {
        val config = core.gitConfigDataStore.configFlow.first()

        return core.withGit(repoPath) { git ->
            val status = git.status().call()
            val hasStagedChanges = status.added.isNotEmpty() ||
                                   status.changed.isNotEmpty() ||
                                   status.removed.isNotEmpty()

            if (!hasStagedChanges) {
                throw Exception("Nothing to commit")
            }

            val authorName = config.userName.takeIf { it.isNotBlank() }
            val authorEmail = config.userEmail.takeIf { it.isNotBlank() }

            val commitBuilder = git.commit()
                .setMessage(message)
                .setAllowEmpty(false)

            if (authorName != null && authorEmail != null) {
                commitBuilder.setAuthor(authorName, authorEmail)
            }

            commitBuilder.call().id.name()
        }.let { result ->
            if (result.isFailure) {
                val exception = result.exceptionOrNull()
                when {
                    exception is LockFailedException -> Result.failure(Exception("Cannot commit: repository lock failed."))
                    exception is JGitInternalException -> Result.failure(Exception("Git error: ${exception.message}"))
                    exception is Exception -> Result.failure(exception)
                    else -> result
                }
            } else {
                result
            }
        }
    }

    override fun reset(repoPath: String, commitHash: String, mode: GitResetMode): Result<String> {
        return try {
            core.withGit(repoPath) { git ->
                git.repository.resolve(commitHash)
                    ?: throw Exception("Commit not found: $commitHash")

                val status = git.status().call()
                val hasUncommittedChanges = status.hasUncommittedChanges()

                if (mode == GitResetMode.HARD && hasUncommittedChanges) {
                    val workingDirectoryChanges = status.modified.isNotEmpty() ||
                        status.added.isNotEmpty() ||
                        status.removed.isNotEmpty() ||
                        status.changed.isNotEmpty()

                    if (workingDirectoryChanges) {
                        val stashResult = try {
                            git.stashCreate()
                                .setIncludeUntracked(true)
                                .call()
                        } catch (_: Exception) {
                            null
                        }

                        if (stashResult != null) {
                            val resetCommand = git.reset().setRef(commitHash)
                                .setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
                            resetCommand.call()
                            return@withGit "Reset to $commitHash (hard): All changes auto-stashed (stash@{0} = ${stashResult.name}). Use 'git stash pop' to restore."
                        } else {
                            throw Exception(
                                "Cannot reset: there are uncommitted changes that could not be auto-stashed. " +
                                "Please commit or stash your changes before performing a hard reset."
                            )
                        }
                    }
                }

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
        } catch (_: LockFailedException) {
            Result.failure(Exception("Cannot reset: repository lock failed."))
        } catch (e: JGitInternalException) {
            Result.failure(Exception("Git error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Reset failed: ${e.message}"))
        }
    }

    private fun countLines(loader: org.eclipse.jgit.lib.ObjectLoader): Int {
        loader.openStream().buffered().use { stream ->
            var count = 0
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                for (i in 0 until bytesRead) {
                    if (buffer[i] == 10.toByte()) count++
                }
            }
            return count
        }
    }
}
