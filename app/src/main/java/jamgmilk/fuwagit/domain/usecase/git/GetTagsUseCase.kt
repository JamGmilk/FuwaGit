package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.GitTag
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class GetTagsUseCase @Inject constructor(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String): Result<List<GitTag>> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return gitRepository.getTags(repoPath)
    }
}
