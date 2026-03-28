package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.GitBranch
import jamgmilk.fuwagit.domain.repository.GitRepository

class GetBranchesUseCase(
    private val gitRepository: GitRepository
) {
    
    suspend operator fun invoke(repoPath: String): Result<List<GitBranch>> {
        return gitRepository.getBranches(repoPath)
    }
}
