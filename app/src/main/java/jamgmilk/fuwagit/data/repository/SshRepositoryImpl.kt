package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.SshDataSource
import jamgmilk.fuwagit.domain.repository.CloneUrlInfo
import jamgmilk.fuwagit.domain.repository.SshRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SshRepositoryImpl @Inject constructor(
    private val sshDataSource: SshDataSource
) : SshRepository {

    override suspend fun testSshConnection(
        host: String,
        privateKeyPem: String,
        passphrase: String?
    ): AppResult<String> = withContext(Dispatchers.IO) {
        sshDataSource.testSshConnection(host, privateKeyPem, passphrase).toAppResult()
    }

    override fun getHostFromUrl(url: String): String? {
        if (url.isBlank()) return null
        return when {
            url.startsWith("git@") -> {
                url.substringAfter("@").substringBefore(":").substringBefore(".git")
            }
            url.startsWith("https://") || url.startsWith("http://") -> {
                url.substringAfter("://").substringBefore("/").substringBefore(":")
            }
            else -> null
        }
    }

    override fun getRepositoryPathFromUrl(url: String): String? {
        if (url.isBlank()) return null
        return when {
            url.startsWith("git@") -> {
                url.substringAfter(":").substringAfter("/")
            }
            url.startsWith("https://") || url.startsWith("http://") -> {
                val pathPart = url.substringAfter("://").substringAfter("/")
                if (pathPart.isNotBlank()) pathPart else null
            }
            url.startsWith("/") -> url
            else -> null
        }
    }

    override fun isSshUrl(url: String): Boolean {
        return url.startsWith("git@") || (url.contains("@") && url.contains(":") && !url.contains("://"))
    }

    override fun formatSshCloneUrl(host: String, repositoryPath: String): String {
        val cleanHost = host.trim()
        val cleanPath = repositoryPath.trim().removeSuffix(".git")
        return "git@$cleanHost:$cleanPath.git"
    }

    override fun formatHttpsCloneUrl(host: String, repositoryPath: String): String {
        val cleanHost = host.trim()
        val cleanPath = repositoryPath.trim().removeSuffix(".git")
        return "https://$cleanHost/$cleanPath.git"
    }

    override fun parseCloneUrl(url: String): CloneUrlInfo? {
        if (url.isBlank()) return null

        val type = when {
            url.startsWith("git@") -> CloneUrlInfo.UrlType.SSH
            url.startsWith("https://") || url.startsWith("http://") -> CloneUrlInfo.UrlType.HTTPS
            url.contains("@") && url.contains(":") && !url.contains("://") -> CloneUrlInfo.UrlType.SSH
            else -> CloneUrlInfo.UrlType.UNKNOWN
        }

        val host = getHostFromUrl(url) ?: return null
        val repositoryPath = getRepositoryPathFromUrl(url) ?: return null

        return CloneUrlInfo(
            type = type,
            host = host,
            repositoryPath = repositoryPath,
            originalUrl = url
        )
    }
}