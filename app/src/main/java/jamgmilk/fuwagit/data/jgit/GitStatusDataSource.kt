package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitFileStatus

/**
 * Interface for Git status and staging operations.
 */
interface GitStatusDataSource {
    fun readRepoStatus(repoPath: String): Result<jamgmilk.fuwagit.domain.model.git.GitRepoStatus>

    fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>>

    fun stageAll(repoPath: String): Result<String>

    fun unstageAll(repoPath: String): Result<String>

    fun stageFile(repoPath: String, filePath: String): Result<Unit>

    fun unstageFile(repoPath: String, filePath: String): Result<Unit>

    fun discardChanges(repoPath: String, filePath: String): Result<Unit>

    fun getBranches(repoPath: String): Result<List<GitBranch>>

    fun createBranch(repoPath: String, branchName: String): Result<Unit>

    fun checkoutBranch(repoPath: String, branchName: String): Result<Unit>

    fun deleteBranch(repoPath: String, branchName: String, force: Boolean): Result<Unit>

    fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String>
}
