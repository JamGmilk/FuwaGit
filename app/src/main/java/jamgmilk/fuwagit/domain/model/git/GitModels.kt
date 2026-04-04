package jamgmilk.fuwagit.domain.model.git

enum class GitChangeType {
    Added, Modified, Removed, Untracked, Renamed, Conflicting
}

data class GitFileStatus(
    val path: String,
    val name: String,
    val isStaged: Boolean,
    val changeType: GitChangeType
)

data class GitCommit(
    val hash: String,
    val shortHash: String,
    val authorName: String,
    val authorEmail: String,
    val message: String,
    val timestamp: Long,
    val parentHashes: List<String> = emptyList()
) {
    val isMerge: Boolean get() = parentHashes.size > 1
}

/**
 * Commit 涓殑鏂囦欢鍙樻洿淇℃伅
 *
 * @param path 鏂囦欢璺緞
 * @param name 鏂囦欢鍚?
 * @param changeType 鍙樻洿绫诲瀷
 * @param additions 鏂板琛屾暟
 * @param deletions 鍒犻櫎琛屾暟
 */
data class GitCommitFileChange(
    val path: String,
    val name: String,
    val changeType: GitChangeType,
    val additions: Int = 0,
    val deletions: Int = 0
) {
    val totalChanges: Int get() = additions + deletions
}

/**
 * Commit 璇︽儏锛屽寘鍚枃浠跺彉鏇村垪琛?
 */
data class GitCommitDetail(
    val commit: GitCommit,
    val fileChanges: List<GitCommitFileChange> = emptyList(),
    val totalAdditions: Int = 0,
    val totalDeletions: Int = 0,
    val totalFiles: Int = 0
) {
    val totalChanges: Int get() = totalAdditions + totalDeletions
}

data class GitBranch(
    val name: String,
    val fullRef: String,
    val isRemote: Boolean,
    val isCurrent: Boolean
)

data class GitRepoStatus(
    val isGitRepo: Boolean,
    val branch: String,
    val hasUncommittedChanges: Boolean,
    val untrackedCount: Int,
    val message: String
)

data class PullResult(
    val isSuccessful: Boolean,
    val message: String,
    // Fetch 缁撴灉
    val fetchResult: FetchResult? = null,
    // Merge 缁撴灉
    val mergeResult: MergeResultDetail? = null,
    // Rebase 缁撴灉锛堝鏋滀娇鐢ㄤ簡 rebase锛?
    val rebaseResult: RebaseResultDetail? = null,
    // 鏄惁鏈夊啿绐?
    val hasConflicts: Boolean = false,
    // 璇︾粏淇℃伅
    val detailMessage: String = ""
) {
    val isUpToDate: Boolean get() = mergeResult?.mergeStatus == MergeStatus.ALREADY_UP_TO_DATE
    val isFastForward: Boolean get() = mergeResult?.mergeStatus == MergeStatus.FAST_FORWARD
    val isMerged: Boolean get() = mergeResult?.mergeStatus == MergeStatus.MERGED
    val commitCount: Int get() = mergeResult?.commitCount ?: 0
}

/**
 * Fetch 缁撴灉璇︽儏
 */
data class FetchResult(
    val isSuccessful: Boolean,
    val messages: List<String> = emptyList()
)

/**
 * Merge 缁撴灉璇︽儏
 */
data class MergeResultDetail(
    val mergeStatus: MergeStatus,
    val commitCount: Int = 0,
    val fastForward: Boolean = false,
    val conflicts: Map<String, Int> = emptyMap()
)

/**
 * Merge 鐘舵€?
 */
enum class MergeStatus {
    ALREADY_UP_TO_DATE,
    FAST_FORWARD,
    MERGED,
    FAILED,
    CONFLICTING,
    ABORTED,
    UNKNOWN
}

/**
 * Rebase 缁撴灉璇︽儏
 */
data class RebaseResultDetail(
    val status: RebaseStatus,
    val commitCount: Int = 0,
    val conflicts: List<String> = emptyList()
)

/**
 * Rebase 鐘舵€?
 */
enum class RebaseStatus {
    UP_TO_DATE,
    FAST_FORWARD,
    OK,
    CONFLICTING,
    ABORTED,
    FAILED,
    UNKNOWN
}

data class CleanResult(
    val files: List<String>,
    val isDryRun: Boolean
) {
    val isEmpty: Boolean get() = files.isEmpty()
    val count: Int get() = files.size
}

data class CommitStats(
    val totalCommits: Int,
    val uniqueAuthors: Int,
    val commitsToday: Int,
    val commitsThisWeek: Int,
    val commitsThisMonth: Int
)
