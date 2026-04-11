package jamgmilk.fuwagit.data.jgit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JGitOperationCheckDataSource @Inject constructor(
    private val core: GitCoreDataSource
) : GitOperationCheckDataSource {

    override fun checkPrePullStatus(repoPath: String): Result<PrePullCheckResult> {
        return core.withGit(repoPath) { git ->
            val gitDir = git.repository.directory
            val status = git.status().call()
            val lockStatus = core.isRepositoryLocked(repoPath)
            val inRebase = File(gitDir, "rebase-apply").exists() ||
                           File(gitDir, "rebase-merge").exists()
            val inMerge = File(gitDir, "MERGE_HEAD").exists()

            val fullBranch = git.repository.fullBranch
            val isDetached = fullBranch == null || !fullBranch.startsWith("refs/heads/")

            val message = buildString {
                when {
                    lockStatus.isLocked -> append(lockStatus.message)
                    isDetached -> append("Cannot pull in detached HEAD state. Checkout a branch first.")
                    inRebase -> append("Repository is in the middle of a rebase")
                    inMerge -> append("Repository is in the middle of a merge")
                    status.conflicting.isNotEmpty() -> append("Unresolved merge conflicts exist")
                    status.hasUncommittedChanges() -> append("Has uncommitted changes")
                    else -> append("Ready to pull")
                }
            }

            PrePullCheckResult(
                canPull = !lockStatus.isLocked && !inRebase && !inMerge && status.conflicting.isEmpty() && !isDetached,
                hasLocalChanges = status.hasUncommittedChanges(),
                hasStagedChanges = status.added.isNotEmpty() || status.changed.isNotEmpty() || status.removed.isNotEmpty(),
                hasUntrackedFiles = status.untracked.isNotEmpty(),
                hasConflicts = status.conflicting.isNotEmpty(),
                inMidRebase = inRebase,
                inMidMerge = inMerge,
                isLocked = lockStatus.isLocked,
                isDetachedHead = isDetached,
                message = message
            )
        }
    }

    override fun checkPrePushStatus(repoPath: String): Result<PrePushCheckResult> {
        return core.withGit(repoPath) { git ->
            val status = git.status().call()
            val lockStatus = core.isRepositoryLocked(repoPath)
            val currentBranch = git.repository.branch

            val headRevision = try {
                git.repository.resolve("HEAD")
            } catch (_: Exception) {
                null
            }
            val empty = headRevision == null

            val remoteBranchAhead = calculateAheadBehind(git, currentBranch)
            val diverged = remoteBranchAhead.first > 0 && remoteBranchAhead.second > 0

            val message = buildString {
                when {
                    lockStatus.isLocked -> append(lockStatus.message)
                    empty -> append("Repository has no commits. Make at least one commit before pushing.")
                    status.conflicting.isNotEmpty() -> append("Unresolved conflicts exist")
                    status.hasUncommittedChanges() -> append("Has uncommitted changes")
                    diverged -> append("Branch has diverged: you have ${remoteBranchAhead.first} commit(s) and remote has ${remoteBranchAhead.second} commit(s). Fetch and merge first.")
                    remoteBranchAhead.first > 0 && remoteBranchAhead.second == 0 -> append("You are ${remoteBranchAhead.first} commit(s) ahead of remote")
                    remoteBranchAhead.second > 0 && remoteBranchAhead.first == 0 -> append("You are ${remoteBranchAhead.second} commit(s) behind remote")
                    else -> append("Ready to push")
                }
            }

            PrePushCheckResult(
                canPush = !lockStatus.isLocked && status.conflicting.isEmpty() && !diverged && !empty,
                hasUncommittedChanges = status.hasUncommittedChanges(),
                hasConflicts = status.conflicting.isNotEmpty(),
                hasStashableChanges = status.hasUncommittedChanges(),
                hasDiverged = diverged,
                isEmpty = empty,
                currentBranch = currentBranch,
                remoteBranchAhead = remoteBranchAhead.first,
                remoteBranchBehind = remoteBranchAhead.second,
                message = message
            )
        }
    }

    override fun isRepositoryLocked(repoPath: String): Boolean {
        return core.isRepositoryLocked(repoPath).isLocked
    }

    override fun getConflictDetails(repoPath: String): Result<List<ConflictFileInfo>> {
        return core.withGit(repoPath) { git ->
            val status = git.status().call()

            if (status.conflicting.isEmpty()) {
                return@withGit emptyList()
            }

            status.conflicting.map { path ->
                val file = File(repoPath, path)
                ConflictFileInfo(
                    path = path,
                    name = file.name,
                    baseContent = null,
                    oursContent = null,
                    theirsContent = null
                )
            }
        }
    }

    private fun calculateAheadBehind(git: Git, branchName: String): Pair<Int, Int> {
        val revWalk = RevWalk(git.repository)
        return try {
            val ref = git.repository.exactRef("refs/heads/$branchName")
            val remoteRef = git.repository.exactRef("refs/remotes/origin/$branchName")

            if (ref == null || remoteRef == null) {
                return Pair(0, 0)
            }

            val localCommit = revWalk.parseCommit(ref.target?.objectId ?: ref.objectId)
            val remoteCommit = revWalk.parseCommit(remoteRef.target?.objectId ?: remoteRef.objectId)

            val localAncestor = revWalk.isMergedInto(localCommit, remoteCommit)
            val remoteAncestor = revWalk.isMergedInto(remoteCommit, localCommit)

            when {
                localAncestor && !remoteAncestor -> {
                    RevWalk(git.repository).use { revWalk2 ->
                        var count = 0
                        revWalk2.markStart(revWalk2.parseCommit(ref.target?.objectId ?: ref.objectId))
                        revWalk2.markUninteresting(revWalk2.parseCommit(remoteRef.target?.objectId ?: remoteRef.objectId))
                        for (commit in revWalk2) count++
                        Pair(count, 0)
                    }
                }
                remoteAncestor && !localAncestor -> {
                    RevWalk(git.repository).use { revWalk2 ->
                        var count = 0
                        revWalk2.markStart(revWalk2.parseCommit(remoteRef.target?.objectId ?: remoteRef.objectId))
                        revWalk2.markUninteresting(revWalk2.parseCommit(ref.target?.objectId ?: ref.objectId))
                        for (commit in revWalk2) count++
                        Pair(0, count)
                    }
                }
                else -> Pair(0, 0)
            }
        } catch (e: Exception) {
            Pair(0, 0)
        } finally {
            revWalk.dispose()
        }
    }
}
