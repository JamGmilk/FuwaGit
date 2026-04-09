package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.GitTag
import jamgmilk.fuwagit.domain.repository.TagRepository
import javax.inject.Inject

class TagUseCase @Inject constructor(
    private val repository: TagRepository
) {
    suspend fun list(repoPath: String): AppResult<List<GitTag>> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.getTags(repoPath)
    }

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

    suspend fun delete(repoPath: String, tagName: String): AppResult<Unit> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return AppResult.Error(AppException.Validation("Tag name cannot be empty"))
        }
        return repository.deleteTag(repoPath, tagName)
    }

    suspend fun pushTag(repoPath: String, tagName: String, remoteName: String = "origin"): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return AppResult.Error(AppException.Validation("Tag name cannot be empty"))
        }
        return repository.pushTag(repoPath, tagName, remoteName)
    }

    suspend fun pushAllTags(repoPath: String, remoteName: String = "origin"): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.pushAllTags(repoPath, remoteName)
    }

    suspend fun checkoutTag(repoPath: String, tagName: String): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return AppResult.Error(AppException.Validation("Tag name cannot be empty"))
        }
        return repository.checkoutTag(repoPath, tagName)
    }

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
