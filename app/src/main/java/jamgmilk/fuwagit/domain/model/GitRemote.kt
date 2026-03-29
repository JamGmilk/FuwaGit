package jamgmilk.fuwagit.domain.model

data class GitRemote(
    val name: String,
    val fetchUrl: String,
    val pushUrl: String?
)
