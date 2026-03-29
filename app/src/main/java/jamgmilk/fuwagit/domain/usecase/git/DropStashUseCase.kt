package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class DropStashUseCase @Inject constructor(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, stashIndex: Int): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (stashIndex < 0) {
            return Result.failure(IllegalArgumentException("Invalid stash index"))
        }
        return gitRepository.dropStash(repoPath, stashIndex)
    }
}
