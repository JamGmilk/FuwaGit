package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.model.git.GitRepoStatus

interface StatusRepository {
    suspend fun getRepoStatus(repoPath: String): AppResult<GitRepoStatus>
    suspend fun getDetailedStatus(repoPath: String): AppResult<List<GitFileStatus>>
    suspend fun stageAll(repoPath: String): AppResult<String>
    suspend fun unstageAll(repoPath: String): AppResult<String>
    suspend fun stageFile(repoPath: String, path: String): AppResult<Unit>
    suspend fun unstageFile(repoPath: String, path: String): AppResult<Unit>
    suspend fun discardChanges(repoPath: String, path: String): AppResult<Unit>
}