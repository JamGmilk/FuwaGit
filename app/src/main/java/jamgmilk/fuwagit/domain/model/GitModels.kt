package jamgmilk.fuwagit.domain.model

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
    val message: String
)

data class CommitStats(
    val totalCommits: Int,
    val uniqueAuthors: Int,
    val commitsToday: Int,
    val commitsThisWeek: Int,
    val commitsThisMonth: Int
)
