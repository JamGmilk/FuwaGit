package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.domain.model.GitBranch
import jamgmilk.obsigit.domain.repository.GitRepository

class GetBranchesUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String): Result<List<GitBranch>> {
        return gitRepository.getBranches(repoPath)
    }
}
