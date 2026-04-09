package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult

interface CoreRepository {
    suspend fun initRepo(repoPath: String): AppResult<String>
    suspend fun hasGitDir(path: String?): Boolean
    suspend fun getRepoInfo(localPath: String): Map<String, String>
}