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
 * File change information for a commit
 *
 * @param path File path
 * @param name File name
 * @param changeType Change type
 * @param additions Number of added lines
 * @param deletions Number of deleted lines
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
 * Commit details with file change list
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
    // Fetch result
    val fetchResult: FetchResult? = null,
    // Merge result
    val mergeResult: MergeResultDetail? = null,
    // Rebase result (if rebase was used)
    val rebaseResult: RebaseResultDetail? = null,
    // Whether there are conflicts
    val hasConflicts: Boolean = false,
    // Detailed message
    val detailMessage: String = ""
) {
    val isUpToDate: Boolean get() = mergeResult?.mergeStatus == MergeStatus.ALREADY_UP_TO_DATE
    val isFastForward: Boolean get() = mergeResult?.mergeStatus == MergeStatus.FAST_FORWARD
    val isMerged: Boolean get() = mergeResult?.mergeStatus == MergeStatus.MERGED
    val commitCount: Int get() = mergeResult?.commitCount ?: 0
}

/**
 * Fetch result details
 */
data class FetchResult(
    val isSuccessful: Boolean,
    val messages: List<String> = emptyList()
)

/**
 * Merge result details
 */
data class MergeResultDetail(
    val mergeStatus: MergeStatus,
    val commitCount: Int = 0,
    val fastForward: Boolean = false,
    val conflicts: Map<String, Int> = emptyMap()
)

/**
 * Merge status
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
 * Rebase result details
 */
data class RebaseResultDetail(
    val status: RebaseStatus,
    val commitCount: Int = 0,
    val conflicts: List<String> = emptyList()
)

/**
 * Rebase status
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
