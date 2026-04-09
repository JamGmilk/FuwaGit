package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult
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

import jamgmilk.fuwagit.domain.model.git.GitTag
import jamgmilk.fuwagit.domain.model.git.FileDiff

interface GitRepository {

    suspend fun getStatus(repoPath: String): AppResult<GitRepoStatus>

    suspend fun getCommitHistory(repoPath: String, maxCount: Int): AppResult<List<GitCommit>>

    suspend fun getCommitFileChanges(repoPath: String, commitHash: String): AppResult<GitCommitDetail>

    suspend fun getBranches(repoPath: String): AppResult<List<GitBranch>>

    suspend fun getDetailedStatus(repoPath: String): AppResult<List<GitFileStatus>>

    suspend fun initRepo(repoPath: String): AppResult<String>

    suspend fun stageAll(repoPath: String): AppResult<String>

    suspend fun unstageAll(repoPath: String): AppResult<String>

    suspend fun stageFile(repoPath: String, path: String): AppResult<Unit>

    suspend fun unstageFile(repoPath: String, path: String): AppResult<Unit>

    suspend fun discardChanges(repoPath: String, path: String): AppResult<Unit>

    suspend fun commit(repoPath: String, message: String): AppResult<String>

    suspend fun reset(repoPath: String, commitHash: String, mode: GitResetMode): AppResult<String>

    suspend fun pull(repoPath: String, credentials: CloneCredential? = null): AppResult<PullResult>

    suspend fun push(
        repoPath: String,
        credentials: CloneCredential? = null,
        options: GitPushOptions = GitPushOptions.default()
    ): AppResult<String>

    suspend fun fetch(repoPath: String, credentials: CloneCredential? = null): AppResult<String>

    suspend fun checkoutBranch(repoPath: String, branchName: String): AppResult<Unit>

    suspend fun createBranch(repoPath: String, branchName: String): AppResult<Unit>

    suspend fun mergeBranch(repoPath: String, branchName: String): AppResult<ConflictResult>

    suspend fun rebaseBranch(repoPath: String, branchName: String): AppResult<ConflictResult>

    suspend fun continueRebase(repoPath: String): AppResult<String>

    suspend fun getConflictStatus(repoPath: String): AppResult<ConflictResult>

    suspend fun markConflictResolved(repoPath: String, filePath: String): AppResult<Unit>

    suspend fun abortRebase(repoPath: String): AppResult<String>

    suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean): AppResult<Unit>

    suspend fun hasGitDir(path: String?): Boolean

    suspend fun cloneRepository(
        uri: String,
        localPath: String,
        credentials: CloneCredential? = null,
        options: CloneOptions = CloneOptions()
    ): AppResult<String>

    suspend fun getRepoInfo(localPath: String): Map<String, String>

    suspend fun getRemoteUrl(localPath: String, name: String = "origin"): String?

    suspend fun configureRemote(localPath: String, name: String, url: String): AppResult<String>

    suspend fun getRemotes(repoPath: String): AppResult<List<GitRemote>>

    suspend fun deleteRemote(repoPath: String, remoteName: String): AppResult<String>

    suspend fun renameBranch(repoPath: String, oldName: String, newName: String): AppResult<String>

    suspend fun clean(repoPath: String, dryRun: Boolean = false): AppResult<CleanResult>

    // ==================== Tag 相关操作 ====================

    suspend fun getTags(repoPath: String): AppResult<List<GitTag>>

    suspend fun createLightweightTag(repoPath: String, tagName: String, commitHash: String? = null): AppResult<String>

    suspend fun createAnnotatedTag(
        repoPath: String,
        tagName: String,
        message: String,
        commitHash: String? = null
    ): AppResult<String>

    suspend fun deleteTag(repoPath: String, tagName: String): AppResult<Unit>

    suspend fun pushTag(repoPath: String, tagName: String, remoteName: String = "origin"): AppResult<String>

    suspend fun pushAllTags(repoPath: String, remoteName: String = "origin"): AppResult<String>

    suspend fun checkoutTag(repoPath: String, tagName: String): AppResult<String>

    // ==================== Diff 相关操作 ====================

    suspend fun getWorkingTreeDiff(repoPath: String, filePath: String): AppResult<FileDiff>

    suspend fun getStagedDiff(repoPath: String, filePath: String): AppResult<FileDiff>

    suspend fun getCommitFileDiff(
        repoPath: String,
        filePath: String,
        oldCommit: String,
        newCommit: String
    ): AppResult<FileDiff>

    suspend fun getCommitDiff(
        repoPath: String,
        oldCommit: String,
        newCommit: String
    ): AppResult<List<FileDiff>>

    suspend fun getFileContent(
        repoPath: String,
        filePath: String,
        commitHash: String? = null
    ): AppResult<String>

    // ==================== Pre-operation safety checks ====================

    suspend fun checkPrePullStatus(repoPath: String): Result<jamgmilk.fuwagit.data.jgit.PrePullCheckResult>

    suspend fun checkPrePushStatus(repoPath: String): Result<jamgmilk.fuwagit.data.jgit.PrePushCheckResult>

    suspend fun isRepositoryLocked(repoPath: String): Boolean

    suspend fun getConflictDetails(repoPath: String): Result<List<jamgmilk.fuwagit.data.jgit.ConflictFileInfo>>
}
