package jamgmilk.fuwagit.domain.model.git

data class GitRemote(
    val name: String,
    val fetchUrl: String,
    val pushUrl: String?
)
