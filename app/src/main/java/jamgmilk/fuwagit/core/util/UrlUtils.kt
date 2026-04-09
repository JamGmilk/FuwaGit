package jamgmilk.fuwagit.core.util

object UrlUtils {
    private val SSH_URL_REGEX = Regex("^git@[^:]+:.+\\.git$")
    private val HTTPS_URL_REGEX = Regex("^https?://.+\\.git$", RegexOption.IGNORE_CASE)
    private val HTTPS_URL_NO_GIT_REGEX = Regex("^https?://.+", RegexOption.IGNORE_CASE)
    private val FILE_URL_REGEX = Regex("^file://.+")

    sealed class ValidationResult {
        data class Valid(val type: UrlType) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }

    enum class UrlType {
        SSH,
        HTTPS,
        FILE,
        UNKNOWN
    }

    fun validateGitUrl(url: String): ValidationResult {
        if (url.isBlank()) {
            return ValidationResult.Invalid("URL cannot be empty")
        }

        val trimmedUrl = url.trim()

        if (trimmedUrl.contains(" ")) {
            return ValidationResult.Invalid("URL cannot contain spaces")
        }

        if (trimmedUrl.startsWith("git@")) {
            return validateSshUrl(trimmedUrl)
        }

        if (trimmedUrl.startsWith("https://") || trimmedUrl.startsWith("http://")) {
            return validateHttpsUrl(trimmedUrl)
        }

        if (trimmedUrl.startsWith("file://")) {
            return ValidationResult.Valid(UrlType.FILE)
        }

        if (SSH_URL_REGEX.matches(trimmedUrl)) {
            return ValidationResult.Valid(UrlType.SSH)
        }

        if (trimmedUrl.endsWith(".git")) {
            return ValidationResult.Valid(UrlType.HTTPS)
        }

        return ValidationResult.Invalid("Invalid Git URL format")
    }

    private fun validateSshUrl(url: String): ValidationResult {
        if (!url.contains("@")) {
            return ValidationResult.Invalid("SSH URL must contain @")
        }
        if (!url.contains(":")) {
            return ValidationResult.Invalid("SSH URL must contain : after host")
        }

        val hostAndPath = url.substringAfter("@")
        val host = hostAndPath.substringBefore(":")
        val path = hostAndPath.substringAfter(":")

        if (host.isBlank()) {
            return ValidationResult.Invalid("SSH URL host is empty")
        }
        if (path.isBlank()) {
            return ValidationResult.Invalid("SSH URL path is empty")
        }
        if (!host.contains(".")) {
            return ValidationResult.Invalid("SSH URL host should be a valid hostname (e.g., github.com)")
        }

        return ValidationResult.Valid(UrlType.SSH)
    }

    private fun validateHttpsUrl(url: String): ValidationResult {
        if (!url.contains("://")) {
            return ValidationResult.Invalid("HTTPS URL must contain ://")
        }

        val hostAndPath = url.substringAfter("://")
        if (hostAndPath.isBlank()) {
            return ValidationResult.Invalid("HTTPS URL host is empty")
        }

        val host = hostAndPath.substringBefore("/").substringBefore("?")
        if (host.isBlank()) {
            return ValidationResult.Invalid("HTTPS URL host is empty")
        }

        if (!host.contains(".") && host != "localhost") {
            return ValidationResult.Invalid("HTTPS URL host should be a valid hostname")
        }

        return ValidationResult.Valid(UrlType.HTTPS)
    }

    fun getUrlType(url: String): UrlType {
        return when {
            url.startsWith("git@") -> UrlType.SSH
            url.startsWith("https://") || url.startsWith("http://") -> UrlType.HTTPS
            url.startsWith("file://") -> UrlType.FILE
            url.endsWith(".git") -> UrlType.HTTPS
            else -> UrlType.UNKNOWN
        }
    }

    fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.endsWith(".git") && !normalized.contains("?")) {
            normalized = "$normalized.git"
        }
        return normalized
    }

    fun extractHost(url: String): String? {
        return when {
            url.startsWith("git@") -> url.substringAfter("@").substringBefore(":").substringBefore(".git")
            url.contains("://") -> url.substringAfter("://").substringBefore("/").substringBefore(":")
            else -> null
        }
    }

    fun formatUrlForDisplay(url: String): String {
        return when {
            url.startsWith("git@") -> url
            url.startsWith("https://") || url.startsWith("http://") -> url.substringBefore(".git").substringAfter("://")
            else -> url
        }
    }
}