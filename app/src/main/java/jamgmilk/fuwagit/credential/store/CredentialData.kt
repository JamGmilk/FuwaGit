package jamgmilk.fuwagit.credential.store

import kotlinx.serialization.Serializable

@Serializable
data class CredentialData(
    val version: Int = 1,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val https_credentials: List<HttpsCredential> = emptyList(),
    val ssh_keys: List<SshKey> = emptyList(),
    val master_password_hint: String? = null
)

@Serializable
data class HttpsCredential(
    val uuid: String,
    val host: String,
    val username: String,
    val password: String,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)

@Serializable
data class SshKey(
    val uuid: String,
    val name: String,
    val type: String,
    val public_key: String,
    val private_key: String,
    val passphrase: String? = null,
    val fingerprint: String,
    val created_at: Long = System.currentTimeMillis()
) {
    val comment: String
        get() = extractCommentFromPublicKey(public_key)
}

@Serializable
data class ExportData(
    val credential_data: CredentialData,
    val exported_at: Long = System.currentTimeMillis()
)

fun extractCommentFromPublicKey(publicKey: String): String {
    return try {
        val parts = publicKey.trim().split(" ")
        if (parts.size >= 3) parts[2] else ""
    } catch (e: Exception) {
        ""
    }
}
