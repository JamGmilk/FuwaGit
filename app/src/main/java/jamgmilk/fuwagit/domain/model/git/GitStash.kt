package jamgmilk.fuwagit.domain.model.git

data class GitStash(
    val index: Int,
    val message: String,
    val branch: String,
    val timestamp: Long
)
