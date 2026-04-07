package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.git.CleanResult
import javax.inject.Inject

/**
 * Facade for repository management Git operations.
 * Aggregates related UseCases to reduce ViewModel dependency count.
 */
class GitRepoFacade @Inject constructor(
    private val cleanUseCase: CleanUseCase,
    private val cloneRepositoryUseCase: CloneRepositoryUseCase,
    private val configureRemoteUseCase: ConfigureRemoteUseCase,
    private val getRepoInfoUseCase: GetRepoInfoUseCase,
    private val getRemoteUrlUseCase: GetRemoteUrlUseCase,
    private val initRepoUseCase: InitRepoUseCase,
    private val getRemotesUseCase: GetRemotesUseCase
) {
    suspend fun clean(repoPath: String, dryRun: Boolean = false): AppResult<CleanResult> =
        cleanUseCase(repoPath, dryRun)

    suspend fun clone(
        remoteUrl: String,
        localPath: String,
        credentials: jamgmilk.fuwagit.domain.model.credential.CloneCredential?,
        options: jamgmilk.fuwagit.domain.model.git.CloneOptions
    ): AppResult<String> =
        cloneRepositoryUseCase(remoteUrl, localPath, credentials, options)

    suspend fun configureRemote(
        repoPath: String,
        name: String,
        url: String
    ): AppResult<String> =
        configureRemoteUseCase(repoPath, name, url)

    suspend fun getRepoInfo(localPath: String): Map<String, String> =
        getRepoInfoUseCase(localPath)

    suspend fun getRemoteUrl(localPath: String, name: String = "origin"): String? =
        getRemoteUrlUseCase(localPath, name)

    suspend fun initRepo(repoPath: String): AppResult<String> =
        initRepoUseCase(repoPath)

    suspend fun getRemotes(localPath: String): AppResult<List<jamgmilk.fuwagit.domain.model.git.GitRemote>> =
        getRemotesUseCase(localPath)
}
