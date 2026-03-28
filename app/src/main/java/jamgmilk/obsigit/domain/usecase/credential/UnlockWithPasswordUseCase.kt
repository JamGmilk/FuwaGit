package jamgmilk.obsigit.domain.usecase.credential

import jamgmilk.obsigit.domain.model.AppException
import jamgmilk.obsigit.domain.model.AppResult
import jamgmilk.obsigit.domain.repository.CredentialRepository

class UnlockWithPasswordUseCase(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(password: String): AppResult<Unit> {
        if (password.isBlank()) {
            return AppResult.Error(AppException.Unknown("Password cannot be empty"))
        }
        return repository.unlockWithPassword(password).map { }
    }
}
