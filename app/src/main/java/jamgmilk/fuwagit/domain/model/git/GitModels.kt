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

    val shortMessage: String get() = message.lineSequence().firstOrNull()?.take(72) ?: ""

    val relativeTime: String get() {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            weeks < 4 -> "${weeks}w ago"
            months < 12 -> "${months}mo ago"
            else -> "${years}y ago"
        }
    }

    val formattedTimestamp: String get() {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    val authorDisplayName: String get() {
        val name = authorName.trim()
        if (name.contains(" ") && !name.contains("<")) {
            return name.substringBefore(" ") + " " + name.substringAfter(" ").take(1) + "."
        }
        return name.substringBefore("<").trim().ifEmpty { authorEmail.substringBefore("@") }
    }

    val isInitialCommit: Boolean get() = parentHashes.isEmpty()

    val parentCount: Int get() = parentHashes.size

    val primaryParentHash: String? get() = parentHashes.firstOrNull()
}

/**
 * Commit 中的文件变更信息
 *
 * @param path 文件路径
 * @param name 文件名
 * @param changeType 变更类型
 * @param additions 新增行数
 * @param deletions 删除行数
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
 * Commit 详情，包含文件变更列表
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
    // Fetch 结果
    val fetchResult: FetchResult? = null,
    // Merge 结果
    val mergeResult: MergeResultDetail? = null,
    // Rebase 结果（如果使用了 rebase）
    val rebaseResult: RebaseResultDetail? = null,
    // 是否有冲突
    val hasConflicts: Boolean = false,
    // 详细信息
    val detailMessage: String = ""
) {
    val isUpToDate: Boolean get() = mergeResult?.mergeStatus == MergeStatus.ALREADY_UP_TO_DATE
    val isFastForward: Boolean get() = mergeResult?.mergeStatus == MergeStatus.FAST_FORWARD
    val isMerged: Boolean get() = mergeResult?.mergeStatus == MergeStatus.MERGED
    val commitCount: Int get() = mergeResult?.commitCount ?: 0
}

/**
 * Fetch 结果详情
 */
data class FetchResult(
    val isSuccessful: Boolean,
    val messages: List<String> = emptyList()
)

/**
 * Merge 结果详情
 */
data class MergeResultDetail(
    val mergeStatus: MergeStatus,
    val commitCount: Int = 0,
    val fastForward: Boolean = false,
    val conflicts: Map<String, Int> = emptyMap()
)

/**
 * Merge 状态
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
 * Rebase 结果详情
 */
data class RebaseResultDetail(
    val status: RebaseStatus,
    val commitCount: Int = 0,
    val conflicts: List<String> = emptyList()
)

/**
 * Rebase 状态
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
