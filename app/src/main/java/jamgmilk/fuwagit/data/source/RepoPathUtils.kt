package jamgmilk.fuwagit.data.source

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object RepoPathUtils {

    val externalStorageDirPrefix: String = Environment.getExternalStorageDirectory().absolutePath

    fun readablePathFromUri(uri: Uri): String {
        val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            ?: runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return uri.toString()

        val pieces = docId.split(":", limit = 2)
        if (pieces.size != 2) {
            return uri.toString()
        }

        val volume = pieces[0]
        val rel = pieces[1]
        return if (volume.equals("primary", ignoreCase = true)) {
            "$externalStorageDirPrefix/$rel"
        } else {
            "$volume:/$rel"
        }
    }

    fun normalizeLocalPath(path: String): String {
        return path.trim().trimEnd('/')
    }

    fun ensureTrailingSlash(path: String): String {
        val trimmed = path.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    fun shortDisplayPath(path: File): String {
        return shortDisplayPath(path.absolutePath)
    }

    fun shortDisplayPath(path: String): String {
        val normalized = path.trim().trimEnd('/')
        val prefix = "$externalStorageDirPrefix/"
        return when {
            normalized.startsWith(prefix) -> {
                val relative = normalized.removePrefix(prefix)
                "/External/$relative"
            }
            normalized.startsWith("/storage/") -> {
                val relative = normalized.removePrefix("/storage/")
                "/Storage/$relative"
            }
            normalized.startsWith("/") -> "/Local$normalized"
            else -> normalized
        }
    }
}
