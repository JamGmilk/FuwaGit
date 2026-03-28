package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.data.source.JGitDataSource
import jamgmilk.obsigit.domain.model.GitCommit
import java.io.File

class GetCommitHistoryUseCase {
    
    suspend operator fun invoke(repoPath: String, maxCount: Int = 100): Result<List<GitCommit>> {
        return try {
            val dir = File(repoPath)
            val commits = JGitDataSource.withGitLock { 
                JGitDataSource.getLog(dir, maxCount) 
            }
            Result.success(commits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
