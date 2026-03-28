package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.data.source.JGitDataSource
import jamgmilk.obsigit.domain.model.GitBranch
import java.io.File

class GetBranchesUseCase {
    
    suspend operator fun invoke(repoPath: String): Result<List<GitBranch>> {
        return try {
            val dir = File(repoPath)
            val branches = JGitDataSource.withGitLock { 
                JGitDataSource.getBranches(dir) 
            }
            Result.success(branches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
