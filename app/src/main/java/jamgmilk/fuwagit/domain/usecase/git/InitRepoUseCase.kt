package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository

class InitRepoUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<String> {
        return gitRepository.initRepo(repoPath)
    }
}
