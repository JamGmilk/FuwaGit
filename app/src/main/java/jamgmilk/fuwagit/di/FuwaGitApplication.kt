package jamgmilk.fuwagit.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jamgmilk.fuwagit.data.biometric.BiometricAuthManager
import jamgmilk.fuwagit.data.biometric.BiometricAuthManagerHost
import javax.inject.Inject

@HiltAndroidApp
class FuwaGitApplication : Application(), BiometricAuthManagerHost {

    @Inject
    lateinit var _biometricAuthManager: BiometricAuthManager

    override val biometricAuthManager: BiometricAuthManager
        get() = _biometricAuthManager
}
