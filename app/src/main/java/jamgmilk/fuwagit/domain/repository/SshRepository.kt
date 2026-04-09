package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult

interface SshRepository {
    suspend fun testSshConnection(
        host: String,
        privateKeyPem: String,
        passphrase: String?
    ): AppResult<String>

    fun getHostFromUrl(url: String): String?

    fun getRepositoryPathFromUrl(url: String): String?

    fun isSshUrl(url: String): Boolean

    fun formatSshCloneUrl(host: String, repositoryPath: String): String

    fun formatHttpsCloneUrl(host: String, repositoryPath: String): String

    fun parseCloneUrl(url: String): CloneUrlInfo?
}

data class CloneUrlInfo(
    val type: UrlType,
    val host: String,
    val repositoryPath: String,
    val originalUrl: String
) {
    enum class UrlType {
        SSH,
        HTTPS,
        UNKNOWN
    }
}