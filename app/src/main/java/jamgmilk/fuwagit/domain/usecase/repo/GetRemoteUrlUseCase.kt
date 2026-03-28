package jamgmilk.fuwagit.domain.usecase.repo

import jamgmilk.fuwagit.domain.repository.GitRepository

class GetRemoteUrlUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(localPath: String, remoteName: String = "origin"): String? {
        return gitRepository.getRemoteUrl(localPath, remoteName)
    }
}
