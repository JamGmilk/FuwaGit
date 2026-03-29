package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class RevertCommitUseCase @Inject constructor(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, commitHash: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (commitHash.isBlank()) {
            return Result.failure(IllegalArgumentException("Commit hash cannot be empty"))
        }
        return gitRepository.revertCommit(repoPath, commitHash)
    }
}
