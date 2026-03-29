package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.GitStash
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class StashListUseCase @Inject constructor(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<List<GitStash>> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.getStashList(repoPath)
    }
}
