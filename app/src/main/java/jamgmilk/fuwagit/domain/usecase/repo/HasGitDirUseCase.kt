package jamgmilk.fuwagit.domain.usecase.repo

import jamgmilk.fuwagit.domain.repository.GitRepository

class HasGitDirUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(path: String?): Boolean {
        return gitRepository.hasGitDir(path)
    }
}
