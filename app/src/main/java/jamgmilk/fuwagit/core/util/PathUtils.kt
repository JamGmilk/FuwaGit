package jamgmilk.fuwagit.core.util

import android.os.Environment
import java.io.File

object PathUtils {
    private val externalStoragePath: String by lazy {
        Environment.getExternalStorageDirectory().absolutePath
    }

    fun getShortPath(path: String): String {
        return if (path.startsWith(externalStoragePath)) {
            "/External${path.removePrefix(externalStoragePath)}"
        } else {
            path
        }
    }

    fun getExternalStorageDir(): String = externalStoragePath

    fun getFileName(path: String): String {
        return File(path).name
    }
}
