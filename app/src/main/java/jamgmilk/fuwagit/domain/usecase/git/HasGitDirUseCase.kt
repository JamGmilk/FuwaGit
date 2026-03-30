package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class HasGitDirUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(path: String?): Boolean {
        return repository.hasGitDir(path)
    }
}
