package jamgmilk.fuwagit.domain.model.git

data class CloneOptions(
    val branch: String? = null,
    val cloneAllBranches: Boolean = true,
    val depth: Int? = null,
    val isBare: Boolean = false
)
