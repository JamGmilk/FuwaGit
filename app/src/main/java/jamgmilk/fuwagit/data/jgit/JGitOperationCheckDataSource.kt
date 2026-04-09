package jamgmilk.fuwagit.data.jgit

import org.eclipse.jgit.api.Git
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JGitOperationCheckDataSource @Inject constructor(
    private val core: GitCoreDataSource
) : GitOperationCheckDataSource {

    override fun checkPrePullStatus(repoPath: String): Result<PrePullCheckResult> {
        return try {
            Git.open(File(repoPath)).use { git ->
                val gitDir = git.repository.directory
                val status = git.status().call()

                val lockStatus = core.isRepositoryLocked(repoPath)
                val inRebase = File(gitDir, "rebase-apply").exists() ||
                               File(gitDir, "rebase-merge").exists()
                val inMerge = File(gitDir, "MERGE_HEAD").exists()

                val message = buildString {
                    when {
                        lockStatus.isLocked -> append(lockStatus.message)
                        inRebase -> append("Repository is in the middle of a rebase")
                        inMerge -> append("Repository is in the middle of a merge")
                        status.conflicting.isNotEmpty() -> append("Unresolved merge conflicts exist")
                        status.hasUncommittedChanges() -> append("Has uncommitted changes")
                        else -> append("Ready to pull")
                    }
                }

                PrePullCheckResult(
                    canPull = !lockStatus.isLocked && !inRebase && !inMerge && status.conflicting.isEmpty(),
                    hasLocalChanges = status.hasUncommittedChanges(),
                    hasStagedChanges = status.staged.isNotEmpty(),
                    hasUntrackedFiles = status.untracked.isNotEmpty(),
                    hasConflicts = status.conflicting.isNotEmpty(),
                    inMidRebase = inRebase,
                    inMidMerge = inMerge,
                    isLocked = lockStatus.isLocked,
                    message = message
                ).asSuccess()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun checkPrePushStatus(repoPath: String): Result<PrePushCheckResult> {
        return try {
            Git.open(File(repoPath)).use { git ->
                val gitDir = git.repository.directory
                val status = git.status().call()

                val lockStatus = core.isRepositoryLocked(repoPath)
                val currentBranch = git.repository.branch

                val remoteBranchAhead = calculateAheadBehind(git, currentBranch)

                val message = buildString {
                    when {
                        lockStatus.isLocked -> append(lockStatus.message)
                        status.conflicting.isNotEmpty() -> append("Unresolved conflicts exist")
                        status.hasUncommittedChanges() -> append("Has uncommitted changes")
                        remoteBranchAhead.first > 0 -> append("Remote is ${remoteBranchAhead.first} commit(s) ahead")
                        remoteBranchAhead.second > 0 -> append("You are ${remoteBranchAhead.second} commit(s) behind remote")
                        else -> append("Ready to push")
                    }
                }

                PrePushCheckResult(
                    canPush = !lockStatus.isLocked && status.conflicting.isEmpty(),
                    hasUncommittedChanges = status.hasUncommittedChanges(),
                    hasConflicts = status.conflicting.isNotEmpty(),
                    hasStashableChanges = status.hasUncommittedChanges(),
                    currentBranch = currentBranch,
                    remoteBranchAhead = remoteBranchAhead.first,
                    remoteBranchBehind = remoteBranchAhead.second,
                    message = message
                ).asSuccess()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isRepositoryLocked(repoPath: String): Boolean {
        return core.isRepositoryLocked(repoPath).isLocked
    }

    override fun getConflictDetails(repoPath: String): Result<List<ConflictFileInfo>> {
        return try {
            Git.open(File(repoPath)).use { git ->
                val status = git.status().call()

                if (status.conflicting.isEmpty()) {
                    return@use emptyList<ConflictFileInfo>().asSuccess()
                }

                val conflictFiles = status.conflicting.map { path ->
                    val file = File(repoPath, path)
                    ConflictFileInfo(
                        path = path,
                        name = file.name,
                        baseContent = null,
                        oursContent = null,
                        theirsContent = null
                    )
                }

                conflictFiles.asSuccess()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateAheadBehind(git: Git, branchName: String): Pair<Int, Int> {
        return try {
            val revWalk = org.eclipse.jgit.revwalk.RevWalk(git.repository)
            val ref = git.repository.getRef("refs/heads/$branchName")
            val remoteRef = git.repository.getRef("refs/remotes/origin/$branchName")

            if (ref == null || remoteRef == null) {
                return Pair(0, 0)
            }

            val localCommit = revWalk.parseCommit(ref.objectId)
            val remoteCommit = revWalk.parseCommit(remoteRef.objectId)

            val localAncestor = revWalk.isMergedInto(localCommit, remoteCommit)
            val remoteAncestor = revWalk.isMergedInto(remoteCommit, localCommit)

            revWalk.dispose()

            when {
                localAncestor && !remoteAncestor -> {
                    val revWalk2 = org.eclipse.jgit.revwalk.RevWalk(git.repository)
                    val counter = revWalk2.newCommitCounter()
                    counter.markStart(localCommit)
                    counter.markUninteresting(remoteCommit)
                    val ahead = counter.count()
                    revWalk2.dispose()
                    Pair(ahead, 0)
                }
                remoteAncestor && !localAncestor -> {
                    val revWalk2 = org.eclipse.jgit.revwalk.RevWalk(git.repository)
                    val counter = revWalk2.newCommitCounter()
                    counter.markStart(remoteCommit)
                    counter.markUninteresting(localCommit)
                    val behind = counter.count()
                    revWalk2.dispose()
                    Pair(0, behind)
                }
                else -> Pair(0, 0)
            }
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    private fun <T> T.asSuccess(): Result<T> = Result.success(this)
}
