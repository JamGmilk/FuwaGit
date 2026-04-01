package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class ExportCredentialsUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(): AppResult<String> = repository.exportCredentials()
}

class ImportCredentialsUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(jsonData: String): AppResult<Unit> = repository.importCredentials(jsonData)
}
