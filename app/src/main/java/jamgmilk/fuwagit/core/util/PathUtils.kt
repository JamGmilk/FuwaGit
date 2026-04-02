package jamgmilk.fuwagit.core.util

import android.os.Environment
import java.io.File

object PathUtils {
    fun getShortPath(path: String): String {
        val externalStorageDirPrefix = Environment.getExternalStorageDirectory().absolutePath
        return if (path.startsWith(externalStorageDirPrefix)) {
            "/External${path.removePrefix(externalStorageDirPrefix)}"
        } else {
            path
        }
    }

    fun getFileName(path: String): String {
        return File(path).name
    }
}
