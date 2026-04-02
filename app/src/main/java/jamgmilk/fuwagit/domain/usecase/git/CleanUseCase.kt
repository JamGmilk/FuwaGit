package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.CleanResult
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class CleanUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, dryRun: Boolean = false): Result<CleanResult> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.clean(repoPath, dryRun)
    }
}
