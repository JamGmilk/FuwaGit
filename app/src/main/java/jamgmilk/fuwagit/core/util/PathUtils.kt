package jamgmilk.fuwagit.core.util

import android.os.Environment
import java.nio.file.Paths

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
        return Paths.get(path).fileName.toString()
    }
}
