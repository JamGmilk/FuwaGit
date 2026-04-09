package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import jamgmilk.fuwagit.domain.model.git.GitResetMode

interface CommitRepository {
    suspend fun getCommitHistory(repoPath: String, maxCount: Int): AppResult<List<GitCommit>>
    suspend fun getCommitFileChanges(repoPath: String, commitHash: String): AppResult<GitCommitDetail>
    suspend fun commit(repoPath: String, message: String): AppResult<String>
    suspend fun reset(repoPath: String, commitHash: String, mode: GitResetMode): AppResult<String>
}