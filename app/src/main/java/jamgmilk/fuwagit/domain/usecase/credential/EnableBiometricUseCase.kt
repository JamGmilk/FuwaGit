package jamgmilk.fuwagit.domain.usecase.credential

import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.repository.BiometricRepository
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class EnableBiometricUseCase @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val biometricRepository: BiometricRepository
) {
    suspend operator fun invoke(
        activity: FragmentActivity,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        val masterKey = credentialRepository.getCachedMasterKey()
            ?: run {
                onResult(AppResult.Error(AppException.MasterKeyNotUnlocked()))
                return
            }

        biometricRepository.enableBiometric(
            activity = activity,
            masterKey = masterKey,
            onSuccess = {
                onResult(AppResult.Success(Unit))
            },
            onError = { message ->
                onResult(AppResult.Error(AppException.BiometricError(message)))
            }
        )
    }
}
