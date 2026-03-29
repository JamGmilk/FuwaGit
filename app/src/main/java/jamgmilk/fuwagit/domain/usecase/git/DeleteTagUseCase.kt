package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class DeleteTagUseCase @Inject constructor(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(repoPath: String, tagName: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return Result.failure(IllegalArgumentException("Tag name cannot be empty"))
        }
        return gitRepository.deleteTag(repoPath, tagName)
    }
}
