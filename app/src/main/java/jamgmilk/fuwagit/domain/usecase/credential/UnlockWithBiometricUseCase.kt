package jamgmilk.fuwagit.domain.usecase.credential

import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.data.local.security.MasterKeyManager
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class UnlockWithBiometricUseCase @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val masterKeyManager: MasterKeyManager
) {
    operator fun invoke(activity: FragmentActivity, onResult: (AppResult<Unit>) -> Unit) {
        masterKeyManager.unlockWithBiometric(
            activity = activity,
            onSuccess = { key ->
                credentialRepository.setMasterKey(key)
                onResult(AppResult.Success(Unit))
            },
            onError = { message ->
                onResult(AppResult.Error(jamgmilk.fuwagit.core.result.AppException.BiometricError(message)))
            }
        )
    }
}
