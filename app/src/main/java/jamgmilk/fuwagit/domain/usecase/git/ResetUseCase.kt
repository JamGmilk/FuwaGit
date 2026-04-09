package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.GitResetMode
import jamgmilk.fuwagit.domain.repository.CommitRepository
import javax.inject.Inject

class ResetUseCase @Inject constructor(
    private val repository: CommitRepository
) {
    suspend operator fun invoke(
        repoPath: String,
        commitHash: String,
        mode: GitResetMode
    ): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (commitHash.isBlank()) {
            return AppResult.Error(AppException.Validation("Commit hash cannot be empty"))
        }
        return repository.reset(repoPath, commitHash, mode)
    }
}
