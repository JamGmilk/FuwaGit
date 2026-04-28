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
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): AppResult<Unit> {
        val masterKey = credentialRepository.getCachedMasterKey()
            ?: return AppResult.Error(AppException.MasterKeyNotUnlocked())

        return biometricRepository.enableBiometric(
            activity = activity,
            masterKey = masterKey,
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText
        )
    }
}
