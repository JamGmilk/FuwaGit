package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.FileDiff
import jamgmilk.fuwagit.domain.repository.DiffRepository
import javax.inject.Inject

class DiffUseCase @Inject constructor(
    private val repository: DiffRepository
) {
    suspend fun getWorkingTreeDiff(repoPath: String, filePath: String): AppResult<FileDiff> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (filePath.isBlank()) {
            return AppResult.Error(AppException.Validation("File path cannot be empty"))
        }
        return repository.getWorkingTreeDiff(repoPath, filePath)
    }

    suspend fun getStagedDiff(repoPath: String, filePath: String): AppResult<FileDiff> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (filePath.isBlank()) {
            return AppResult.Error(AppException.Validation("File path cannot be empty"))
        }
        return repository.getStagedDiff(repoPath, filePath)
    }

    suspend fun getCommitFileDiff(
        repoPath: String,
        filePath: String,
        oldCommit: String,
        newCommit: String = "HEAD"
    ): AppResult<FileDiff> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (filePath.isBlank()) {
            return AppResult.Error(AppException.Validation("File path cannot be empty"))
        }
        if (oldCommit.isBlank()) {
            return AppResult.Error(AppException.Validation("Old commit hash cannot be empty"))
        }
        return repository.getCommitFileDiff(repoPath, filePath, oldCommit, newCommit)
    }

    suspend fun getCommitDiff(
        repoPath: String,
        oldCommit: String,
        newCommit: String = "HEAD"
    ): AppResult<List<FileDiff>> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (oldCommit.isBlank()) {
            return AppResult.Error(AppException.Validation("Old commit hash cannot be empty"))
        }
        return repository.getCommitDiff(repoPath, oldCommit, newCommit)
    }

    suspend fun getFileContent(
        repoPath: String,
        filePath: String,
        commitHash: String? = null
    ): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (filePath.isBlank()) {
            return AppResult.Error(AppException.Validation("File path cannot be empty"))
        }
        return repository.getFileContent(repoPath, filePath, commitHash)
    }
}
