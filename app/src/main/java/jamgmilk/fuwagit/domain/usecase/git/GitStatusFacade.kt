package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import javax.inject.Inject

/**
 * Facade for Git status operations.
 * Aggregates related UseCases to reduce ViewModel dependency count.
 */
class GitStatusFacade @Inject constructor(
    private val hasGitDirUseCase: HasGitDirUseCase,
    private val initRepoUseCase: InitRepoUseCase,
    private val getDetailedStatusUseCase: GetDetailedStatusUseCase,
    private val branchUseCase: BranchUseCase,
    private val stageUseCase: StageUseCase,
    private val discardChangesUseCase: DiscardChangesUseCase,
    private val commitUseCase: CommitUseCase,
    private val getRemoteUrlUseCase: GetRemoteUrlUseCase
) {
    suspend fun hasGitDir(repoPath: String): Boolean = hasGitDirUseCase(repoPath)

    suspend fun initRepo(repoPath: String): Result<String> = initRepoUseCase(repoPath)

    suspend fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>> =
        getDetailedStatusUseCase(repoPath)

    suspend fun getBranches(repoPath: String): Result<List<GitBranch>> =
        branchUseCase.list(repoPath)

    suspend fun stageAll(repoPath: String): Result<String> = stageUseCase.all(repoPath)

    suspend fun unstageAll(repoPath: String): Result<String> = stageUseCase.unstageAll(repoPath)

    suspend fun stageFile(repoPath: String, filePath: String): Result<Unit> =
        stageUseCase.file(repoPath, filePath)

    suspend fun unstageFile(repoPath: String, filePath: String): Result<Unit> =
        stageUseCase.unstageFile(repoPath, filePath)

    suspend fun discardChanges(repoPath: String, filePath: String): Result<Unit> =
        discardChangesUseCase(repoPath, filePath)

    suspend fun commit(repoPath: String, message: String): Result<String> =
        commitUseCase(repoPath, message)

    suspend fun getRemoteUrl(repoPath: String, remoteName: String = "origin"): String? =
        getRemoteUrlUseCase(repoPath, remoteName)
}
