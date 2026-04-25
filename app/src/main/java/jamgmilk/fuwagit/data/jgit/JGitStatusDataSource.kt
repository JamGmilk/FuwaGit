package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import org.eclipse.jgit.api.ListBranchCommand
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Git status and staging operations.
 */
@Singleton
class JGitStatusDataSource @Inject constructor(
    private val core: GitCoreDataSource
) : GitStatusDataSource {
    /**
     * Reads the current repository status.
     */
    override fun readRepoStatus(repoPath: String): Result<jamgmilk.fuwagit.domain.model.git.GitRepoStatus> =
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
    override fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>> =
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
                if (path != "/dev/null" && path.isNotBlank()) {
                    allFiles.add(GitFileStatus(path, java.io.File(path).name, true, GitChangeType.Removed))
                }
            }

            status.modified.forEach { path ->
                allFiles.add(GitFileStatus(path, java.io.File(path).name, false, GitChangeType.Modified))
            }
            status.missing.forEach { path ->
                if (path != "/dev/null" && path.isNotBlank()) {
                    allFiles.add(GitFileStatus(path, java.io.File(path).name, false, GitChangeType.Removed))
                }
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
    override fun stageAll(repoPath: String): Result<String> = core.withGit(repoPath) { git ->
        git.add().addFilepattern(".").setUpdate(true).call()
        git.add().addFilepattern(".").call()
        "All changes staged"
    }

    /**
     * Unstages all changes.
     */
    override fun unstageAll(repoPath: String): Result<String> = core.withGit(repoPath) { git ->
        val status = git.status().call()
        val hasStagedChanges = status.added.isNotEmpty() ||
                              status.changed.isNotEmpty() ||
                              status.removed.isNotEmpty()

        if (!hasStagedChanges) {
            return@withGit "No staged changes to unstaged"
        }

        val headRef = git.repository.resolve("HEAD")
        if (headRef == null) {
            status.added.forEach { git.rm().setCached(true).addFilepattern(it).call() }
            status.changed.forEach { git.rm().setCached(true).addFilepattern(it).call() }
            status.removed.forEach { git.rm().setCached(true).addFilepattern(it).call() }
        } else {
            git.reset().setRef("HEAD").call()
        }
        "All changes unstaged"
    }

    /**
     * Stages a specific file.
     */
    override fun stageFile(repoPath: String, filePath: String): Result<Unit> = core.withGit(repoPath) { git ->
        val status = git.status().call()
        if (status.missing.contains(filePath) || status.removed.contains(filePath)) {
            git.rm().addFilepattern(filePath).call()
        } else {
            git.add().addFilepattern(filePath).call()
        }
    }

    /**
     * Unstages a specific file.
     */
    override fun unstageFile(repoPath: String, filePath: String): Result<Unit> = core.withGit(repoPath) { git ->
        val headRef = git.repository.resolve("HEAD")
        if (headRef == null) {
            git.rm().setCached(true).addFilepattern(filePath).call()
        } else {
            git.reset().setRef("HEAD").addPath(filePath).call()
        }
        Unit
    }

    /**
     * Discards changes to a specific file.
     * Handles both modified files (checkout) and untracked files (clean).
     */
    override fun discardChanges(repoPath: String, filePath: String): Result<Unit> = core.withGit(repoPath) { git ->
        val status = git.status().call()
        val isModified = status.changed.contains(filePath) ||
                         status.modified.contains(filePath) ||
                         status.removed.contains(filePath)
        val isUntracked = status.untracked.contains(filePath)

        if (!isModified && !isUntracked) {
            return@withGit
        }

        try {
            if (isUntracked) {
                git.clean().setPaths(setOf(filePath)).call()
            }
            if (isModified) {
                git.checkout().addPath(filePath).call()
            }
        } catch (e: Exception) {
            throw Exception("Failed to discard changes: ${e.message}. Make sure the file is not locked by another process.")
        }
    }

    /**
     * Lists all branches.
     */
    override fun getBranches(repoPath: String): Result<List<GitBranch>> = core.withGit(repoPath) { git ->
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
    override fun createBranch(repoPath: String, branchName: String): Result<Unit> = core.withGit(repoPath) { git ->
        git.branchCreate().setName(branchName).call()
    }

    /**
     * Checks out a branch (supports remote tracking branches).
     */
    override fun checkoutBranch(repoPath: String, branchName: String): Result<Unit> = core.withGit(repoPath) { git ->
        val remoteBranchRegex = Regex("^([^/]+)/(.+)$")
        val matchResult = remoteBranchRegex.find(branchName)

        if (matchResult != null) {
            val remoteName = matchResult.groupValues[1]
            val shortBranchName = matchResult.groupValues[2]
            val remoteRefName = "refs/remotes/$remoteName/$shortBranchName"

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
    }

    /**
     * Deletes a branch.
     */
    override fun deleteBranch(repoPath: String, branchName: String, force: Boolean): Result<Unit> =
        core.withGit(repoPath) { git ->
            git.branchDelete().setBranchNames(branchName).setForce(force).call()
        }

    /**
     * Renames a branch.
     */
    override fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String> =
        core.withGit(repoPath) { git ->
            git.branchRename().setOldName(oldName).setNewName(newName).call()
            "Branch renamed to $newName"
        }
}
