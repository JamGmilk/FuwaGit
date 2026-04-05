package jamgmilk.fuwagit.domain.usecase.credential

import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.repository.BiometricRepository
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class UnlockWithBiometricUseCase @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val biometricRepository: BiometricRepository
) {
    operator fun invoke(activity: FragmentActivity, onResult: (AppResult<Unit>) -> Unit) {
        biometricRepository.unlockWithBiometric(
            activity = activity,
            onSuccess = { key ->
                credentialRepository.setMasterKey(key)
                onResult(AppResult.Success(Unit))
            },
            onError = { message ->
                onResult(AppResult.Error(AppException.BiometricError(message)))
            }
        )
    }
}
