package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.GitRepoStatus
import jamgmilk.fuwagit.domain.repository.GitRepository

class CheckRepoStatusUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<GitRepoStatus> {
        return gitRepository.getStatus(repoPath)
    }
}
