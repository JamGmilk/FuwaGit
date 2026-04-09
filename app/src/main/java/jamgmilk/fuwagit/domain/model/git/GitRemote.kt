package jamgmilk.fuwagit.domain.model.git

data class GitRemote(
    val name: String,
    val fetchUrl: String,
    val pushUrl: String?
) {
    val primaryUrl: String get() = pushUrl ?: fetchUrl

    val isPushUrlDifferent: Boolean get() = pushUrl != null && pushUrl != fetchUrl

    val credentialType: CredentialType get() = when {
        primaryUrl.startsWith("git@") || primaryUrl.contains("://github.com") -> CredentialType.SSH
        primaryUrl.startsWith("https://") || primaryUrl.startsWith("http://") -> CredentialType.HTTPS
        else -> CredentialType.UNKNOWN
    }

    val isSsh: Boolean get() = credentialType == CredentialType.SSH
    val isHttps: Boolean get() = credentialType == CredentialType.HTTPS

    val hostName: String get() {
        val url = primaryUrl
        return when {
            url.startsWith("git@") -> url.substringAfter(":").substringBefore(".git").substringBefore("/")
            url.contains("://") -> url.substringAfter("://").substringBefore(":").substringBefore("/")
            else -> url.substringBefore(":").substringBefore("/")
        }
    }

    val displayUrl: String get() {
        val url = primaryUrl
        return when {
            url.startsWith("git@") -> url.substringBefore(".git").substringBefore(",")
            url.startsWith("https://") || url.startsWith("http://") -> url.substringBefore(".git").substringBefore("@")
            else -> url.substringBefore(".git")
        }
    }

    enum class CredentialType {
        SSH,
        HTTPS,
        UNKNOWN
    }
}
