package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.GitStatusDataSource
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.repository.BranchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BranchRepositoryImpl @Inject constructor(
    private val statusDataSource: GitStatusDataSource
) : BranchRepository {

    override suspend fun getBranches(repoPath: String): AppResult<List<GitBranch>> =
        withContext(Dispatchers.IO) {
            statusDataSource.getBranches(repoPath).toAppResult()
        }

    override suspend fun checkoutBranch(repoPath: String, branchName: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            statusDataSource.checkoutBranch(repoPath, branchName).toAppResult()
        }

    override suspend fun createBranch(repoPath: String, branchName: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            statusDataSource.createBranch(repoPath, branchName).toAppResult()
        }

    override suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            statusDataSource.deleteBranch(repoPath, branchName, force).toAppResult()
        }

    override suspend fun renameBranch(repoPath: String, oldName: String, newName: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            statusDataSource.renameBranch(repoPath, oldName, newName).toAppResult()
        }
}