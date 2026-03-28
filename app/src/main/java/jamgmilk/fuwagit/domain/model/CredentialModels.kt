package jamgmilk.fuwagit.domain.model

data class HttpsCredential(
    val id: String,
    val host: String,
    val username: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis()
)
