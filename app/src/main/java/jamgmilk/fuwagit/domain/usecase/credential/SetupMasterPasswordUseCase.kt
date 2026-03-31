package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class SetupMasterPasswordUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(password: String, confirmPassword: String, hint: String?): AppResult<Unit> {
        if (password != confirmPassword) {
            return AppResult.Error(AppException.PasswordMismatch())
        }
        if (password.length < 6) {
            return AppResult.Error(AppException.Unknown("Password must be at least 6 characters"))
        }
        return repository.setupMasterPassword(password, hint)
    }
}
