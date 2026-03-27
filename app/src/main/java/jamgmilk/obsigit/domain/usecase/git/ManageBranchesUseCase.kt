package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.domain.model.GitBranch
import jamgmilk.obsigit.domain.repository.GitRepository

class ManageBranchesUseCase(
    private val gitRepository: GitRepository
) {
    suspend fun getBranches(repoPath: String): Result<List<GitBranch>> {
        return gitRepository.getBranches(repoPath)
    }
    
    suspend fun checkout(repoPath: String, branchName: String): Result<Unit> {
        return gitRepository.checkoutBranch(repoPath, branchName)
    }
    
    suspend fun merge(repoPath: String, branchName: String): Result<Unit> {
        return gitRepository.mergeBranch(repoPath, branchName)
    }
    
    suspend fun rebase(repoPath: String, branchName: String): Result<Unit> {
        return gitRepository.rebaseBranch(repoPath, branchName)
    }
    
    suspend fun delete(repoPath: String, branchName: String, force: Boolean = false): Result<Unit> {
        return gitRepository.deleteBranch(repoPath, branchName, force)
    }
}
