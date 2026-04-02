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
    operator fun invoke(activity: FragmentActivity): AppResult<Unit> {
        val masterKey = credentialRepository.getCachedMasterKey()
            ?: return AppResult.Error(jamgmilk.fuwagit.core.result.AppException.MasterKeyNotUnlocked())

        var result: AppResult<Unit> = AppResult.Error(jamgmilk.fuwagit.core.result.AppException.Unknown(""))

        masterKeyManager.enableBiometric(
            activity = activity,
            masterKey = masterKey,
            onSuccess = {
                result = AppResult.Success(Unit)
            },
            onError = { message ->
                result = AppResult.Error(jamgmilk.fuwagit.core.result.AppException.BiometricError(message))
            }
        )

        return result
    }
}
