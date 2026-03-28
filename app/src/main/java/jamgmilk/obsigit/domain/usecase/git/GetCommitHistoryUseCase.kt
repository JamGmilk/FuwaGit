package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.domain.model.GitCommit
import jamgmilk.obsigit.domain.repository.GitRepository

class GetCommitHistoryUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String, maxCount: Int = 100): Result<List<GitCommit>> {
        return gitRepository.getCommitHistory(repoPath, maxCount)
    }
}
