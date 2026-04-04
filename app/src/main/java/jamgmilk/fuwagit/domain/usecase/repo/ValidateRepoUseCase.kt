package jamgmilk.fuwagit.domain.usecase.repo

import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.domain.usecase.git.HasGitDirUseCase
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 楠岃瘉浠撳簱璺緞骞舵洿鏂版寔涔呭寲瀛樺偍銆?
 * 灞炰簬 Domain 灞傦紝璐熻矗涓氬姟閫昏緫鍜屾暟鎹啓鍏ャ€?
 */
@Singleton
class ValidateRepoUseCase @Inject constructor(
    private val repoDataStore: RepoDataStore,
    private val hasGitDirUseCase: HasGitDirUseCase
) {
    suspend operator fun invoke(path: String?): ValidationResult {
        if (path == null) {
            repoDataStore.setCurrentRepo(null)
            return ValidationResult.Cleared
        }

        val file = File(path)
        val name = file.name

        return when {
            !file.exists() -> {
                repoDataStore.setCurrentRepo(null)
                ValidationResult.Error("Path does not exist")
            }
            !hasGitDirUseCase(path) -> {
                repoDataStore.setCurrentRepo(null)
                ValidationResult.Error("Not a git repository")
            }
            else -> {
                repoDataStore.setCurrentRepo(path)
                repoDataStore.updateLastAccessed(path)
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
