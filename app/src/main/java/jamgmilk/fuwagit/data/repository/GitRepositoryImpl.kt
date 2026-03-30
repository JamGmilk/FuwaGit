package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.data.source.JGitDataSource
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
import java.io.File
import javax.inject.Inject

class GitRepositoryImpl @Inject constructor(
    private val jGitDataSource: JGitDataSource
) : GitRepository {
    
    override suspend fun getStatus(repoPath: String): Result<GitRepoStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(repoPath)
            val internalStatus = jGitDataSource.readRepoStatus(dir)
            GitRepoStatus(
                isGitRepo = internalStatus.isGitRepo,
                branch = "",
                hasUncommittedChanges = false,
                untrackedCount = 0,
                message = internalStatus.message
            )
        }
    }
    
    override suspend fun getCommitHistory(repoPath: String, maxCount: Int): Result<List<GitCommit>> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.getLog(File(repoPath), maxCount).map { commit ->
                GitCommit(
                    hash = commit.hash,
                    shortHash = commit.shortHash,
                    authorName = commit.authorName,
                    authorEmail = commit.authorEmail,
                    message = commit.message,
                    timestamp = commit.timestamp
                )
            }
        }
    }
    
    override suspend fun getBranches(repoPath: String): Result<List<GitBranch>> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.getBranches(File(repoPath)).map { branch ->
                GitBranch(
                    name = branch.name,
                    fullRef = branch.fullRef,
                    isRemote = branch.isRemote,
                    isCurrent = branch.isCurrent
                )
            }
        }
    }
    
    override suspend fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.getDetailedStatus(File(repoPath)).map { status ->
                GitFileStatus(
                    path = status.path,
                    name = status.name,
                    isStaged = status.isStaged,
                    changeType = GitChangeType.valueOf(status.changeType.name)
                )
            }
        }
    }
    
    override suspend fun initRepo(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.initRepo(File(repoPath))
        }
    }
    
    override suspend fun stageAll(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.stageAll(File(repoPath))
        }
    }
    
    override suspend fun unstageAll(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.unstageAll(File(repoPath))
        }
    }
    
    override suspend fun stageFile(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.stageFile(File(repoPath), path)
        }
    }
    
    override suspend fun unstageFile(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.unstageFile(File(repoPath), path)
        }
    }
    
    override suspend fun discardChanges(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.discardChanges(File(repoPath), path)
        }
    }
    
    override suspend fun commit(repoPath: String, message: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.commit(File(repoPath), message)
        }
    }
    
    override suspend fun pull(repoPath: String): Result<PullResult> = withContext(Dispatchers.IO) {
        runCatching {
            val result = jGitDataSource.pull(File(repoPath))
            PullResult(
                isSuccessful = result.contains("Success: true"),
                message = result
            )
        }
    }
    
    override suspend fun push(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.push(File(repoPath))
        }
    }

    override suspend fun fetch(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.fetch(File(repoPath))
        }
    }
    
    override suspend fun checkoutBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.checkoutBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun createBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.createBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun mergeBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.mergeBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun rebaseBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.rebaseBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.deleteBranch(File(repoPath), branchName, force)
        }
    }
    
    override suspend fun hasGitDir(path: String?): Boolean = withContext(Dispatchers.IO) {
        jGitDataSource.hasGitDir(path)
    }
    
    override suspend fun getRepoInfo(localPath: String): Map<String, String> = withContext(Dispatchers.IO) {
        jGitDataSource.getRepoInfo(File(localPath))
    }
    
    override suspend fun getRemoteUrl(localPath: String, name: String): String? = withContext(Dispatchers.IO) {
        jGitDataSource.getRemoteUrl(File(localPath), name)
    }
    
    override suspend fun configureRemote(localPath: String, name: String, url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.configureRemote(File(localPath), name, url)
        }
    }

    override suspend fun cloneRepository(
        uri: String,
        localPath: String,
        branch: String?,
        credentials: CloneCredential?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.cloneRepository(uri, File(localPath), branch, credentials)
        }
    }

    override suspend fun getRemotes(repoPath: String): Result<List<GitRemote>> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.getRemotes(File(repoPath))
        }
    }

    override suspend fun deleteRemote(repoPath: String, remoteName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.deleteRemote(File(repoPath), remoteName)
        }
    }

    override suspend fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.renameBranch(File(repoPath), oldName, newName)
        }
    }

    override suspend fun clean(repoPath: String, dryRun: Boolean): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            jGitDataSource.clean(File(repoPath), dryRun)
        }
    }
}
