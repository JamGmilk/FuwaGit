package jamgmilk.obsigit.ui

import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.topjohnwu.superuser.Shell

internal object AppVaultOps {

    fun readOwner(path: String): String {
        if (!path.startsWith("/")) {
            return "Unknown (SAF)"
        }

        val escaped = path.replace("\"", "\\\"")
        val command = "stat -c '%U' \"$escaped\" 2>/dev/null || ls -ld \"$escaped\" 2>/dev/null | awk '{print \$3}'"
        val shellResult = Shell.cmd(command).exec()
        val owner = shellResult.out.firstOrNull()?.trim().orEmpty()
        if (shellResult.isSuccess && owner.isNotBlank()) {
            return owner
        }

        val rootCommand = command.replace("'", "'\\''")
        val rootShellResult = Shell.cmd("su -c '$rootCommand'").exec()
        val rootOwner = rootShellResult.out.firstOrNull()?.trim().orEmpty()
        return if (rootShellResult.isSuccess && rootOwner.isNotBlank()) rootOwner else "Unavailable"
    }

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

    fun pathToLocal(path: String): String? {
        return if (path.startsWith("/")) path else null
    }

    fun defaultDocumentsTreeUri(): Uri? {
        return runCatching {
            DocumentsContract.buildTreeDocumentUri(
                "com.android.externalstorage.documents",
                "primary:Documents"
            )
        }.getOrNull()
    }

    fun collectFolderCandidates(root: DocumentFile): List<DocumentFile> {
        val list = mutableListOf<DocumentFile>()
        if (root.isDirectory) {
            list += root
        }
        list += root.listFiles().filter { it.isDirectory }
        return list
    }
}
