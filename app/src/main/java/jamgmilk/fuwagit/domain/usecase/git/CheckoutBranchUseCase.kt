package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository

class CheckoutBranchUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, branchName: String): Result<Unit> {
        return gitRepository.checkoutBranch(repoPath, branchName)
    }
}
