package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.data.jgit.JGitDataSource
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.model.git.GitRepoStatus
import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.model.git.PullResult
import jamgmilk.fuwagit.domain.repository.GitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GitRepositoryImpl @Inject constructor(
    private val jGitDataSource: JGitDataSource
) : GitRepository {

    override suspend fun getStatus(repoPath: String): Result<GitRepoStatus> = withContext(Dispatchers.IO) {
        jGitDataSource.readRepoStatus(repoPath)
    }

    override suspend fun getCommitHistory(repoPath: String, maxCount: Int): Result<List<GitCommit>> = withContext(Dispatchers.IO) {
        jGitDataSource.getLog(repoPath, maxCount)
    }

    override suspend fun getBranches(repoPath: String): Result<List<GitBranch>> = withContext(Dispatchers.IO) {
        jGitDataSource.getBranches(repoPath)
    }

    override suspend fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>> = withContext(Dispatchers.IO) {
        jGitDataSource.getDetailedStatus(repoPath)
    }

    override suspend fun initRepo(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.initRepo(repoPath)
    }

    override suspend fun stageAll(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.stageAll(repoPath)
    }

    override suspend fun unstageAll(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.unstageAll(repoPath)
    }

    override suspend fun stageFile(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        jGitDataSource.stageFile(repoPath, path)
    }

    override suspend fun unstageFile(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        jGitDataSource.unstageFile(repoPath, path)
    }

    override suspend fun discardChanges(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        jGitDataSource.discardChanges(repoPath, path)
    }

    override suspend fun commit(repoPath: String, message: String): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.commit(repoPath, message)
    }

    override suspend fun pull(repoPath: String): Result<PullResult> = withContext(Dispatchers.IO) {
        jGitDataSource.pull(repoPath)
    }

    override suspend fun push(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.push(repoPath)
    }

    override suspend fun fetch(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.fetch(repoPath)
    }

    override suspend fun checkoutBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        jGitDataSource.checkoutBranch(repoPath, branchName)
    }

    override suspend fun createBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        jGitDataSource.createBranch(repoPath, branchName)
    }

    override suspend fun mergeBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        jGitDataSource.mergeBranch(repoPath, branchName)
    }

    override suspend fun rebaseBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        jGitDataSource.rebaseBranch(repoPath, branchName)
    }

    override suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        jGitDataSource.deleteBranch(repoPath, branchName, force)
    }

    override suspend fun hasGitDir(path: String?): Boolean = withContext(Dispatchers.IO) {
        jGitDataSource.hasGitDir(path)
    }

    override suspend fun getRepoInfo(localPath: String): Map<String, String> = withContext(Dispatchers.IO) {
        jGitDataSource.getRepoInfo(localPath)
    }

    override suspend fun getRemoteUrl(localPath: String, name: String): String? = withContext(Dispatchers.IO) {
        jGitDataSource.getRemoteUrl(localPath, name)
    }

    override suspend fun configureRemote(localPath: String, name: String, url: String): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.configureRemote(localPath, name, url)
    }

    override suspend fun cloneRepository(
        uri: String,
        localPath: String,
        branch: String?,
        credentials: CloneCredential?
    ): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.cloneRepository(uri, localPath, branch, credentials)
    }

    override suspend fun getRemotes(repoPath: String): Result<List<GitRemote>> = withContext(Dispatchers.IO) {
        jGitDataSource.getRemotes(repoPath)
    }

    override suspend fun deleteRemote(repoPath: String, remoteName: String): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.deleteRemote(repoPath, remoteName)
    }

    override suspend fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.renameBranch(repoPath, oldName, newName)
    }

    override suspend fun clean(repoPath: String, dryRun: Boolean): Result<String> = withContext(Dispatchers.IO) {
        jGitDataSource.clean(repoPath, dryRun)
    }
}