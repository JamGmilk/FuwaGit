package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.domain.model.AppException
import jamgmilk.fuwagit.domain.model.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository

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
