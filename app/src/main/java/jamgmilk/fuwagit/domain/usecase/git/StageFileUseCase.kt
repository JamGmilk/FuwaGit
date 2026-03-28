package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository

class StageFileUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String, filePath: String): Result<Unit> {
        return gitRepository.stageFile(repoPath, filePath)
    }
}
