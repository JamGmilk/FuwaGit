package jamgmilk.fuwagit.domain.usecase.repo

import jamgmilk.fuwagit.domain.repository.GitRepository

class GetRepoInfoUseCase(
    private val gitRepository: GitRepository
) {
    suspend operator fun invoke(localPath: String): Map<String, String> {
        return try {
            gitRepository.getRepoInfo(localPath)
        } catch (e: Exception) {
            mapOf("Error" to (e.message ?: "Unknown error"))
        }
    }
}
