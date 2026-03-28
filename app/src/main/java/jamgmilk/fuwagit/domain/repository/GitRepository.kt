package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.domain.model.GitBranch
import jamgmilk.fuwagit.domain.model.GitCommit
import jamgmilk.fuwagit.domain.model.GitFileStatus
import jamgmilk.fuwagit.domain.model.GitRepoStatus
import jamgmilk.fuwagit.domain.model.PullResult

interface GitRepository {
    
    suspend fun getStatus(repoPath: String): Result<GitRepoStatus>
    
    suspend fun getCommitHistory(repoPath: String, maxCount: Int): Result<List<GitCommit>>
    
    suspend fun getBranches(repoPath: String): Result<List<GitBranch>>
    
    suspend fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>>
    
    suspend fun initRepo(repoPath: String): Result<String>
    
    suspend fun stageAll(repoPath: String): Result<String>
    
    suspend fun unstageAll(repoPath: String): Result<String>
    
    suspend fun stageFile(repoPath: String, path: String): Result<Unit>
    
    suspend fun unstageFile(repoPath: String, path: String): Result<Unit>
    
    suspend fun discardChanges(repoPath: String, path: String): Result<Unit>
    
    suspend fun commit(repoPath: String, message: String): Result<String>
    
    suspend fun pull(repoPath: String): Result<PullResult>
    
    suspend fun push(repoPath: String): Result<String>
    
    suspend fun checkoutBranch(repoPath: String, branchName: String): Result<Unit>
    
    suspend fun createBranch(repoPath: String, branchName: String): Result<Unit>
    
    suspend fun mergeBranch(repoPath: String, branchName: String): Result<Unit>
    
    suspend fun rebaseBranch(repoPath: String, branchName: String): Result<Unit>
    
    suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): Result<Unit>
    
    suspend fun hasGitDir(path: String?): Boolean
    
    suspend fun getRepoInfo(localPath: String): Map<String, String>
    
    suspend fun getRemoteUrl(localPath: String, name: String = "origin"): String?
    
    suspend fun configureRemote(localPath: String, name: String, url: String): Result<String>
}
