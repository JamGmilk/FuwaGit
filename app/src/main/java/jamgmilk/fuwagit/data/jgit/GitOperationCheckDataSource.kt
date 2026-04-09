package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.GitRepoStatus

interface GitOperationCheckDataSource {
    fun checkPrePullStatus(repoPath: String): Result<PrePullCheckResult>
    fun checkPrePushStatus(repoPath: String): Result<PrePushCheckResult>
    fun isRepositoryLocked(repoPath: String): Boolean
    fun getConflictDetails(repoPath: String): Result<List<ConflictFileInfo>>
}

data class PrePullCheckResult(
    val canPull: Boolean,
    val hasLocalChanges: Boolean = false,
    val hasStagedChanges: Boolean = false,
    val hasUntrackedFiles: Boolean = false,
    val hasConflicts: Boolean = false,
    val inMidRebase: Boolean = false,
    val inMidMerge: Boolean = false,
    val isLocked: Boolean = false,
    val isDetachedHead: Boolean = false,
    val message: String = ""
)

data class PrePushCheckResult(
    val canPush: Boolean,
    val hasUncommittedChanges: Boolean = false,
    val hasConflicts: Boolean = false,
    val hasStashableChanges: Boolean = false,
    val hasDiverged: Boolean = false,
    val isEmpty: Boolean = false,
    val currentBranch: String = "",
    val remoteBranchAhead: Int = 0,
    val remoteBranchBehind: Int = 0,
    val message: String = ""
)

data class ConflictFileInfo(
    val path: String,
    val name: String,
    val baseContent: String? = null,
    val oursContent: String? = null,
    val theirsContent: String? = null
)
