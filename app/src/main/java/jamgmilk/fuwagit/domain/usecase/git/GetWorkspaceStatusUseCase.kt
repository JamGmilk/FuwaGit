package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.GitRepoStatus
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class GetWorkspaceStatusUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): AppResult<GitRepoStatus> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.getStatus(repoPath)
    }
}
