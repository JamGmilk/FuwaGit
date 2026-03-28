package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.domain.model.GitBranch
import jamgmilk.fuwagit.domain.model.GitChangeType
import jamgmilk.fuwagit.domain.model.GitCommit
import jamgmilk.fuwagit.domain.model.GitFileStatus
import jamgmilk.fuwagit.domain.model.GitRepoStatus
import jamgmilk.fuwagit.domain.model.PullResult
import jamgmilk.fuwagit.domain.repository.GitRepository
import jamgmilk.fuwagit.data.source.JGitDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GitRepositoryImpl : GitRepository {
    
    override suspend fun getStatus(repoPath: String): Result<GitRepoStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(repoPath)
            val internalStatus = JGitDataSource.readRepoStatus(dir)
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
            JGitDataSource.getLog(File(repoPath), maxCount).map { commit ->
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
            JGitDataSource.getBranches(File(repoPath)).map { branch ->
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
            JGitDataSource.getDetailedStatus(File(repoPath)).map { status ->
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
            JGitDataSource.initRepo(File(repoPath))
        }
    }
    
    override suspend fun stageAll(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.stageAll(File(repoPath))
        }
    }
    
    override suspend fun unstageAll(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.unstageAll(File(repoPath))
        }
    }
    
    override suspend fun stageFile(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.stageFile(File(repoPath), path)
        }
    }
    
    override suspend fun unstageFile(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.unstageFile(File(repoPath), path)
        }
    }
    
    override suspend fun discardChanges(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.discardChanges(File(repoPath), path)
        }
    }
    
    override suspend fun commit(repoPath: String, message: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.commit(File(repoPath), message)
        }
    }
    
    override suspend fun pull(repoPath: String): Result<PullResult> = withContext(Dispatchers.IO) {
        runCatching {
            val result = JGitDataSource.pull(File(repoPath))
            PullResult(
                isSuccessful = result.contains("Success: true"),
                message = result
            )
        }
    }
    
    override suspend fun push(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.push(File(repoPath))
        }
    }
    
    override suspend fun checkoutBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.checkoutBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun createBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.createBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun mergeBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.mergeBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun rebaseBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.rebaseBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.deleteBranch(File(repoPath), branchName, force)
        }
    }
    
    override suspend fun hasGitDir(path: String?): Boolean = withContext(Dispatchers.IO) {
        JGitDataSource.hasGitDir(path)
    }
    
    override suspend fun getRepoInfo(localPath: String): Map<String, String> = withContext(Dispatchers.IO) {
        JGitDataSource.getRepoInfo(File(localPath))
    }
    
    override suspend fun getRemoteUrl(localPath: String, name: String): String? = withContext(Dispatchers.IO) {
        JGitDataSource.getRemoteUrl(File(localPath), name)
    }
    
    override suspend fun configureRemote(localPath: String, name: String, url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            JGitDataSource.configureRemote(File(localPath), name, url)
        }
    }
}
