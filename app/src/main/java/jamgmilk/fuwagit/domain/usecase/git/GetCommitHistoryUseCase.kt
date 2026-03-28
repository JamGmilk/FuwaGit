package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.GitCommit
import jamgmilk.fuwagit.domain.repository.GitRepository

class GetCommitHistoryUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String, maxCount: Int = 100): Result<List<GitCommit>> {
        return gitRepository.getCommitHistory(repoPath, maxCount)
    }
}
