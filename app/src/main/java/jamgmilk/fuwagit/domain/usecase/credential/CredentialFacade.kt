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
    private val getSshPassphraseUseCase: GetSshPassphraseUseCase,
    private val resolveCloneCredentialUseCase: ResolveCloneCredentialUseCase,
    private val credentialRepository: jamgmilk.fuwagit.domain.repository.CredentialRepository
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

    suspend fun isUnlocked(): Boolean = credentialRepository.isUnlocked()

    /**
     * Resolves credentials based on selected UUIDs.
     * Delegates to ResolveCloneCredentialUseCase.
     */
    suspend fun resolveCredentials(
        selectedCredentialUuid: String?,
        selectedSshKeyUuid: String?,
        httpsCredentials: List<HttpsCredential>,
        sshKeys: List<SshKey>,
        remoteUrl: String? = null
    ): CloneCredential? {
        return resolveCloneCredentialUseCase(
            selectedCredentialUuid,
            selectedSshKeyUuid,
            httpsCredentials,
            sshKeys,
            remoteUrl
        )
    }
}
