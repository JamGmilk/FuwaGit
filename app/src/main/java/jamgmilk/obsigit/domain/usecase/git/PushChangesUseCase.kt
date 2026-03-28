package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.data.source.JGitDataSource
import java.io.File

class PushChangesUseCase {
    
    suspend operator fun invoke(repoPath: String): Result<String> {
        return try {
            val dir = File(repoPath)
            val result = JGitDataSource.withGitLock { 
                JGitDataSource.push(dir) 
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
