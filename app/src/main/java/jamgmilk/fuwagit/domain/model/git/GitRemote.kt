package jamgmilk.fuwagit.domain.model.git

data class GitRemote(
    val name: String,
    val fetchUrl: String,
    val pushUrl: String?
) {
    val primaryUrl: String get() = pushUrl ?: fetchUrl

    val credentialType: CredentialType get() = when {
        primaryUrl.startsWith("git@") || primaryUrl.contains("://github.com") -> CredentialType.SSH
        primaryUrl.startsWith("https://") || primaryUrl.startsWith("http://") -> CredentialType.HTTPS
        else -> CredentialType.UNKNOWN
    }

    enum class CredentialType {
        SSH,
        HTTPS,
        UNKNOWN
    }
}
