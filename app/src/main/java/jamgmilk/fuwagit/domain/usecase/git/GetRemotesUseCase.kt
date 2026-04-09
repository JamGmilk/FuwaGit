package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.repository.RemoteRepository
import javax.inject.Inject

class GetRemotesUseCase @Inject constructor(
    private val repository: RemoteRepository
) {
    suspend operator fun invoke(repoPath: String): AppResult<List<GitRemote>> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }
        return repository.getRemotes(repoPath)
    }
}
