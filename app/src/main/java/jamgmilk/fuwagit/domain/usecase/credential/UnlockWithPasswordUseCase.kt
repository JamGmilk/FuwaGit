package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class UnlockWithPasswordUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(password: String): AppResult<Unit> {
        return repository.unlockWithPassword(password)
    }
}
