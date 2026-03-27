package jamgmilk.obsigit.ui.screen.credentials

import androidx.compose.runtime.Immutable
import jamgmilk.obsigit.domain.model.HttpsCredential
import jamgmilk.obsigit.credential.SshKeyInfo

@Immutable
data class CredentialsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val httpsCredentials: List<HttpsCredential> = emptyList(),
    val sshKeys: List<SshKeyInfo> = emptyList(),
    val keyStoreAvailable: Boolean = false
)
