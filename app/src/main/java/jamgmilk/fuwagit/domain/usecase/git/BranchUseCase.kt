package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.repository.BranchRepository
import javax.inject.Inject

class BranchUseCase @Inject constructor(
    private val repository: BranchRepository
) {
    suspend fun list(repoPath: String): AppResult<List<GitBranch>> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        return repository.getBranches(repoPath)
    }

    suspend fun create(repoPath: String, branchName: String): AppResult<Unit> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        if (branchName.isBlank()) return AppResult.Error(AppException.Validation("Branch name cannot be empty"))
        return repository.createBranch(repoPath, branchName)
    }

    suspend fun delete(repoPath: String, branchName: String, force: Boolean = false): AppResult<Unit> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        if (branchName.isBlank()) return AppResult.Error(AppException.Validation("Branch name cannot be empty"))
        return repository.deleteBranch(repoPath, branchName, force)
    }

    suspend fun rename(repoPath: String, oldName: String, newName: String): AppResult<String> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        if (oldName.isBlank() || newName.isBlank()) return AppResult.Error(AppException.Validation("Branch names cannot be empty"))
        return repository.renameBranch(repoPath, oldName, newName)
    }

    suspend fun checkout(repoPath: String, branchName: String): AppResult<Unit> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        if (branchName.isBlank()) return AppResult.Error(AppException.Validation("Branch name cannot be empty"))
        return repository.checkoutBranch(repoPath, branchName)
    }
}
