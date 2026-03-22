package jamgmilk.obsigit.ui

import android.net.Uri
import android.provider.DocumentsContract

internal object AppVaultOps {

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
            "/storage/emulated/0/$rel"
        } else {
            "$volume:/$rel"
        }
    }

    fun shortDisplayPath(path: String): String {
        val normalized = path.trim()
        val prefix = "/storage/emulated/0/"
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
