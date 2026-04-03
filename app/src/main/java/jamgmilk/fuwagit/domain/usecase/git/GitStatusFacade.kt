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
    private val getBranchesUseCase: GetBranchesUseCase,
    private val stageAllUseCase: StageAllUseCase,
    private val unstageAllUseCase: UnstageAllUseCase,
    private val stageFileUseCase: StageFileUseCase,
    private val unstageFileUseCase: UnstageFileUseCase,
    private val discardChangesUseCase: DiscardChangesUseCase,
    private val commitUseCase: CommitUseCase
) {
    suspend fun hasGitDir(repoPath: String): Boolean = hasGitDirUseCase(repoPath)

    suspend fun initRepo(repoPath: String): Result<String> = initRepoUseCase(repoPath)

    suspend fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>> =
        getDetailedStatusUseCase(repoPath)

    suspend fun getBranches(repoPath: String): Result<List<GitBranch>> =
        getBranchesUseCase(repoPath)

    suspend fun stageAll(repoPath: String): Result<String> = stageAllUseCase(repoPath)

    suspend fun unstageAll(repoPath: String): Result<String> = unstageAllUseCase(repoPath)

    suspend fun stageFile(repoPath: String, filePath: String): Result<Unit> =
        stageFileUseCase(repoPath, filePath)

    suspend fun unstageFile(repoPath: String, filePath: String): Result<Unit> =
        unstageFileUseCase(repoPath, filePath)

    suspend fun discardChanges(repoPath: String, filePath: String): Result<Unit> =
        discardChangesUseCase(repoPath, filePath)

    suspend fun commit(repoPath: String, message: String): Result<String> =
        commitUseCase(repoPath, message)
}
