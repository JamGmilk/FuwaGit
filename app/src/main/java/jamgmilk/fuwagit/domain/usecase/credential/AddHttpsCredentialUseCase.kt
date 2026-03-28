package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.domain.model.AppException
import jamgmilk.fuwagit.domain.model.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository

class AddHttpsCredentialUseCase(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(
        host: String,
        username: String,
        password: String
    ): AppResult<String> {
        if (host.isBlank()) {
            return AppResult.Error(AppException.Unknown("Host cannot be empty"))
        }
        if (username.isBlank()) {
            return AppResult.Error(AppException.Unknown("Username cannot be empty"))
        }
        if (password.isBlank()) {
            return AppResult.Error(AppException.Unknown("Password cannot be empty"))
        }
        return repository.addHttpsCredential(host, username, password)
    }
}
