package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository

class CommitChangesUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String, message: String): Result<String> {
        if (message.isBlank()) {
            return Result.failure(IllegalArgumentException("Commit message cannot be empty"))
        }
        return gitRepository.commit(repoPath, message.trim())
    }
}
