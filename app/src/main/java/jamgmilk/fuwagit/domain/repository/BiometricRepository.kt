package jamgmilk.fuwagit.domain.repository

import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.result.AppResult
import javax.crypto.SecretKey

interface BiometricRepository {
    fun canAuthenticate(): Boolean

    suspend fun enableBiometric(
        activity: FragmentActivity,
        masterKey: SecretKey,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): AppResult<Unit>

    suspend fun unlockWithBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): AppResult<SecretKey>

    suspend fun disableBiometric(): AppResult<Unit>
}