package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository

class UnstageFileUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String, filePath: String): Result<Unit> {
        return gitRepository.unstageFile(repoPath, filePath)
    }
}
