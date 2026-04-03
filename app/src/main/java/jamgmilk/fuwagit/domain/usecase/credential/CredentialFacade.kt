package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import javax.inject.Inject

/**
 * Facade for credential operations.
 * Aggregates related UseCases to reduce ViewModel dependency count.
 */
class CredentialFacade @Inject constructor(
    private val getHttpsCredentialsUseCase: GetHttpsCredentialsUseCase,
    private val getHttpsPasswordUseCase: GetHttpsPasswordUseCase,
    private val getSshKeysUseCase: GetSshKeysUseCase,
    private val getSshPrivateKeyUseCase: GetSshPrivateKeyUseCase,
    private val getSshPassphraseUseCase: GetSshPassphraseUseCase
) {
    suspend fun getHttpsCredentials(): AppResult<List<HttpsCredential>> =
        getHttpsCredentialsUseCase()

    suspend fun getHttpsPassword(uuid: String): AppResult<String> =
        getHttpsPasswordUseCase(uuid)

    suspend fun getSshKeys(): AppResult<List<SshKey>> =
        getSshKeysUseCase()

    suspend fun getSshPrivateKey(uuid: String): AppResult<String> =
        getSshPrivateKeyUseCase(uuid)

    suspend fun getSshPassphrase(uuid: String): AppResult<String?> =
        getSshPassphraseUseCase(uuid)

    /**
     * Resolves credentials based on selected UUIDs.
     */
    suspend fun resolveCredentials(
        selectedCredentialUuid: String?,
        selectedSshKeyUuid: String?,
        httpsCredentials: List<HttpsCredential>,
        sshKeys: List<SshKey>
    ): CloneCredential? {
        return when {
            selectedCredentialUuid != null -> {
                val cred = httpsCredentials.find { it.uuid == selectedCredentialUuid } ?: return null
                val password = getHttpsPassword(cred.uuid)
                if (password is AppResult.Success) {
                    CloneCredential.Https(cred.username, password.data)
                } else null
            }
            selectedSshKeyUuid != null -> {
                val privateKey = getSshPrivateKey(selectedSshKeyUuid)
                if (privateKey is AppResult.Success) {
                    val passphrase = getSshPassphrase(selectedSshKeyUuid)
                    val passphraseData = if (passphrase is AppResult.Success) passphrase.data else null
                    CloneCredential.Ssh(privateKey.data, passphraseData)
                } else null
            }
            else -> {
                // Auto-select first available credential
                if (httpsCredentials.isNotEmpty()) {
                    val cred = httpsCredentials.first()
                    val password = getHttpsPassword(cred.uuid)
                    if (password is AppResult.Success) {
                        CloneCredential.Https(cred.username, password.data)
                    } else null
                } else if (sshKeys.isNotEmpty()) {
                    val key = sshKeys.first()
                    val privateKey = getSshPrivateKey(key.uuid)
                    if (privateKey is AppResult.Success) {
                        val passphrase = getSshPassphrase(key.uuid)
                        val passphraseData = if (passphrase is AppResult.Success) passphrase.data else null
                        CloneCredential.Ssh(privateKey.data, passphraseData)
                    } else null
                } else {
                    null
                }
            }
        }
    }
}
