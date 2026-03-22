package jamgmilk.obsigit.ui

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File

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

    fun defaultDocumentsTreeUri(): Uri? {
        return runCatching {
            DocumentsContract.buildTreeDocumentUri(
                "com.android.externalstorage.documents",
                "primary:Documents"
            )
        }.getOrNull()
    }

    fun syncSafTreeToLocal(context: Context, uriText: String, localDir: File) {
        val tree = DocumentFile.fromTreeUri(context, uriText.toUri()) ?: return
        if (!localDir.exists()) {
            localDir.mkdirs()
        }
        clearLocalDirectoryContents(localDir)
        copySafDirectoryToLocal(context, tree, localDir)
    }

    fun syncLocalToSafTree(context: Context, localDir: File, uriText: String) {
        val tree = DocumentFile.fromTreeUri(context, uriText.toUri()) ?: return
        clearSafDirectoryContents(tree)
        copyLocalDirectoryToSaf(context, localDir, tree)
    }

    private fun clearLocalDirectoryContents(localDir: File) {
        localDir.listFiles()?.forEach { entry ->
            if (entry.isDirectory) {
                entry.deleteRecursively()
            } else {
                entry.delete()
            }
        }
    }

    private fun clearSafDirectoryContents(tree: DocumentFile) {
        tree.listFiles().forEach { child ->
            child.delete()
        }
    }

    private fun copySafDirectoryToLocal(context: Context, sourceDir: DocumentFile, targetDir: File) {
        sourceDir.listFiles().forEach { child ->
            val name = child.name ?: return@forEach
            val target = File(targetDir, name)

            if (child.isDirectory) {
                if (!target.exists()) {
                    target.mkdirs()
                }
                copySafDirectoryToLocal(context, child, target)
            } else if (child.isFile) {
                context.contentResolver.openInputStream(child.uri)?.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun copyLocalDirectoryToSaf(context: Context, sourceDir: File, targetDir: DocumentFile) {
        sourceDir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                val targetChildDir = targetDir.createDirectory(child.name)
                if (targetChildDir != null) {
                    copyLocalDirectoryToSaf(context, child, targetChildDir)
                }
            } else {
                val mimeType = guessMimeType(child.name)
                val targetFile = targetDir.createFile(mimeType, child.name) ?: return@forEach
                context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { output ->
                    child.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun guessMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }
}
