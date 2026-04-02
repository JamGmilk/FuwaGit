package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class GetCommitFileChangesUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, commitHash: String): Result<GitCommitDetail> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (commitHash.isBlank()) {
            return Result.failure(IllegalArgumentException("Commit hash cannot be empty"))
        }
        return repository.getCommitFileChanges(repoPath, commitHash)
    }
}
