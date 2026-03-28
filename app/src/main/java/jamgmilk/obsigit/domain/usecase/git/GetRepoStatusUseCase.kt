package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.data.source.JGitDataSource
import jamgmilk.obsigit.data.source.RepoPathUtils
import jamgmilk.obsigit.domain.model.GitRepoStatus
import java.io.File

class GetRepoStatusUseCase {
    
    suspend operator fun invoke(repoPath: String): Result<GitRepoStatus> {
        return try {
            val dir = File(repoPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            
            val status = JGitDataSource.withGitLock { 
                JGitDataSource.readRepoStatus(dir) 
            }
            Result.success(GitRepoStatus(
                isGitRepo = status.isGitRepo,
                branch = "",
                hasUncommittedChanges = false,
                untrackedCount = 0,
                message = status.message
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
