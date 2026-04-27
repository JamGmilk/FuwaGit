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
    suspend operator fun invoke(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): AppResult<Unit> {
        val result = biometricRepository.unlockWithBiometric(
            activity = activity,
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText
        )
        return when (result) {
            is AppResult.Success -> {
                credentialRepository.setMasterKeyFromBiometric(result.data)
                AppResult.Success(Unit)
            }
            is AppResult.Error -> {
                AppResult.Error(AppException.BiometricError(result.message ?: "Biometric error"))
            }
        }
    }
}
