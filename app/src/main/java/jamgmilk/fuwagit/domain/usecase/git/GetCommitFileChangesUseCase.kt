package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import jamgmilk.fuwagit.domain.repository.CommitRepository
import javax.inject.Inject

class GetCommitFileChangesUseCase @Inject constructor(
    private val repository: CommitRepository
) {
    suspend operator fun invoke(repoPath: String, commitHash: String): AppResult<GitCommitDetail> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        if (commitHash.isBlank()) {
            return AppResult.Error(AppException.Validation("Commit hash cannot be empty"))
        }
        return repository.getCommitFileChanges(repoPath, commitHash)
    }
}
