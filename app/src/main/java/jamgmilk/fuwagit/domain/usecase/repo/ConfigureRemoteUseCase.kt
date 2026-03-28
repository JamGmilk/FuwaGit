package jamgmilk.fuwagit.domain.usecase.repo

import jamgmilk.fuwagit.domain.repository.GitRepository

class ConfigureRemoteUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(localPath: String, name: String, url: String): Result<String> {
        return gitRepository.configureRemote(localPath, name, url)
    }
}
