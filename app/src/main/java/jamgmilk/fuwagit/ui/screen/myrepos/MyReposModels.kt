package jamgmilk.fuwagit.ui.screen.myrepos

import androidx.compose.runtime.Stable
import jamgmilk.fuwagit.core.util.PathUtils
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.model.repo.RepoData
import java.util.Locale

@Stable
data class RepoFolderItem(
    val path: String,
    val alias: String,
    val isGitRepo: Boolean,
    val isRemote: Boolean,
    val isActive: Boolean,
    val lastModified: Long = 0L,
    val size: Long = 0L
) {
    val shortPath: String
        get() = PathUtils.getShortPath(path)

    val formattedSize: String
        get() {
            return when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
                else -> String.format(Locale.US, "%.1f GB", size / (1024.0 * 1024 * 1024))
            }
        }
}

@Stable
data class RepoUiState(
    val repoItems: List<RepoFolderItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val untrackedFilesForClean: List<String> = emptyList(),
    val cleanedFilesForResult: List<String> = emptyList(),
    val savedRepos: List<RepoData> = emptyList(),
    val repoSizes: Map<String, Long> = emptyMap(),
    val isCleanPreviewing: Boolean = false,
    val cleanMessage: String? = null,
    val httpsCredentials: List<HttpsCredential> = emptyList(),
    val sshKeys: List<SshKey> = emptyList()
)

data class HttpsCredentialItem(
    val uuid: String,
    val host: String,
    val username: String,
    val displayName: String
)

data class SshKeyItem(
    val uuid: String,
    val name: String,
    val fingerprint: String,
    val displayName: String
)
