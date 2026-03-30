package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class GetRemoteUrlUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(localPath: String, name: String = "origin"): String? {
        return repository.getRemoteUrl(localPath, name)
    }
}
