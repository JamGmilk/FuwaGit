package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Git status and staging operations.
 */
@Singleton
class JGitStatusDataSource @Inject constructor(
    private val core: JGitCoreDataSource
) {
    /**
     * Reads the current repository status.
     */
    fun readRepoStatus(repoPath: String): Result<jamgmilk.fuwagit.domain.model.git.GitRepoStatus> =
        core.withGit(repoPath) { git ->
            val status = git.status().call()
            val repository = git.repository

            jamgmilk.fuwagit.domain.model.git.GitRepoStatus(
                isGitRepo = true,
                branch = repository.fullBranch ?: "",
                hasUncommittedChanges = !status.isClean,
                untrackedCount = status.untracked.size,
                message = if (status.isClean) "Clean" else "Changes detected"
            )
        }

    /**
     * Gets detailed file status including staged and unstaged changes.
     */
    fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>> =
        core.withGit(repoPath) { git ->
            val status = git.status().call()
            val allFiles = mutableListOf<GitFileStatus>()

            status.added.forEach { path ->
                allFiles.add(GitFileStatus(path, java.io.File(path).name, true, GitChangeType.Added))
            }
            status.changed.forEach { path ->
                allFiles.add(GitFileStatus(path, java.io.File(path).name, true, GitChangeType.Modified))
            }
            status.removed.forEach { path ->
                allFiles.add(GitFileStatus(path, java.io.File(path).name, true, GitChangeType.Removed))
            }

            status.modified.forEach { path ->
                allFiles.add(GitFileStatus(path, java.io.File(path).name, false, GitChangeType.Modified))
            }
            status.missing.forEach { path ->
                allFiles.add(GitFileStatus(path, java.io.File(path).name, false, GitChangeType.Removed))
            }
            status.untracked.forEach { path ->
                allFiles.add(GitFileStatus(path, java.io.File(path).name, false, GitChangeType.Untracked))
            }
            status.conflicting.forEach { path ->
                allFiles.add(GitFileStatus(path, java.io.File(path).name, false, GitChangeType.Conflicting))
            }

            allFiles
        }

    /**
     * Stages all changes including deletions.
     */
    fun stageAll(repoPath: String): Result<String> = core.withGit(repoPath) { git ->
        git.add().addFilepattern(".").setUpdate(true).call()
        git.add().addFilepattern(".").call()
        "All changes staged"
    }

    /**
     * Unstages all changes.
     */
    fun unstageAll(repoPath: String): Result<String> = core.withGit(repoPath) { git ->
        try {
            git.reset().setRef("HEAD").call()
        } catch (e: Exception) {
            git.reset().call()
        }
        "All changes unstaged"
    }

    /**
     * Stages a specific file.
     */
    fun stageFile(repoPath: String, filePath: String): Result<Unit> = core.withGit(repoPath) { git ->
        val status = git.status().addPath(filePath).call()
        if (status.missing.contains(filePath) || status.removed.contains(filePath)) {
            git.rm().addFilepattern(filePath).call()
        } else {
            git.add().addFilepattern(filePath).call()
        }
        Unit
    }

    /**
     * Unstages a specific file.
     */
    fun unstageFile(repoPath: String, filePath: String): Result<Unit> = core.withGit(repoPath) { git ->
        git.reset().setRef("HEAD").addPath(filePath).call()
        Unit
    }

    /**
     * Discards changes to a specific file.
     */
    fun discardChanges(repoPath: String, filePath: String): Result<Unit> = core.withGit(repoPath) { git ->
        git.checkout().addPath(filePath).call()
        Unit
    }

    /**
     * Lists all branches.
     */
    fun getBranches(repoPath: String): Result<List<GitBranch>> = core.withGit(repoPath) { git ->
        val branchList = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
        val currentBranch = git.repository.fullBranch

        branchList.map { ref ->
            val isRemote = ref.name.startsWith("refs/remotes/")
            val name = if (isRemote) {
                ref.name.removePrefix("refs/remotes/")
            } else {
                ref.name.removePrefix("refs/heads/")
            }

            GitBranch(
                name = name,
                fullRef = ref.name,
                isRemote = isRemote,
                isCurrent = ref.name == currentBranch
            )
        }
    }

    /**
     * Creates a new branch.
     */
    fun createBranch(repoPath: String, branchName: String): Result<Unit> = core.withGit(repoPath) { git ->
        git.branchCreate().setName(branchName).call()
        Unit
    }

    /**
     * Checks out a branch (supports remote tracking branches).
     */
    fun checkoutBranch(repoPath: String, branchName: String): Result<Unit> = core.withGit(repoPath) { git ->
        val remoteBranchRegex = Regex("^([^/]+)/(.+)$")
        val matchResult = remoteBranchRegex.find(branchName)

        if (matchResult != null) {
            val remoteName = matchResult.groupValues[1]
            val shortBranchName = matchResult.groupValues[2]
            val remoteRefName = "refs/remotes/$branchName"

            val remoteRef = git.repository.findRef(remoteRefName)
            if (remoteRef != null) {
                val localRef = git.repository.findRef("refs/heads/$shortBranchName")

                if (localRef != null) {
                    git.checkout().setName(shortBranchName).call()
                } else {
                    git.checkout()
                        .setName(shortBranchName)
                        .setCreateBranch(true)
                        .setStartPoint(remoteRefName)
                        .setUpstreamMode(org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                        .call()
                }
            } else {
                git.checkout().setName(branchName).call()
            }
        } else {
            git.checkout().setName(branchName).call()
        }
        Unit
    }

    /**
     * Deletes a branch.
     */
    fun deleteBranch(repoPath: String, branchName: String, force: Boolean = false): Result<Unit> =
        core.withGit(repoPath) { git ->
            git.branchDelete().setBranchNames(branchName).setForce(force).call()
            Unit
        }

    /**
     * Renames a branch.
     */
    fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String> =
        core.withGit(repoPath) { git ->
            git.branchRename().setOldName(oldName).setNewName(newName).call()
            "Branch renamed to $newName"
        }
}
