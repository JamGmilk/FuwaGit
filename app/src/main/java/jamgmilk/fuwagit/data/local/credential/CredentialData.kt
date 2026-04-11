package jamgmilk.fuwagit.data.local.credential

import jamgmilk.fuwagit.domain.model.credential.HttpsCredential as DomainHttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey as DomainSshKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialData(
    val version: Int = 1,
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("https_credentials")
    val httpsCredentials: List<HttpsCredential> = emptyList(),
    @SerialName("ssh_keys")
    val sshKeys: List<SshKey> = emptyList()
)

@Serializable
data class HttpsCredential(
    val uuid: String,
    val host: String,
    val username: String,
    val password: String,
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DomainHttpsCredential = DomainHttpsCredential(
        uuid = uuid,
        host = host,
        username = username,
        password = password,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Serializable
data class SshKey(
    val uuid: String,
    val name: String,
    val type: String,
    @SerialName("public_key")
    val publicKey: String,
    @SerialName("private_key")
    val privateKey: String,
    val passphrase: String? = null,
    val fingerprint: String,
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    val comment: String
        get() = try {
            val parts = publicKey.trim().split(" ")
            if (parts.size >= 3) parts[2] else ""
        } catch (e: Exception) {
            ""
        }

    fun toDomain(): DomainSshKey = DomainSshKey(
        uuid = uuid,
        name = name,
        type = type,
        publicKey = publicKey,
        privateKey = privateKey,
        passphrase = passphrase,
        fingerprint = fingerprint,
        createdAt = createdAt
    )
}

@Serializable
data class ExportData(
    @SerialName("credential_data")
    val credentialData: CredentialData,
    @SerialName("exported_at")
    val exportedAt: Long = System.currentTimeMillis()
)
