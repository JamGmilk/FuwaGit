package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.ConflictFileInfo
import jamgmilk.fuwagit.data.jgit.GitCommitDataSource
import jamgmilk.fuwagit.data.jgit.GitCoreDataSource
import jamgmilk.fuwagit.data.jgit.GitDiffDataSource
import jamgmilk.fuwagit.data.jgit.GitMergeDataSource
import jamgmilk.fuwagit.data.jgit.GitOperationCheckDataSource
import jamgmilk.fuwagit.data.jgit.GitRemoteDataSource
import jamgmilk.fuwagit.data.jgit.GitStatusDataSource
import jamgmilk.fuwagit.data.jgit.GitTagDataSource
import jamgmilk.fuwagit.data.jgit.PrePullCheckResult
import jamgmilk.fuwagit.data.jgit.PrePushCheckResult
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.*
import jamgmilk.fuwagit.domain.repository.GitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GitRepositoryImpl @Inject constructor(
    private val core: GitCoreDataSource,
    private val status: GitStatusDataSource,
    private val commit: GitCommitDataSource,
    private val remote: GitRemoteDataSource,
    private val merge: GitMergeDataSource,
    private val tag: GitTagDataSource,
    private val diff: GitDiffDataSource,
    private val operationCheck: GitOperationCheckDataSource
) : GitRepository {

    override suspend fun getStatus(repoPath: String): AppResult<GitRepoStatus> =
        withContext(Dispatchers.IO) { status.readRepoStatus(repoPath).toAppResult() }

    override suspend fun getCommitHistory(repoPath: String, maxCount: Int): AppResult<List<GitCommit>> =
        withContext(Dispatchers.IO) { commit.getLog(repoPath, maxCount).toAppResult() }

    override suspend fun getCommitFileChanges(repoPath: String, commitHash: String): AppResult<GitCommitDetail> =
        withContext(Dispatchers.IO) { commit.getCommitFileChanges(repoPath, commitHash).toAppResult() }

    override suspend fun getBranches(repoPath: String): AppResult<List<GitBranch>> =
        withContext(Dispatchers.IO) { status.getBranches(repoPath).toAppResult() }

    override suspend fun getDetailedStatus(repoPath: String): AppResult<List<GitFileStatus>> =
        withContext(Dispatchers.IO) { status.getDetailedStatus(repoPath).toAppResult() }

    override suspend fun initRepo(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) { core.initRepo(repoPath).toAppResult() }

    override suspend fun stageAll(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) { status.stageAll(repoPath).toAppResult() }

    override suspend fun unstageAll(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) { status.unstageAll(repoPath).toAppResult() }

    override suspend fun stageFile(repoPath: String, path: String): AppResult<Unit> =
        withContext(Dispatchers.IO) { status.stageFile(repoPath, path).toAppResult() }

    override suspend fun unstageFile(repoPath: String, path: String): AppResult<Unit> =
        withContext(Dispatchers.IO) { status.unstageFile(repoPath, path).toAppResult() }

    override suspend fun discardChanges(repoPath: String, path: String): AppResult<Unit> =
        withContext(Dispatchers.IO) { status.discardChanges(repoPath, path).toAppResult() }

    override suspend fun commit(repoPath: String, message: String): AppResult<String> =
        withContext(Dispatchers.IO) { commit.commit(repoPath, message).toAppResult() }

    override suspend fun reset(repoPath: String, commitHash: String, mode: GitResetMode): AppResult<String> =
        withContext(Dispatchers.IO) { commit.reset(repoPath, commitHash, mode).toAppResult() }

    override suspend fun pull(repoPath: String, credentials: CloneCredential?): AppResult<PullResult> =
        withContext(Dispatchers.IO) { remote.pull(repoPath, credentials).toAppResult() }

    override suspend fun push(repoPath: String, credentials: CloneCredential?, options: GitPushOptions): AppResult<String> =
        withContext(Dispatchers.IO) { remote.push(repoPath, credentials, options).toAppResult() }

    override suspend fun fetch(repoPath: String, credentials: CloneCredential?): AppResult<String> =
        withContext(Dispatchers.IO) { remote.fetch(repoPath, credentials).toAppResult() }

    override suspend fun checkoutBranch(repoPath: String, branchName: String): AppResult<Unit> =
        withContext(Dispatchers.IO) { status.checkoutBranch(repoPath, branchName).toAppResult() }

    override suspend fun createBranch(repoPath: String, branchName: String): AppResult<Unit> =
        withContext(Dispatchers.IO) { status.createBranch(repoPath, branchName).toAppResult() }

    override suspend fun mergeBranch(repoPath: String, branchName: String): AppResult<ConflictResult> =
        withContext(Dispatchers.IO) { merge.mergeBranch(repoPath, branchName).toAppResult() }

    override suspend fun rebaseBranch(repoPath: String, branchName: String): AppResult<ConflictResult> =
        withContext(Dispatchers.IO) { merge.rebaseBranch(repoPath, branchName).toAppResult() }

    override suspend fun continueRebase(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) { merge.continueRebase(repoPath).toAppResult() }

    override suspend fun getConflictStatus(repoPath: String): AppResult<ConflictResult> =
        withContext(Dispatchers.IO) { merge.getConflictStatus(repoPath).toAppResult() }

    override suspend fun markConflictResolved(repoPath: String, filePath: String): AppResult<Unit> =
        withContext(Dispatchers.IO) { merge.markConflictResolved(repoPath, filePath).toAppResult() }

    override suspend fun abortRebase(repoPath: String): AppResult<String> =
        withContext(Dispatchers.IO) { merge.abortRebase(repoPath).toAppResult() }

    override suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): AppResult<Unit> =
        withContext(Dispatchers.IO) { status.deleteBranch(repoPath, branchName, force).toAppResult() }

    override suspend fun hasGitDir(path: String?): Boolean =
        withContext(Dispatchers.IO) { core.hasGitDir(path) }

    override suspend fun getRepoInfo(localPath: String): Map<String, String> =
        withContext(Dispatchers.IO) { core.getRepoInfo(localPath) }

    override suspend fun getRemoteUrl(localPath: String, name: String): String? =
        withContext(Dispatchers.IO) { remote.getRemoteUrl(localPath, name) }

    override suspend fun configureRemote(localPath: String, name: String, url: String): AppResult<String> =
        withContext(Dispatchers.IO) { remote.configureRemote(localPath, name, url).toAppResult() }

    override suspend fun cloneRepository(uri: String, localPath: String, credentials: CloneCredential?, options: CloneOptions): AppResult<String> =
        withContext(Dispatchers.IO) { remote.cloneRepository(uri, localPath, credentials, options).toAppResult() }

    override suspend fun getRemotes(repoPath: String): AppResult<List<GitRemote>> =
        withContext(Dispatchers.IO) { remote.getRemotes(repoPath).toAppResult() }

    override suspend fun deleteRemote(repoPath: String, remoteName: String): AppResult<String> =
        withContext(Dispatchers.IO) { remote.deleteRemote(repoPath, remoteName).toAppResult() }

    override suspend fun renameBranch(repoPath: String, oldName: String, newName: String): AppResult<String> =
        withContext(Dispatchers.IO) { status.renameBranch(repoPath, oldName, newName).toAppResult() }

    override suspend fun clean(repoPath: String, dryRun: Boolean): AppResult<CleanResult> =
        withContext(Dispatchers.IO) { merge.clean(repoPath, dryRun).toAppResult() }

    // ==================== Tag ====================

    override suspend fun getTags(repoPath: String): AppResult<List<GitTag>> =
        withContext(Dispatchers.IO) { tag.getTags(repoPath).toAppResult() }

    override suspend fun createLightweightTag(repoPath: String, tagName: String, commitHash: String?): AppResult<String> =
        withContext(Dispatchers.IO) { tag.createLightweightTag(repoPath, tagName, commitHash).toAppResult() }

    override suspend fun createAnnotatedTag(
        repoPath: String,
        tagName: String,
        message: String,
        commitHash: String?
    ): AppResult<String> =
        withContext(Dispatchers.IO) { tag.createAnnotatedTag(repoPath, tagName, message, commitHash).toAppResult() }

    override suspend fun deleteTag(repoPath: String, tagName: String): AppResult<Unit> =
        withContext(Dispatchers.IO) { tag.deleteTag(repoPath, tagName).toAppResult() }

    override suspend fun pushTag(repoPath: String, tagName: String, remoteName: String): AppResult<String> =
        withContext(Dispatchers.IO) { tag.pushTag(repoPath, tagName, remoteName).toAppResult() }

    override suspend fun pushAllTags(repoPath: String, remoteName: String): AppResult<String> =
        withContext(Dispatchers.IO) { tag.pushAllTags(repoPath, remoteName).toAppResult() }

    override suspend fun checkoutTag(repoPath: String, tagName: String): AppResult<String> =
        withContext(Dispatchers.IO) { tag.checkoutTag(repoPath, tagName).toAppResult() }

    // ==================== Diff ====================

    override suspend fun getWorkingTreeDiff(repoPath: String, filePath: String): AppResult<FileDiff> =
        withContext(Dispatchers.IO) { diff.getWorkingTreeDiff(repoPath, filePath).toAppResult() }

    override suspend fun getStagedDiff(repoPath: String, filePath: String): AppResult<FileDiff> =
        withContext(Dispatchers.IO) { diff.getStagedDiff(repoPath, filePath).toAppResult() }

    override suspend fun getCommitFileDiff(
        repoPath: String,
        filePath: String,
        oldCommit: String,
        newCommit: String
    ): AppResult<FileDiff> =
        withContext(Dispatchers.IO) { diff.getCommitFileDiff(repoPath, filePath, oldCommit, newCommit).toAppResult() }

    override suspend fun getCommitDiff(
        repoPath: String,
        oldCommit: String,
        newCommit: String
    ): AppResult<List<FileDiff>> =
        withContext(Dispatchers.IO) { diff.getCommitDiff(repoPath, oldCommit, newCommit).toAppResult() }

    override suspend fun getFileContent(
        repoPath: String,
        filePath: String,
        commitHash: String?
    ): AppResult<String> =
        withContext(Dispatchers.IO) { diff.getFileContent(repoPath, filePath, commitHash).toAppResult() }

    override suspend fun checkPrePullStatus(repoPath: String): Result<PrePullCheckResult> =
        withContext(Dispatchers.IO) { operationCheck.checkPrePullStatus(repoPath) }

    override suspend fun checkPrePushStatus(repoPath: String): Result<PrePushCheckResult> =
        withContext(Dispatchers.IO) { operationCheck.checkPrePushStatus(repoPath) }

    override suspend fun isRepositoryLocked(repoPath: String): Boolean =
        withContext(Dispatchers.IO) { operationCheck.isRepositoryLocked(repoPath) }

    override suspend fun getConflictDetails(repoPath: String): Result<List<ConflictFileInfo>> =
        withContext(Dispatchers.IO) { operationCheck.getConflictDetails(repoPath) }
}
