package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.ConfigRepository
import javax.inject.Inject

/**
 * Apply user config to specified repository
 */
class ApplyGitConfigToRepo @Inject constructor(
    private val configRepository: ConfigRepository
) {
    suspend operator fun invoke(repoPath: String, name: String, email: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (name.isBlank() && email.isBlank()) {
            return Result.failure(IllegalArgumentException("Name or email must be provided"))
        }

        return configRepository.setRepoUserConfig(repoPath, name, email)
    }
}

/**
 * Apply user config to global git config
 */
class ApplyGitConfigToGlobal @Inject constructor(
    private val configRepository: ConfigRepository
) {
    suspend operator fun invoke(name: String, email: String): Result<Unit> {
        if (name.isBlank() && email.isBlank()) {
            return Result.failure(IllegalArgumentException("Name or email must be provided"))
        }

        return configRepository.setGlobalUserConfig(name, email)
    }
}

/**
 * Apply user config to all known repositories
 */
class ApplyGitConfigToAllRepos @Inject constructor(
    private val configRepository: ConfigRepository,
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

        // Apply to global first
        if (alsoApplyToGlobal) {
            applyGitConfigToGlobal(name, email)
        }

        // Then apply to all repositories
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
 * Result of applying config to all repositories
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
 * Remove repository local config (use global config instead)
 */
class RemoveRepoLocalConfig @Inject constructor(
    private val configRepository: ConfigRepository
) {
    suspend operator fun invoke(repoPath: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }

        return configRepository.removeRepoUserConfig(repoPath)
    }
}

/**
 * Get currently effective user config for repository
 */
class GetEffectiveUserConfig @Inject constructor(
    private val configRepository: ConfigRepository
) {
    suspend operator fun invoke(repoPath: String): Pair<String?, String?> {
        return configRepository.getEffectiveUserConfig(repoPath)
    }
}

/**
 * Get global user config
 */
class GetGlobalUserConfig @Inject constructor(
    private val configRepository: ConfigRepository
) {
    suspend operator fun invoke(): Pair<String?, String?> {
        return Pair(
            configRepository.getGlobalUserName(),
            configRepository.getGlobalUserEmail()
        )
    }
}
