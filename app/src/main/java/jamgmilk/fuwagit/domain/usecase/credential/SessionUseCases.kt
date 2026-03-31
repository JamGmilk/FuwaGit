package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class IsUnlockedUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    operator fun invoke(): Boolean = repository.isUnlocked()
}

class LockCredentialsUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    operator fun invoke() = repository.lock()
}

class IsMasterPasswordSetUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    operator fun invoke(): Boolean = repository.isMasterPasswordSet()
}

class IsBiometricEnabledUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    operator fun invoke(): Boolean = repository.isBiometricEnabled()
}

class GetMasterPasswordHintUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    operator fun invoke(): String? = repository.getMasterPasswordHint()
}

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

class EnableBiometricUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.enableBiometric()
}

class DisableBiometricUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.disableBiometric()
}
