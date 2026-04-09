package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.GitTagDataSource
import jamgmilk.fuwagit.domain.model.git.GitTag
import jamgmilk.fuwagit.domain.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TagRepositoryImpl @Inject constructor(
    private val tagDataSource: GitTagDataSource
) : TagRepository {

    override suspend fun getTags(repoPath: String): AppResult<List<GitTag>> =
        withContext(Dispatchers.IO) {
            tagDataSource.getTags(repoPath).toAppResult()
        }

    override suspend fun createLightweightTag(repoPath: String, tagName: String, commitHash: String?): AppResult<String> =
        withContext(Dispatchers.IO) {
            tagDataSource.createLightweightTag(repoPath, tagName, commitHash).toAppResult()
        }

    override suspend fun createAnnotatedTag(
        repoPath: String,
        tagName: String,
        message: String,
        commitHash: String?
    ): AppResult<String> =
        withContext(Dispatchers.IO) {
            tagDataSource.createAnnotatedTag(repoPath, tagName, message, commitHash).toAppResult()
        }

    override suspend fun deleteTag(repoPath: String, tagName: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            tagDataSource.deleteTag(repoPath, tagName).toAppResult()
        }

    override suspend fun pushTag(repoPath: String, tagName: String, remoteName: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            tagDataSource.pushTag(repoPath, tagName, remoteName).toAppResult()
        }

    override suspend fun pushAllTags(repoPath: String, remoteName: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            tagDataSource.pushAllTags(repoPath, remoteName).toAppResult()
        }

    override suspend fun checkoutTag(repoPath: String, tagName: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            tagDataSource.checkoutTag(repoPath, tagName).toAppResult()
        }
}