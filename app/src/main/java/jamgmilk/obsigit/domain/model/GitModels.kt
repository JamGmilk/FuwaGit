package jamgmilk.obsigit.domain.model

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
