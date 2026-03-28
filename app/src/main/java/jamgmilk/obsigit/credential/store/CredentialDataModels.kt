package jamgmilk.obsigit.credential.store

import kotlinx.serialization.Serializable

@Serializable
data class PublicCredentialData(
    val version: Int = 1,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val https_credentials: List<PublicHttpsCredential> = emptyList(),
    val ssh_keys: List<PublicSshKey> = emptyList()
)

@Serializable
data class PublicHttpsCredential(
    val id: String,
    val host: String,
    val username: String,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val has_password: Boolean = false,
    val has_pat: Boolean = false
)

@Serializable
data class PublicSshKey(
    val id: String,
    val name: String,
    val type: String,
    val public_key: String,
    val fingerprint: String,
    val comment: String = "",
    val created_at: Long = System.currentTimeMillis(),
    val has_passphrase: Boolean = false,
    val has_private_key: Boolean = true
)

@Serializable
data class PrivateCredentialData(
    val version: Int = 1,
    val https_credentials: List<PrivateHttpsCredential> = emptyList(),
    val ssh_keys: List<PrivateSshKey> = emptyList(),
    val master_password_hint: String? = null
)

@Serializable
data class PrivateHttpsCredential(
    val id: String,
    val password: String? = null,
    val pat: String? = null,
    val pat_scopes: List<String> = emptyList()
)

@Serializable
data class PrivateSshKey(
    val id: String,
    val private_key: String,
    val passphrase: String? = null
)

@Serializable
data class ExportData(
    val public_data: PublicCredentialData,
    val private_data: PrivateCredentialData,
    val exported_at: Long = System.currentTimeMillis()
)
