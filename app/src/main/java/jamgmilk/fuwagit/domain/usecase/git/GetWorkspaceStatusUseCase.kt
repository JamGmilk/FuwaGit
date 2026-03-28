package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.GitFileStatus
import jamgmilk.fuwagit.domain.repository.GitRepository

class GetWorkspaceStatusUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String): Result<List<GitFileStatus>> {
        return gitRepository.getDetailedStatus(repoPath)
    }
}
