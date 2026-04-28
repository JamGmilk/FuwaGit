package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.core.util.UrlUtils
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves credentials for Git operations.
 *
 * This UseCase encapsulates the logic for selecting and preparing credentials
 * based on user selection or auto-selection fallback.
 */
@Singleton
class ResolveCloneCredentialUseCase @Inject constructor(
    private val getHttpsPasswordUseCase: GetHttpsPasswordUseCase,
    private val getSshPrivateKeyUseCase: GetSshPrivateKeyUseCase,
    private val getSshPassphraseUseCase: GetSshPassphraseUseCase
) {
    suspend operator fun invoke(
        selectedCredentialUuid: String?,
        selectedSshKeyUuid: String?,
        httpsCredentials: List<HttpsCredential>,
        sshKeys: List<SshKey>,
        remoteUrl: String? = null
    ): CloneCredential? {
        return when {
            selectedCredentialUuid != null -> {
                resolveHttpsCredential(selectedCredentialUuid, httpsCredentials)
            }
            selectedSshKeyUuid != null -> {
                resolveSshCredential(selectedSshKeyUuid)
            }
            else -> {
                resolveAutoSelectCredential(httpsCredentials, sshKeys, remoteUrl)
            }
        }
    }

    private suspend fun resolveHttpsCredential(
        uuid: String,
        httpsCredentials: List<HttpsCredential>
    ): CloneCredential? {
        val cred = httpsCredentials.find { it.uuid == uuid } ?: return null
        val password = getHttpsPasswordUseCase(uuid).getOrNull() ?: return null
        return CloneCredential.Https(cred.username, password)
    }

    private suspend fun resolveSshCredential(
        uuid: String
    ): CloneCredential? {
        val privateKey = getSshPrivateKeyUseCase(uuid).getOrNull() ?: return null
        val passphrase = getSshPassphraseUseCase(uuid).getOrNull()
        return CloneCredential.Ssh(privateKey, passphrase)
    }

    private suspend fun resolveAutoSelectCredential(
        httpsCredentials: List<HttpsCredential>,
        sshKeys: List<SshKey>,
        remoteUrl: String? = null
    ): CloneCredential? {
        if (httpsCredentials.isNotEmpty()) {
            if (remoteUrl != null) {
                val remoteHost = UrlUtils.extractHost(remoteUrl)
                if (remoteHost != null) {
                    val matched = httpsCredentials.find { cred ->
                        isHostMatch(cred.host, remoteHost)
                    }
                    if (matched != null) {
                        return resolveHttpsCredential(matched.uuid, httpsCredentials)
                    }
                }
            }
            return resolveHttpsCredential(httpsCredentials.first().uuid, httpsCredentials)
        }

        if (sshKeys.isNotEmpty()) {
            return resolveSshCredential(sshKeys.first().uuid)
        }

        return null
    }

    private fun isHostMatch(credHost: String, remoteHost: String): Boolean {
        val normalizedCredHost = credHost.lowercase()
        val normalizedRemoteHost = remoteHost.lowercase()
        return normalizedCredHost == normalizedRemoteHost ||
               normalizedRemoteHost.endsWith(".$normalizedCredHost")
    }
}
