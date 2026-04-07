package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.FileDiff
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

/**
 * Facade for Git diff operations.
 * Aggregates diff operations to reduce UseCase count.
 */
class DiffUseCase @Inject constructor(
    private val repository: GitRepository
) {
    /**
     * 获取工作区中文件的差异（未暂存的更改）
     */
    suspend fun getWorkingTreeDiff(repoPath: String, filePath: String): AppResult<FileDiff> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (filePath.isBlank()) {
            return AppResult.Error(AppException.Validation("File path cannot be empty"))
        }
        return repository.getWorkingTreeDiff(repoPath, filePath)
    }

    /**
     * 获取已暂存文件的差异（staged vs HEAD）
     */
    suspend fun getStagedDiff(repoPath: String, filePath: String): AppResult<FileDiff> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (filePath.isBlank()) {
            return AppResult.Error(AppException.Validation("File path cannot be empty"))
        }
        return repository.getStagedDiff(repoPath, filePath)
    }

    /**
     * 获取两个提交之间单个文件的差异
     */
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

    /**
     * 获取两个提交之间所有文件的差异摘要
     */
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

    /**
     * 获取文件内容
     */
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
