package jamgmilk.fuwagit.domain.usecase.git

import javax.inject.Inject

/**
 * Facade for Git remote sync operations (pull, push, fetch).
 * Aggregates related UseCases to reduce ViewModel dependency count.
 */
class GitSyncFacade @Inject constructor(
    private val pullUseCase: PullUseCase,
    private val pushUseCase: PushUseCase,
    private val fetchUseCase: FetchUseCase
) {
    suspend fun pull(
        repoPath: String,
        credentials: jamgmilk.fuwagit.domain.model.credential.CloneCredential?
    ): Result<jamgmilk.fuwagit.domain.model.git.PullResult> =
        pullUseCase(repoPath, credentials)

    suspend fun push(
        repoPath: String,
        credentials: jamgmilk.fuwagit.domain.model.credential.CloneCredential?
    ): Result<String> =
        pushUseCase(repoPath, credentials)

    suspend fun fetch(
        repoPath: String,
        credentials: jamgmilk.fuwagit.domain.model.credential.CloneCredential?
    ): Result<String> =
        fetchUseCase(repoPath, credentials)
}
