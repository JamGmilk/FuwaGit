package jamgmilk.fuwagit.domain.model.git

import jamgmilk.fuwagit.core.util.UrlUtils
import jamgmilk.fuwagit.ui.screen.credentials.CredentialType

data class GitRemote(
    val name: String,
    val fetchUrl: String,
    val pushUrl: String?
) {
    val primaryUrl: String get() = pushUrl ?: fetchUrl

    val credentialType: CredentialType? get() = when {
        UrlUtils.isSshUrl(primaryUrl) -> CredentialType.SSH
        UrlUtils.isHttpsUrl(primaryUrl) -> CredentialType.HTTPS
        else -> null
    }
}
