package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.data.source.JGitDataSource
import java.io.File

class StageFileUseCase {
    
    suspend operator fun invoke(repoPath: String, filePath: String): Result<Unit> {
        return try {
            val dir = File(repoPath)
            JGitDataSource.withGitLock { 
                JGitDataSource.stageFile(dir, filePath) 
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
