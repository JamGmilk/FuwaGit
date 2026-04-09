package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.CoreRepository
import javax.inject.Inject

class GetRepoInfoUseCase @Inject constructor(
    private val repository: CoreRepository
) {
    suspend operator fun invoke(localPath: String): Map<String, String> {
        return repository.getRepoInfo(localPath)
    }
}
