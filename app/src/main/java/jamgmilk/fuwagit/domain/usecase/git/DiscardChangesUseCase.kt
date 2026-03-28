package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository

class DiscardChangesUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, filePath: String): Result<Unit> {
        return gitRepository.discardChanges(repoPath, filePath)
    }
}
