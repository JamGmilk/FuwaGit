package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class GetRepoInfoUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(localPath: String): Map<String, String> {
        return repository.getRepoInfo(localPath)
    }
}
