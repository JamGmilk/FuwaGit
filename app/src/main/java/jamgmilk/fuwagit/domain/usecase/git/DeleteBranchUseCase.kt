package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository

class DeleteBranchUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, branchName: String, force: Boolean = false): Result<Unit> {
        return gitRepository.deleteBranch(repoPath, branchName, force)
    }
}
