package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.PullResult
import jamgmilk.fuwagit.domain.repository.MergeRepository
import jamgmilk.fuwagit.domain.repository.RemoteRepository
import javax.inject.Inject

class SafePullUseCase @Inject constructor(
    private val mergeRepository: MergeRepository,
    private val remoteRepository: RemoteRepository
) {
    suspend operator fun invoke(
        repoPath: String,
        credentials: CloneCredential? = null
    ): AppResult<PullResult> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }

        val preCheckResult = mergeRepository.checkPrePullStatus(repoPath)

        return preCheckResult.fold(
            onSuccess = { check ->
                if (!check.canPull) {
                    return@fold AppResult.Error(
                        AppException.GitOperationFailed(
                            "pull",
                            check.message.ifEmpty { "Cannot pull: repository is not in a valid state" }
                        )
                    )
                }

                if (check.hasLocalChanges && check.hasStagedChanges) {
                    return@fold AppResult.Error(
                        AppException.GitOperationFailed(
                            "pull",
                            "Cannot pull: you have staged changes that would be overwritten by merge. Commit or unstage them first."
                        )
                    )
                }

                if (check.hasConflicts) {
                    return@fold AppResult.Error(
                        AppException.GitOperationFailed(
                            "pull",
                            "Cannot pull: you have unresolved merge conflicts. Resolve them first: ${check.message}"
                        )
                    )
                }

                remoteRepository.pull(repoPath, credentials)
            },
            onFailure = { error ->
                AppResult.Error(
                    AppException.GitOperationFailed(
                        "pull",
                        "Failed to check repository status: ${error.message}"
                    )
                )
            }
        )
    }
}
