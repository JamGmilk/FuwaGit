package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.data.source.JGitDataSource
import jamgmilk.obsigit.domain.model.GitFileStatus
import java.io.File

class GetWorkspaceStatusUseCase {
    
    suspend operator fun invoke(repoPath: String): Result<List<GitFileStatus>> {
        return try {
            val dir = File(repoPath)
            val files = JGitDataSource.withGitLock { 
                JGitDataSource.getDetailedStatus(dir) 
            }
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
