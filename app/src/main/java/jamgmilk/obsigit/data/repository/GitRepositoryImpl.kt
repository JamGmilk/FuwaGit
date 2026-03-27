package jamgmilk.obsigit.data.repository

import jamgmilk.obsigit.domain.model.GitBranch
import jamgmilk.obsigit.domain.model.GitChangeType
import jamgmilk.obsigit.domain.model.GitCommit
import jamgmilk.obsigit.domain.model.GitFileStatus
import jamgmilk.obsigit.domain.model.GitRepoStatus
import jamgmilk.obsigit.domain.model.PullResult
import jamgmilk.obsigit.domain.repository.GitRepository
import jamgmilk.obsigit.ui.AppGitOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GitRepositoryImpl : GitRepository {
    
    override suspend fun getStatus(repoPath: String): Result<GitRepoStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(repoPath)
            val internalStatus = AppGitOps.readRepoStatus(dir)
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
            AppGitOps.getLog(File(repoPath), maxCount).map { commit ->
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
            AppGitOps.getBranches(File(repoPath)).map { branch ->
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
            AppGitOps.getDetailedStatus(File(repoPath)).map { status ->
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
            AppGitOps.initRepo(File(repoPath))
        }
    }
    
    override suspend fun stageAll(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.stageAll(File(repoPath))
        }
    }
    
    override suspend fun unstageAll(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.unstageAll(File(repoPath))
        }
    }
    
    override suspend fun stageFile(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.stageFile(File(repoPath), path)
        }
    }
    
    override suspend fun unstageFile(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.unstageFile(File(repoPath), path)
        }
    }
    
    override suspend fun discardChanges(repoPath: String, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.discardChanges(File(repoPath), path)
        }
    }
    
    override suspend fun commit(repoPath: String, message: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.commit(File(repoPath), message)
        }
    }
    
    override suspend fun pull(repoPath: String): Result<PullResult> = withContext(Dispatchers.IO) {
        runCatching {
            val result = AppGitOps.pull(File(repoPath))
            PullResult(
                isSuccessful = result.contains("Success: true"),
                message = result
            )
        }
    }
    
    override suspend fun push(repoPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.push(File(repoPath))
        }
    }
    
    override suspend fun checkoutBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.checkoutBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun mergeBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.mergeBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun rebaseBranch(repoPath: String, branchName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.rebaseBranch(File(repoPath), branchName)
        }
    }
    
    override suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            AppGitOps.deleteBranch(File(repoPath), branchName, force)
        }
    }
    
    override suspend fun hasGitDir(path: String?): Boolean = withContext(Dispatchers.IO) {
        AppGitOps.hasGitDir(path)
    }
}
