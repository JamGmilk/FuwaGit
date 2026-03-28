package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository

class PullUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String): Result<String> {
        return gitRepository.pull(repoPath).map { it.message }
    }
}
