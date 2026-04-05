package jamgmilk.fuwagit.domain.usecase.repo

import jamgmilk.fuwagit.domain.repository.RepoRepository
import jamgmilk.fuwagit.domain.usecase.git.HasGitDirUseCase
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 验证仓库路径并更新持久化存储。
 * 属于 Domain 层，负责业务逻辑。
 */
@Singleton
class ValidateRepoUseCase @Inject constructor(
    private val repoRepository: RepoRepository,
    private val hasGitDirUseCase: HasGitDirUseCase
) {
    suspend operator fun invoke(path: String?): ValidationResult {
        if (path == null) {
            repoRepository.setCurrentRepo(null)
            return ValidationResult.Cleared
        }

        val file = File(path)
        val name = file.name

        return when {
            !file.exists() -> {
                repoRepository.setCurrentRepo(null)
                ValidationResult.Error("Path does not exist")
            }
            !hasGitDirUseCase(path) -> {
                repoRepository.setCurrentRepo(null)
                ValidationResult.Error("Not a git repository")
            }
            else -> {
                repoRepository.setCurrentRepo(path)
                repoRepository.updateLastAccessed(path)
                ValidationResult.Success(path, name)
            }
        }
    }

    sealed interface ValidationResult {
        data class Success(val path: String, val name: String) : ValidationResult
        data class Error(val message: String) : ValidationResult
        object Cleared : ValidationResult
    }
}
