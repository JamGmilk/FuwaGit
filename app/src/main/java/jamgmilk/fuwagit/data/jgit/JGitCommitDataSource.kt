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
        val statusResult = core.withGit(repoPath) { git ->
            val status = git.status().call()
            status.added.isNotEmpty() || status.changed.isNotEmpty() || status.removed.isNotEmpty()
        }

        if (statusResult.isFailure) {
            return Result.failure(statusResult.exceptionOrNull() ?: Exception("Failed to check status"))
        }
        val hasStagedChanges = statusResult.getOrNull() == true

        if (!hasStagedChanges) {
            return Result.failure(Exception("Nothing to commit"))
        }

        return try {
            val config = core.gitConfigDataStore.configFlow.first()
            core.withGit(repoPath) { git ->
                val authorName = config.userName.takeIf { it.isNotBlank() }
                val authorEmail = config.userEmail.takeIf { it.isNotBlank() }

                val commitBuilder = git.commit()
                    .setMessage(message)
                    .setAllowEmpty(false)

                if (authorName != null && authorEmail != null) {
                    commitBuilder.setAuthor(authorName, authorEmail)
                }

                commitBuilder.call().id.name()
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
                // 验证 commitHash 是否存在
                git.repository.resolve(commitHash)
                    ?: throw Exception("Commit not found: $commitHash")

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

    private fun countLines(loader: org.eclipse.jgit.lib.ObjectLoader): Int {
        loader.openStream().use { stream ->
            var count = 0
            while (true) {
                val byte = stream.read()
                if (byte == -1) break
                if (byte == 10) count++
            }
            return count
        }
    }
}
