package jamgmilk.fuwagit.core.util

object UrlUtils {

    data class UrlValidationResult(
        val isValid: Boolean,
        val isSsh: Boolean,
        val isHttps: Boolean,
        val errorMessage: String? = null
    )

    fun validateUrl(url: String): UrlValidationResult {
        if (url.isBlank()) {
            return UrlValidationResult(isValid = false, isSsh = false, isHttps = false)
        }

        return when {
            url.startsWith("https://") || url.startsWith("http://") -> {
                if (url.contains(" ") || !url.contains(".") || url.length < 10) {
                    UrlValidationResult(isValid = false, isSsh = false, isHttps = true, errorMessage = "Invalid HTTPS URL format")
                } else {
                    UrlValidationResult(isValid = true, isSsh = false, isHttps = true)
                }
            }
            url.startsWith("git@") -> {
                val gitHostPattern = Regex("^git@[a-zA-Z0-9.-]+:[a-zA-Z0-9._/-]+$")
                if (gitHostPattern.matches(url)) {
                    UrlValidationResult(isValid = true, isSsh = true, isHttps = false)
                } else {
                    UrlValidationResult(isValid = false, isSsh = true, isHttps = false, errorMessage = "Invalid SSH format (expected: git@host:path)")
                }
            }
            url.startsWith("ssh://") -> {
                UrlValidationResult(isValid = true, isSsh = true, isHttps = false)
            }
            else -> {
                UrlValidationResult(isValid = false, isSsh = false, isHttps = false, errorMessage = "URL must start with https://, http://, git@, ssh://")
            }
        }
    }

    fun isSshUrl(url: String): Boolean {
        return url.startsWith("git@") || url.startsWith("ssh://")
    }

    fun isHttpsUrl(url: String): Boolean {
        return url.startsWith("https://") || url.startsWith("http://")
    }

    fun extractHost(url: String): String? {
        return when {
            url.startsWith("git@") -> url.substringAfter("@").substringBefore(":")
            url.startsWith("ssh://") -> {
                val afterProtocol = url.removePrefix("ssh://")
                if (afterProtocol.contains("@")) {
                    afterProtocol.substringAfter("@").substringBefore(":")
                } else {
                    afterProtocol.substringBefore(":")
                }
            }
            url.contains("://") -> url.substringAfter("://").substringBefore("/").substringBefore(":")
            else -> null
        }
    }

    fun extractRepoName(url: String): String {
        return url
            .substringAfterLast("/")
            .substringBefore(".git")
            .substringBefore("?")
            .ifBlank { "repository" }
    }
}
