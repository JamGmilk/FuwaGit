package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.data.jgit.JGitCoreDataSource
import jamgmilk.fuwagit.data.jgit.JGitStatusDataSource
import jamgmilk.fuwagit.data.jgit.JGitCommitDataSource
import jamgmilk.fuwagit.data.jgit.JGitRemoteDataSource
import jamgmilk.fuwagit.data.jgit.JGitMergeDataSource
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.*
import jamgmilk.fuwagit.domain.repository.GitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GitRepositoryImpl @Inject constructor(
    private val core: JGitCoreDataSource,
    private val status: JGitStatusDataSource,
    private val commit: JGitCommitDataSource,
    private val remote: JGitRemoteDataSource,
    private val merge: JGitMergeDataSource
) : GitRepository {

    override suspend fun getStatus(repoPath: String): Result<GitRepoStatus> =
        withContext(Dispatchers.IO) { status.readRepoStatus(repoPath) }

    override suspend fun getCommitHistory(repoPath: String, maxCount: Int): Result<List<GitCommit>> =
        withContext(Dispatchers.IO) { commit.getLog(repoPath, maxCount) }

    override suspend fun getCommitFileChanges(repoPath: String, commitHash: String): Result<GitCommitDetail> =
        withContext(Dispatchers.IO) { commit.getCommitFileChanges(repoPath, commitHash) }

    override suspend fun getBranches(repoPath: String): Result<List<GitBranch>> =
        withContext(Dispatchers.IO) { status.getBranches(repoPath) }

    override suspend fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>> =
        withContext(Dispatchers.IO) { status.getDetailedStatus(repoPath) }

    override suspend fun initRepo(repoPath: String): Result<String> =
        withContext(Dispatchers.IO) { core.initRepo(repoPath) }

    override suspend fun stageAll(repoPath: String): Result<String> =
        withContext(Dispatchers.IO) { status.stageAll(repoPath) }

    override suspend fun unstageAll(repoPath: String): Result<String> =
        withContext(Dispatchers.IO) { status.unstageAll(repoPath) }

    override suspend fun stageFile(repoPath: String, path: String): Result<Unit> =
        withContext(Dispatchers.IO) { status.stageFile(repoPath, path) }

    override suspend fun unstageFile(repoPath: String, path: String): Result<Unit> =
        withContext(Dispatchers.IO) { status.unstageFile(repoPath, path) }

    override suspend fun discardChanges(repoPath: String, path: String): Result<Unit> =
        withContext(Dispatchers.IO) { status.discardChanges(repoPath, path) }

    override suspend fun commit(repoPath: String, message: String): Result<String> =
        withContext(Dispatchers.IO) { commit.commit(repoPath, message) }

    override suspend fun reset(repoPath: String, commitHash: String, mode: GitResetMode): Result<String> =
        withContext(Dispatchers.IO) { commit.reset(repoPath, commitHash, mode) }

    override suspend fun pull(repoPath: String, credentials: CloneCredential?): Result<PullResult> =
        withContext(Dispatchers.IO) { remote.pull(repoPath, credentials) }

    override suspend fun push(repoPath: String, credentials: CloneCredential?, options: GitPushOptions): Result<String> =
        withContext(Dispatchers.IO) { remote.push(repoPath, credentials, options) }

    override suspend fun fetch(repoPath: String, credentials: CloneCredential?): Result<String> =
        withContext(Dispatchers.IO) { remote.fetch(repoPath, credentials) }

    override suspend fun checkoutBranch(repoPath: String, branchName: String): Result<Unit> =
        withContext(Dispatchers.IO) { status.checkoutBranch(repoPath, branchName) }

    override suspend fun createBranch(repoPath: String, branchName: String): Result<Unit> =
        withContext(Dispatchers.IO) { status.createBranch(repoPath, branchName) }

    override suspend fun mergeBranch(repoPath: String, branchName: String): Result<ConflictResult> =
        withContext(Dispatchers.IO) { merge.mergeBranch(repoPath, branchName) }

    override suspend fun rebaseBranch(repoPath: String, branchName: String): Result<ConflictResult> =
        withContext(Dispatchers.IO) { merge.rebaseBranch(repoPath, branchName) }

    override suspend fun getConflictStatus(repoPath: String): Result<ConflictResult> =
        withContext(Dispatchers.IO) { merge.getConflictStatus(repoPath) }

    override suspend fun markConflictResolved(repoPath: String, filePath: String): Result<Unit> =
        withContext(Dispatchers.IO) { merge.markConflictResolved(repoPath, filePath) }

    override suspend fun abortRebase(repoPath: String): Result<String> =
        withContext(Dispatchers.IO) { merge.abortRebase(repoPath) }

    override suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) { status.deleteBranch(repoPath, branchName, force) }

    override suspend fun hasGitDir(path: String?): Boolean =
        withContext(Dispatchers.IO) { core.hasGitDir(path) }

    override suspend fun getRepoInfo(localPath: String): Map<String, String> =
        withContext(Dispatchers.IO) { core.getRepoInfo(localPath) }

    override suspend fun getRemoteUrl(localPath: String, name: String): String? =
        withContext(Dispatchers.IO) { remote.getRemoteUrl(localPath, name) }

    override suspend fun configureRemote(localPath: String, name: String, url: String): Result<String> =
        withContext(Dispatchers.IO) { remote.configureRemote(localPath, name, url) }

    override suspend fun cloneRepository(uri: String, localPath: String, credentials: CloneCredential?, options: CloneOptions): Result<String> =
        withContext(Dispatchers.IO) { remote.cloneRepository(uri, localPath, credentials, options) }

    override suspend fun getRemotes(repoPath: String): Result<List<GitRemote>> =
        withContext(Dispatchers.IO) { remote.getRemotes(repoPath) }

    override suspend fun deleteRemote(repoPath: String, remoteName: String): Result<String> =
        withContext(Dispatchers.IO) { remote.deleteRemote(repoPath, remoteName) }

    override suspend fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String> =
        withContext(Dispatchers.IO) { status.renameBranch(repoPath, oldName, newName) }

    override suspend fun clean(repoPath: String, dryRun: Boolean): Result<CleanResult> =
        withContext(Dispatchers.IO) { merge.clean(repoPath, dryRun) }
}
