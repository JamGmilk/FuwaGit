package jamgmilk.fuwagit.domain.model

data class GitTag(
    val name: String,
    val commitHash: String,
    val message: String?,
    val tagger: String?,
    val timestamp: Long
)
