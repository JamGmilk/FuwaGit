package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import jamgmilk.fuwagit.domain.model.git.GitResetMode

/**
 * Interface for Git commit and history operations.
 */
interface GitCommitDataSource {
    fun getLog(repoPath: String, maxCount: Int): Result<List<GitCommit>>

    fun getCommitFileChanges(repoPath: String, commitHash: String): Result<GitCommitDetail>

    suspend fun commit(repoPath: String, message: String): Result<String>

    fun reset(repoPath: String, commitHash: String, mode: GitResetMode): Result<String>
}
