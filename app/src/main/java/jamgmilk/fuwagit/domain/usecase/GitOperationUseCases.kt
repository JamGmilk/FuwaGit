package jamgmilk.fuwagit.domain.usecase

import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.PullResult
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitOperationUseCases @Inject constructor(
    private val repository: GitRepository
) {
    suspend fun initRepo(repoPath: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.initRepo(repoPath)
    }

    suspend fun cloneRepository(
        uri: String,
        localPath: String,
        branch: String? = null,
        credentials: CloneCredential? = null
    ): Result<String> {
        if (uri.isBlank()) {
            return Result.failure(IllegalArgumentException("URI cannot be empty"))
        }
        if (localPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Local path cannot be empty"))
        }
        return repository.cloneRepository(uri, localPath, branch, credentials)
    }

    suspend fun stageAll(repoPath: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.stageAll(repoPath)
    }

    suspend fun unstageAll(repoPath: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.unstageAll(repoPath)
    }

    suspend fun stageFile(repoPath: String, filePath: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (filePath.isBlank()) {
            return Result.failure(IllegalArgumentException("File path cannot be empty"))
        }
        return repository.stageFile(repoPath, filePath)
    }

    suspend fun unstageFile(repoPath: String, filePath: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (filePath.isBlank()) {
            return Result.failure(IllegalArgumentException("File path cannot be empty"))
        }
        return repository.unstageFile(repoPath, filePath)
    }

    suspend fun commit(repoPath: String, message: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (message.isBlank()) {
            return Result.failure(IllegalArgumentException("Commit message cannot be empty"))
        }
        return repository.commit(repoPath, message)
    }

    suspend fun discardChanges(repoPath: String, filePath: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.discardChanges(repoPath, filePath)
    }

    suspend fun pull(repoPath: String): Result<PullResult> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.pull(repoPath)
    }

    suspend fun push(repoPath: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.push(repoPath)
    }

    suspend fun fetch(repoPath: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.fetch(repoPath)
    }

    suspend fun createBranch(repoPath: String, branchName: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (branchName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }
        return repository.createBranch(repoPath, branchName)
    }

    suspend fun checkoutBranch(repoPath: String, branchName: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (branchName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }
        return repository.checkoutBranch(repoPath, branchName)
    }

    suspend fun deleteBranch(repoPath: String, branchName: String, force: Boolean = false): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (branchName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }
        return repository.deleteBranch(repoPath, branchName, force)
    }

    suspend fun mergeBranch(repoPath: String, branchName: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (branchName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }
        return repository.mergeBranch(repoPath, branchName)
    }

    suspend fun rebaseBranch(repoPath: String, branchName: String): Result<Unit> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (branchName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }
        return repository.rebaseBranch(repoPath, branchName)
    }

    suspend fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (oldName.isBlank() || newName.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch names cannot be empty"))
        }
        return repository.renameBranch(repoPath, oldName, newName)
    }

    suspend fun createTag(
        repoPath: String,
        tagName: String,
        message: String? = null,
        commitHash: String? = null
    ): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return Result.failure(IllegalArgumentException("Tag name cannot be empty"))
        }
        return repository.createTag(repoPath, tagName, message, commitHash)
    }

    suspend fun deleteTag(repoPath: String, tagName: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (tagName.isBlank()) {
            return Result.failure(IllegalArgumentException("Tag name cannot be empty"))
        }
        return repository.deleteTag(repoPath, tagName)
    }

    suspend fun stashChanges(repoPath: String, message: String? = null): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.stashChanges(repoPath, message)
    }

    suspend fun applyStash(repoPath: String, stashIndex: Int, dropAfterApply: Boolean = false): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (stashIndex < 0) {
            return Result.failure(IllegalArgumentException("Stash index cannot be negative"))
        }
        return repository.applyStash(repoPath, stashIndex, dropAfterApply)
    }

    suspend fun dropStash(repoPath: String, stashIndex: Int): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (stashIndex < 0) {
            return Result.failure(IllegalArgumentException("Stash index cannot be negative"))
        }
        return repository.dropStash(repoPath, stashIndex)
    }

    suspend fun configureRemote(localPath: String, name: String, url: String): Result<String> {
        if (localPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Local path cannot be empty"))
        }
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Remote name cannot be empty"))
        }
        if (url.isBlank()) {
            return Result.failure(IllegalArgumentException("Remote URL cannot be empty"))
        }
        return repository.configureRemote(localPath, name, url)
    }

    suspend fun deleteRemote(repoPath: String, remoteName: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (remoteName.isBlank()) {
            return Result.failure(IllegalArgumentException("Remote name cannot be empty"))
        }
        return repository.deleteRemote(repoPath, remoteName)
    }

    suspend fun clean(repoPath: String, dryRun: Boolean = false): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.clean(repoPath, dryRun)
    }

    suspend fun revertCommit(repoPath: String, commitHash: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (commitHash.isBlank()) {
            return Result.failure(IllegalArgumentException("Commit hash cannot be empty"))
        }
        return repository.revertCommit(repoPath, commitHash)
    }

    suspend fun cherryPick(repoPath: String, commitHash: String): Result<String> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        if (commitHash.isBlank()) {
            return Result.failure(IllegalArgumentException("Commit hash cannot be empty"))
        }
        return repository.cherryPick(repoPath, commitHash)
    }
}
