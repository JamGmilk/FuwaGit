package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.domain.model.AppException
import jamgmilk.fuwagit.domain.model.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository

class AddSshKeyUseCase(
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
        if (name.isBlank()) {
            return AppResult.Error(AppException.Unknown("Name cannot be empty"))
        }
        if (publicKey.isBlank()) {
            return AppResult.Error(AppException.Unknown("Public key cannot be empty"))
        }
        if (privateKey.isBlank()) {
            return AppResult.Error(AppException.Unknown("Private key cannot be empty"))
        }
        if (fingerprint.isBlank()) {
            return AppResult.Error(AppException.Unknown("Fingerprint cannot be empty"))
        }
        return repository.addSshKey(name, type, publicKey, privateKey, passphrase, fingerprint)
    }
}
