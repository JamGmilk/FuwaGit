package jamgmilk.fuwagit.domain.model.repo

import jamgmilk.fuwagit.core.util.PathUtils
import kotlinx.serialization.Serializable

@Serializable
data class RepoData(
    val path: String,
    val alias: String? = null,
    val credentialId: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
) {
    val displayName: String
        get() = alias ?: PathUtils.getFileName(path)
}
