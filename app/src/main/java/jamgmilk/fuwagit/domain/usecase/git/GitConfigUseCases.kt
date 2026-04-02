package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.data.jgit.GitConfigManager
import javax.inject.Inject

/**
 * 将用户配置应用到指定仓库
 */
class ApplyGitConfigToRepo @Inject constructor(
    private val gitConfigManager: GitConfigManager
) {
    suspend operator fun invoke(repoPath: String, name: String, email: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (name.isBlank() && email.isBlank()) {
            return Result.failure(IllegalArgumentException("Name or email must be provided"))
        }
        
        return gitConfigManager.setRepoUserConfig(repoPath, name, email)
    }
}

/**
 * 将用户配置应用到 global git config
 */
class ApplyGitConfigToGlobal @Inject constructor(
    private val gitConfigManager: GitConfigManager
) {
    suspend operator fun invoke(name: String, email: String): Result<Unit> {
        if (name.isBlank() && email.isBlank()) {
            return Result.failure(IllegalArgumentException("Name or email must be provided"))
        }
        
        return gitConfigManager.setGlobalUserConfig(name, email)
    }
}

/**
 * 将用户配置应用到所有已知仓库
 */
class ApplyGitConfigToAllRepos @Inject constructor(
    private val gitConfigManager: GitConfigManager,
    private val applyGitConfigToRepo: ApplyGitConfigToRepo,
    private val applyGitConfigToGlobal: ApplyGitConfigToGlobal
) {
    suspend operator fun invoke(
        repoPaths: List<String>,
        name: String,
        email: String,
        alsoApplyToGlobal: Boolean = false
    ): ApplyToAllReposResult {
        val results = mutableMapOf<String, Result<Unit>>()
        var successCount = 0
        var failureCount = 0
        
        // 先应用到 global
        if (alsoApplyToGlobal) {
            applyGitConfigToGlobal(name, email)
        }
        
        // 然后应用到所有仓库
        repoPaths.forEach { repoPath ->
            val result = applyGitConfigToRepo(repoPath, name, email)
            results[repoPath] = result
            if (result.isSuccess) {
                successCount++
            } else {
                failureCount++
            }
        }
        
        return ApplyToAllReposResult(
            results = results,
            successCount = successCount,
            failureCount = failureCount,
            totalCount = repoPaths.size
        )
    }
}

/**
 * 应用到所有仓库的结果
 */
data class ApplyToAllReposResult(
    val results: Map<String, Result<Unit>>,
    val successCount: Int,
    val failureCount: Int,
    val totalCount: Int
) {
    val allSuccess: Boolean get() = failureCount == 0 && successCount > 0
    val successRate: Float get() = if (totalCount > 0) successCount.toFloat() / totalCount else 0f
}

/**
 * 移除仓库本地配置（使用 global 配置）
 */
class RemoveRepoLocalConfig @Inject constructor(
    private val gitConfigManager: GitConfigManager
) {
    suspend operator fun invoke(repoPath: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        
        return gitConfigManager.removeRepoLocalUserConfig(repoPath)
    }
}

/**
 * 获取仓库当前生效的用户配置
 */
class GetEffectiveUserConfig @Inject constructor(
    private val gitConfigManager: GitConfigManager
) {
    suspend operator fun invoke(repoPath: String): Pair<String?, String?> {
        return gitConfigManager.getEffectiveUserConfig(repoPath)
    }
}

/**
 * 获取 global 用户配置
 */
class GetGlobalUserConfig @Inject constructor(
    private val gitConfigManager: GitConfigManager
) {
    suspend operator fun invoke(): Pair<String?, String?> {
        return Pair(
            gitConfigManager.getGlobalUserName(),
            gitConfigManager.getGlobalUserEmail()
        )
    }
}
