package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.git.GitBranch

interface BranchRepository {
    suspend fun getBranches(repoPath: String): AppResult<List<GitBranch>>
    suspend fun checkoutBranch(repoPath: String, branchName: String): AppResult<Unit>
    suspend fun createBranch(repoPath: String, branchName: String): AppResult<Unit>
    suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): AppResult<Unit>
    suspend fun renameBranch(repoPath: String, oldName: String, newName: String): AppResult<String>
}