package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.domain.model.GitFileStatus
import jamgmilk.obsigit.domain.repository.GitRepository

class GetWorkspaceStatusUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String): Result<List<GitFileStatus>> {
        return gitRepository.getDetailedStatus(repoPath)
    }
}
