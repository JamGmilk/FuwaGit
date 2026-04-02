package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.CleanResult
import jamgmilk.fuwagit.domain.model.git.CloneOptions
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.model.git.GitPushOptions
import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.model.git.GitRepoStatus
import jamgmilk.fuwagit.domain.model.git.PullResult
import jamgmilk.fuwagit.domain.model.git.GitResetMode

interface GitRepository {
    
    suspend fun getStatus(repoPath: String): Result<GitRepoStatus>
    
    suspend fun getCommitHistory(repoPath: String, maxCount: Int): Result<List<GitCommit>>

    suspend fun getCommitFileChanges(repoPath: String, commitHash: String): Result<GitCommitDetail>

    suspend fun getBranches(repoPath: String): Result<List<GitBranch>>
    
    suspend fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>>
    
    suspend fun initRepo(repoPath: String): Result<String>
    
    suspend fun stageAll(repoPath: String): Result<String>
    
    suspend fun unstageAll(repoPath: String): Result<String>
    
    suspend fun stageFile(repoPath: String, path: String): Result<Unit>
    
    suspend fun unstageFile(repoPath: String, path: String): Result<Unit>
    
    suspend fun discardChanges(repoPath: String, path: String): Result<Unit>
    
    suspend fun commit(repoPath: String, message: String): Result<String>

    suspend fun reset(repoPath: String, commitHash: String, mode: GitResetMode): Result<String>

    suspend fun pull(repoPath: String, credentials: CloneCredential? = null): Result<PullResult>

    suspend fun push(
        repoPath: String,
        credentials: CloneCredential? = null,
        options: GitPushOptions = GitPushOptions.default()
    ): Result<String>

    suspend fun fetch(repoPath: String, credentials: CloneCredential? = null): Result<String>

    suspend fun checkoutBranch(repoPath: String, branchName: String): Result<Unit>
    
    suspend fun createBranch(repoPath: String, branchName: String): Result<Unit>
    
    suspend fun mergeBranch(repoPath: String, branchName: String): Result<ConflictResult>

    suspend fun rebaseBranch(repoPath: String, branchName: String): Result<ConflictResult>

    suspend fun getConflictStatus(repoPath: String): Result<ConflictResult>

    suspend fun markConflictResolved(repoPath: String, filePath: String): Result<Unit>

    suspend fun abortRebase(repoPath: String): Result<String>
    
    suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): Result<Unit>
    
    suspend fun hasGitDir(path: String?): Boolean

    suspend fun cloneRepository(
        uri: String,
        localPath: String,
        credentials: CloneCredential? = null,
        options: CloneOptions = CloneOptions()
    ): Result<String>

    suspend fun getRepoInfo(localPath: String): Map<String, String>
    
    suspend fun getRemoteUrl(localPath: String, name: String = "origin"): String?
    
    suspend fun configureRemote(localPath: String, name: String, url: String): Result<String>

    suspend fun getRemotes(repoPath: String): Result<List<GitRemote>>

    suspend fun deleteRemote(repoPath: String, remoteName: String): Result<String>

    suspend fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String>

    suspend fun clean(repoPath: String, dryRun: Boolean = false): Result<CleanResult>
}
