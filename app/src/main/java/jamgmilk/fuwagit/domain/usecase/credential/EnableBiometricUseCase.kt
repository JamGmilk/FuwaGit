package jamgmilk.fuwagit.domain.usecase.credential

import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.data.local.security.MasterKeyManager
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class EnableBiometricUseCase @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val masterKeyManager: MasterKeyManager
) {
    suspend operator fun invoke(
        activity: FragmentActivity,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        val masterKey = credentialRepository.getCachedMasterKey()
            ?: run {
                onResult(AppResult.Error(jamgmilk.fuwagit.core.result.AppException.MasterKeyNotUnlocked()))
                return
            }

        masterKeyManager.enableBiometric(
            activity = activity,
            masterKey = masterKey,
            onSuccess = {
                onResult(AppResult.Success(Unit))
            },
            onError = { message ->
                onResult(AppResult.Error(jamgmilk.fuwagit.core.result.AppException.BiometricError(message)))
            }
        )
    }
}
