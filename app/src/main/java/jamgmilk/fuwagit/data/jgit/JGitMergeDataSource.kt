package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.CleanResult
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.git.ConflictStatus
import jamgmilk.fuwagit.domain.model.git.GitConflict
import org.eclipse.jgit.api.Git
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Git merge, rebase, conflict resolution, and clean operations.
 */
@Singleton
class JGitMergeDataSource @Inject constructor(
    private val core: GitCoreDataSource
) : GitMergeDataSource {
    /**
     * Merges a branch into the current branch.
     */
    override fun mergeBranch(repoPath: String, branchName: String): Result<ConflictResult> =
        core.withGit(repoPath) { git ->
            try {
                val mergeResult = git.merge()
                    .include(git.repository.findRef(branchName))
                    .setCommit(true)
                    .call()

                val conflicts = mergeResult.conflicts
                val hasConflicts = conflicts != null && conflicts.isNotEmpty()

                if (hasConflicts) {
                    val conflictFiles = getConflictFiles(git)
                    ConflictResult(
                        isConflicting = true,
                        operationType = "MERGE",
                        conflicts = conflictFiles,
                        message = "Merge conflict: ${conflictFiles.size} file(s) need resolution"
                    )
                } else if (mergeResult.mergeStatus.name == "FAST_FORWARD" || mergeResult.mergeStatus.name == "MERGED") {
                    ConflictResult(
                        isConflicting = false,
                        operationType = "MERGE",
                        message = "Merge successful"
                    )
                } else {
                    ConflictResult(
                        isConflicting = false,
                        operationType = "MERGE",
                        message = "Merge completed: ${mergeResult.mergeStatus.name}"
                    )
                }
            } catch (e: Exception) {
                throw Exception("Merge failed: ${e.message}")
            }
        }

    /**
     * Rebases the current branch onto another branch.
     */
    override fun rebaseBranch(repoPath: String, branchName: String): Result<ConflictResult> =
        core.withGit(repoPath) { git ->
            try {
                val rebaseResult = git.rebase().setUpstream(branchName).call()
                val hasConflicts = rebaseResult.status.name == "CONFLICTING"

                if (hasConflicts) {
                    val conflictFiles = getConflictFiles(git)
                    ConflictResult(
                        isConflicting = true,
                        operationType = "REBASE",
                        conflicts = conflictFiles,
                        message = "Rebase conflict: ${conflictFiles.size} file(s) need resolution"
                    )
                } else if (rebaseResult.status.name == "UP_TO_DATE" ||
                           rebaseResult.status.name == "FAST_FORWARD" ||
                           rebaseResult.status.name == "OK") {
                    ConflictResult(
                        isConflicting = false,
                        operationType = "REBASE",
                        message = "Rebase successful"
                    )
                } else {
                    ConflictResult(
                        isConflicting = false,
                        operationType = "REBASE",
                        message = "Rebase completed: ${rebaseResult.status.name}"
                    )
                }
            } catch (e: Exception) {
                throw Exception("Rebase failed: ${e.message}")
            }
        }

    /**
     * Gets the current conflict status.
     */
    override fun getConflictStatus(repoPath: String): Result<ConflictResult> =
        core.withGit(repoPath) { git ->
            val status = git.status().call()
            if (status.conflicting.isNotEmpty()) {
                val gitDir = git.repository.directory
                val isMerge = java.io.File(gitDir, "MERGE_HEAD").exists()
                val isRebase = java.io.File(gitDir, "rebase-apply").exists() ||
                               java.io.File(gitDir, "rebase-merge").exists()
                val operationType = when {
                    isMerge -> "MERGE"
                    isRebase -> "REBASE"
                    else -> "UNKNOWN"
                }
                val conflicts = getConflictFiles(git)
                ConflictResult(
                    isConflicting = true,
                    operationType = operationType,
                    conflicts = conflicts,
                    message = "${conflicts.size} conflict(s) need resolution"
                )
            } else {
                ConflictResult(isConflicting = false, message = "No conflicts")
            }
        }

    /**
     * Marks a conflict file as resolved (adds to staging area).
     */
    override fun markConflictResolved(repoPath: String, filePath: String): Result<Unit> =
        core.withGit(repoPath) { git ->
            git.add().addFilepattern(filePath).call()
            Unit
        }

    /**
     * Aborts an ongoing rebase operation.
     */
    override fun abortRebase(repoPath: String): Result<String> =
        core.withGit(repoPath) { git ->
            git.rebase().setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT).call()
            "Rebase aborted"
        }

    /**
     * Cleans untracked files from the working directory.
     */
    override fun clean(repoPath: String, dryRun: Boolean): Result<CleanResult> =
        core.withGit(repoPath) { git ->
            val cleanedPaths = git.clean()
                .setCleanDirectories(true)
                .setIgnore(false)
                .setDryRun(dryRun)
                .call()
            CleanResult(files = cleanedPaths.toList(), isDryRun = dryRun)
        }

    /**
     * Gets the list of conflicting files.
     */
    private fun getConflictFiles(git: Git): List<GitConflict> {
        val status = git.status().call()
        return status.conflicting.map { path ->
            GitConflict(
                path = path,
                name = java.io.File(path).name,
                status = ConflictStatus.UNRESOLVED,
                description = "Conflict"
            )
        }
    }
}
