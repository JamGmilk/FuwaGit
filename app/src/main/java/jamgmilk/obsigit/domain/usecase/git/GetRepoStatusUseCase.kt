package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.domain.model.GitRepoStatus
import jamgmilk.obsigit.domain.repository.GitRepository

class GetRepoStatusUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<GitRepoStatus> {
        return gitRepository.getStatus(repoPath)
    }
}
