package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.git.GitTag

interface TagRepository {
    suspend fun getTags(repoPath: String): AppResult<List<GitTag>>
    suspend fun createLightweightTag(repoPath: String, tagName: String, commitHash: String? = null): AppResult<String>
    suspend fun createAnnotatedTag(
        repoPath: String,
        tagName: String,
        message: String,
        commitHash: String? = null
    ): AppResult<String>
    suspend fun deleteTag(repoPath: String, tagName: String): AppResult<Unit>
    suspend fun pushTag(repoPath: String, tagName: String, remoteName: String = "origin"): AppResult<String>
    suspend fun pushAllTags(repoPath: String, remoteName: String = "origin"): AppResult<String>
    suspend fun checkoutTag(repoPath: String, tagName: String): AppResult<String>
}