package jamgmilk.obsigit.domain.model

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
    val shortHash: String = hash.take(7),
    val authorName: String,
    val authorEmail: String,
    val message: String,
    val timestamp: Long
)

data class GitBranch(
    val name: String,
    val fullRef: String,
    val isRemote: Boolean,
    val isCurrent: Boolean
)
