package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.git.FileDiff

interface DiffRepository {
    suspend fun getWorkingTreeDiff(repoPath: String, filePath: String): AppResult<FileDiff>
    suspend fun getStagedDiff(repoPath: String, filePath: String): AppResult<FileDiff>
    suspend fun getCommitFileDiff(
        repoPath: String,
        filePath: String,
        oldCommit: String,
        newCommit: String
    ): AppResult<FileDiff>
    suspend fun getCommitDiff(
        repoPath: String,
        oldCommit: String,
        newCommit: String
    ): AppResult<List<FileDiff>>
    suspend fun getFileContent(
        repoPath: String,
        filePath: String,
        commitHash: String? = null
    ): AppResult<String>
}