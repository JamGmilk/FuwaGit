package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.repository.StatusRepository
import javax.inject.Inject

class StageUseCase @Inject constructor(
    private val repository: StatusRepository
) {
    suspend fun all(repoPath: String): AppResult<String> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        return repository.stageAll(repoPath)
    }

    suspend fun file(repoPath: String, filePath: String): AppResult<Unit> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        if (filePath.isBlank()) return AppResult.Error(AppException.Validation("File path cannot be empty"))
        return repository.stageFile(repoPath, filePath)
    }

    suspend fun unstageAll(repoPath: String): AppResult<String> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        return repository.unstageAll(repoPath)
    }

    suspend fun unstageFile(repoPath: String, filePath: String): AppResult<Unit> {
        if (repoPath.isBlank()) return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        if (filePath.isBlank()) return AppResult.Error(AppException.Validation("File path cannot be empty"))
        return repository.unstageFile(repoPath, filePath)
    }
}
