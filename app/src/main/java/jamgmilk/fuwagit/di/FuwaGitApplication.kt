package jamgmilk.fuwagit.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jamgmilk.fuwagit.util.CrashLogManager

@HiltAndroidApp
class FuwaGitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogManager.init(this)
    }
}
