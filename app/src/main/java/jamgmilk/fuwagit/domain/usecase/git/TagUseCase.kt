package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.GitTag
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

/**
 * Facade for Git tag operations.
 * Aggregates tag operations to reduce UseCase count.
 */
class TagUseCase @Inject constructor(
    private val repository: GitRepository
) {
    /**
     * 列出所有标签
     */
    suspend fun list(repoPath: String): AppResult<List<GitTag>> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.getTags(repoPath)
    }

    /**
     * 创建轻量标签
     */
    suspend fun createLightweight(
        repoPath: String,
        tagName: String,
        commitHash: String? = null
    ): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return AppResult.Error(AppException.Validation("Tag name cannot be empty"))
        }
        if (!isValidTagName(tagName)) {
            return AppResult.Error(AppException.Validation("Invalid tag name. Tag names cannot contain spaces, ~, ^, :, ?, *, [, \\, or begin with a dot"))
        }
        return repository.createLightweightTag(repoPath, tagName, commitHash)
    }

    /**
     * 创建附注标签
     */
    suspend fun createAnnotated(
        repoPath: String,
        tagName: String,
        message: String,
        commitHash: String? = null
    ): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return AppResult.Error(AppException.Validation("Tag name cannot be empty"))
        }
        if (message.isBlank()) {
            return AppResult.Error(AppException.Validation("Tag message cannot be empty"))
        }
        if (!isValidTagName(tagName)) {
            return AppResult.Error(AppException.Validation("Invalid tag name. Tag names cannot contain spaces, ~, ^, :, ?, *, [, \\, or begin with a dot"))
        }
        return repository.createAnnotatedTag(repoPath, tagName, message, commitHash)
    }

    /**
     * 删除标签
     */
    suspend fun delete(repoPath: String, tagName: String): AppResult<Unit> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return AppResult.Error(AppException.Validation("Tag name cannot be empty"))
        }
        return repository.deleteTag(repoPath, tagName)
    }

    /**
     * 推送单个标签到远程
     */
    suspend fun pushTag(repoPath: String, tagName: String, remoteName: String = "origin"): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return AppResult.Error(AppException.Validation("Tag name cannot be empty"))
        }
        return repository.pushTag(repoPath, tagName, remoteName)
    }

    /**
     * 推送所有标签到远程
     */
    suspend fun pushAllTags(repoPath: String, remoteName: String = "origin"): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.pushAllTags(repoPath, remoteName)
    }

    /**
     * 检出标签（进入 detached HEAD 状态）
     */
    suspend fun checkoutTag(repoPath: String, tagName: String): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return AppResult.Error(AppException.Validation("Tag name cannot be empty"))
        }
        return repository.checkoutTag(repoPath, tagName)
    }

    /**
     * 验证标签名称是否合法
     * Git 标签名称规则：
     * - 不能包含空格、~、^、:、?、*、[、\
     * - 不能以 . 开头
     * - 不能以 / 结尾
     * - 不能包含连续的 /
     */
    private fun isValidTagName(tagName: String): Boolean {
        if (tagName.startsWith(".")) return false
        if (tagName.endsWith("/")) return false
        if (tagName.contains("..")) return false
        if (tagName.contains(" ")) return false
        if (tagName.contains("~")) return false
        if (tagName.contains("^")) return false
        if (tagName.contains(":")) return false
        if (tagName.contains("?")) return false
        if (tagName.contains("*")) return false
        if (tagName.contains("[")) return false
        if (tagName.contains("\\")) return false
        if (tagName.contains("@{")) return false
        return true
    }
}
