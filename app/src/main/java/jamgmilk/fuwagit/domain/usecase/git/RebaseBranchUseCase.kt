package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository

class RebaseBranchUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, branchName: String): Result<Unit> {
        return gitRepository.rebaseBranch(repoPath, branchName)
    }
}
