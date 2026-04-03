package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

/**
 * Facade for Git staging operations.
 * Aggregates stage/unstage operations to reduce UseCase count.
 */
class StageUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend fun all(repoPath: String): Result<String> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        return repository.stageAll(repoPath)
    }

    suspend fun file(repoPath: String, filePath: String): Result<Unit> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        if (filePath.isBlank()) return Result.failure(IllegalArgumentException("File path cannot be empty"))
        return repository.stageFile(repoPath, filePath)
    }

    suspend fun unstageAll(repoPath: String): Result<String> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        return repository.unstageAll(repoPath)
    }

    suspend fun unstageFile(repoPath: String, filePath: String): Result<Unit> {
        if (repoPath.isBlank()) return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        if (filePath.isBlank()) return Result.failure(IllegalArgumentException("File path cannot be empty"))
        return repository.unstageFile(repoPath, filePath)
    }
}
