package jamgmilk.fuwagit.domain.usecase

import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.model.git.GitRepoStatus
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitQueryUseCases @Inject constructor(
    private val repository: GitRepository
) {
    suspend fun getWorkspaceStatus(repoPath: String): Result<GitRepoStatus> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.getStatus(repoPath)
    }

    suspend fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.getDetailedStatus(repoPath)
    }

    suspend fun getBranches(repoPath: String): Result<List<GitBranch>> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.getBranches(repoPath)
    }

    suspend fun getCommitHistory(repoPath: String, maxCount: Int = 100): Result<List<GitCommit>> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.getCommitHistory(repoPath, maxCount)
    }

    suspend fun getRemotes(repoPath: String): Result<List<GitRemote>> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository path cannot be empty"))
        }
        return repository.getRemotes(repoPath)
    }

    suspend fun getRepoInfo(localPath: String): Map<String, String> {
        return repository.getRepoInfo(localPath)
    }

    suspend fun getRemoteUrl(localPath: String, name: String = "origin"): String? {
        return repository.getRemoteUrl(localPath, name)
    }

    suspend fun hasGitDir(path: String?): Boolean {
        return repository.hasGitDir(path)
    }
}
