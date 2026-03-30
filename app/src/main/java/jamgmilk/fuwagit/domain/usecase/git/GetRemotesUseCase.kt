package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class GetRemotesUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<List<GitRemote>> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.getRemotes(repoPath)
    }
}
