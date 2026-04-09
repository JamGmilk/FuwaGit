package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.GitPushOptions
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject

class SafePushUseCase @Inject constructor(
    private val repository: GitRepository
) {
    suspend operator fun invoke(
        repoPath: String,
        credentials: CloneCredential? = null,
        options: GitPushOptions = GitPushOptions.default()
    ): AppResult<String> {
        if (repoPath.isBlank()) {
            return AppResult.Error(AppException.Validation("Repository path cannot be empty"))
        }

        val preCheckResult = repository.checkPrePushStatus(repoPath)

        return preCheckResult.fold(
            onSuccess = { check ->
                if (!check.canPush) {
                    return@fold AppResult.Error(
                        AppException.GitOperationFailed(
                            "push",
                            check.message.ifEmpty { "Cannot push: repository is not in a valid state" }
                        )
                    )
                }

                if (check.hasUncommittedChanges) {
                    return@fold AppResult.Error(
                        AppException.GitOperationFailed(
                            "push",
                            "Cannot push: you have uncommitted changes. Commit them first."
                        )
                    )
                }

                if (check.hasConflicts) {
                    return@fold AppResult.Error(
                        AppException.GitOperationFailed(
                            "push",
                            "Cannot push: you have unresolved merge conflicts. Resolve them first."
                        )
                    )
                }

                if (!options.forcePush && !options.forceWithLease && check.remoteBranchBehind > 0) {
                    return@fold AppResult.Error(
                        AppException.GitOperationFailed(
                            "push",
                            "Cannot push: remote is ${check.remoteBranchBehind} commit(s) ahead. Pull and merge first, or use force push to override."
                        )
                    )
                }

                repository.push(repoPath, credentials, options)
            },
            onFailure = { error ->
                AppResult.Error(
                    AppException.GitOperationFailed(
                        "push",
                        "Failed to check repository status: ${error.message}"
                    )
                )
            }
        )
    }
}
