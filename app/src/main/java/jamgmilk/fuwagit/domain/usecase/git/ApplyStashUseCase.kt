package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class ApplyStashUseCase @Inject constructor(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(
        repoPath: String,
        stashIndex: Int,
        dropAfterApply: Boolean = false
    ): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (stashIndex < 0) {
            return Result.failure(IllegalArgumentException("Invalid stash index"))
        }
        return gitRepository.applyStash(repoPath, stashIndex, dropAfterApply)
    }
}
