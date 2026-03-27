package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.domain.repository.GitRepository

class PushChangesUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<String> {
        return gitRepository.push(repoPath)
    }
}
