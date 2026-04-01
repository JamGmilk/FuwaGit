package jamgmilk.fuwagit.domain.model.credential

data class HttpsCredential(
    val uuid: String,
    val host: String,
    val username: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SshKey(
    val uuid: String,
    val name: String,
    val type: String,
    val publicKey: String,
    val privateKey: String,
    val passphrase: String? = null,
    val fingerprint: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val comment: String
        get() = extractCommentFromPublicKey(publicKey)
}

private fun extractCommentFromPublicKey(publicKey: String): String {
    return try {
        val parts = publicKey.trim().split(" ")
        if (parts.size >= 3) parts[2] else ""
    } catch (e: Exception) {
        ""
    }
}
