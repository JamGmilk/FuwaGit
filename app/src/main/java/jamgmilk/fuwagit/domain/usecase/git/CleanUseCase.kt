package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class CleanUseCase @Inject constructor(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, dryRun: Boolean = false): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.clean(repoPath, dryRun)
    }
}
