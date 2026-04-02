package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class GetSshKeysUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(): AppResult<List<SshKey>> {
        return repository.getAllSshKeys()
    }
}

class AddSshKeyUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String?,
        fingerprint: String
    ): AppResult<String> {
        return repository.addSshKey(name, type, publicKey, privateKey, passphrase, fingerprint)
    }
}

class DeleteSshKeyUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(uuid: String): AppResult<Unit> {
        return repository.deleteSshKey(uuid)
    }
}

class GetSshPrivateKeyUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(uuid: String): AppResult<String> {
        return repository.getSshPrivateKey(uuid)
    }
}

class GetSshPassphraseUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(uuid: String): AppResult<String?> {
        return repository.getSshPassphrase(uuid)
    }
}
