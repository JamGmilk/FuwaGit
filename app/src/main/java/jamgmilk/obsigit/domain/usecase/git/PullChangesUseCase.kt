package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.domain.model.PullResult
import jamgmilk.obsigit.domain.repository.GitRepository

class PullChangesUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<PullResult> {
        return gitRepository.pull(repoPath)
    }
}
