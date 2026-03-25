package jamgmilk.obsigit.ui

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Repository
import java.io.File

internal data class GitRepoStatus(
    val isGitRepo: Boolean,
    val message: String
)

internal object AppGitOps {

    fun readRepoStatus(dir: File): GitRepoStatus {
        if (!dir.exists()) {
            dir.mkdirs()
        }

        return try {
            Git.open(dir).use { git ->
                val status = git.status().call()
                GitRepoStatus(
                    isGitRepo = true,

                    message = "Path: ${AppRepoOps.shortDisplayPath(dir)}\n" +
                        "Branch: ${git.repository.branch}\n" +
                        "Uncommitted: ${status.hasUncommittedChanges()}\n" +
                        "Untracked: ${status.untracked.size}"
                )
            }
        } catch (_: RepositoryNotFoundException) {
            GitRepoStatus(
                isGitRepo = false,
                message = "Path: ${AppRepoOps.shortDisplayPath(dir)}\nNot a Git repository"
            )
        }
    }

    fun getDetailedStatus(dir: File): List<GitFileStatus> {
        val result = mutableListOf<GitFileStatus>()
        try {
            Git.open(dir).use { git ->
                val status = git.status().call()

                // Staged
                status.added.forEach { result.add(GitFileStatus(it, File(it).name, true, GitChangeType.Added)) }
                status.changed.forEach { result.add(GitFileStatus(it, File(it).name, true, GitChangeType.Modified)) }
                status.removed.forEach { result.add(GitFileStatus(it, File(it).name, true, GitChangeType.Removed)) }

                // Unstaged
                status.modified.forEach { result.add(GitFileStatus(it, File(it).name, false, GitChangeType.Modified)) }
                status.untracked.forEach { result.add(GitFileStatus(it, File(it).name, false, GitChangeType.Untracked)) }
                status.missing.forEach { result.add(GitFileStatus(it, File(it).name, false, GitChangeType.Removed)) }
                
                // Conflicting
                status.conflicting.forEach { result.add(GitFileStatus(it, File(it).name, false, GitChangeType.Conflicting)) }
            }
        } catch (_: Exception) {}
        return result.distinctBy { it.path + it.isStaged }.sortedBy { it.path.lowercase() }
    }

    fun getLog(dir: File, maxCount: Int = 100): List<GitCommit> {
        try {
            Git.open(dir).use { git ->
                return git.log().setMaxCount(maxCount).call().map { rev ->
                    GitCommit(
                        hash = rev.name,
                        shortHash = rev.name.take(7),
                        authorName = rev.authorIdent.name,
                        authorEmail = rev.authorIdent.emailAddress,
                        message = rev.shortMessage,
                        timestamp = rev.commitTime.toLong() * 1000L
                    )
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    fun getBranches(dir: File): List<GitBranch> {
        val result = mutableListOf<GitBranch>()
        try {
            Git.open(dir).use { git ->
                val repo = git.repository
                val currentBranch = try { repo.branch } catch(_: Exception) { null }

                // Local branches
                git.branchList().call().forEach { ref ->
                    val name = Repository.shortenRefName(ref.name)
                    result.add(GitBranch(name, ref.name, false, name == currentBranch))
                }

                // Remote branches
                git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call().forEach { ref ->
                    val name = Repository.shortenRefName(ref.name)
                    result.add(GitBranch(name, ref.name, true, false))
                }
            }
        } catch (_: Exception) {}
        return result
    }

    fun terminalStatus(dir: File): String {
        return try {
            Git.open(dir).use { git ->
                val status = git.status().call()
                "Branch: ${git.repository.branch}\n" +
                    "Added: ${status.added.size}\n" +
                    "Modified: ${status.modified.size}\n" +
                    "Untracked: ${status.untracked.size}"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun initRepo(dir: File): String {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        Git.init().setDirectory(dir).call().use { }
        return "Initialized repository in ${AppRepoOps.shortDisplayPath(dir)}"
    }

    fun stageAll(dir: File): String {
        Git.open(dir).use { git ->
            git.add().addFilepattern(".").call()
        }
        return "All files staged"
    }

    fun unstageAll(dir: File): String {
        Git.open(dir).use { git ->
            git.reset().call()
        }
        return "Unstaged all changes"
    }

    fun unstageFile(dir: File, path: String) {
        Git.open(dir).use { git ->
            git.reset().addPath(path).call()
        }
    }
    
    fun discardChanges(dir: File, path: String) {
        Git.open(dir).use { git ->
            git.checkout().addPath(path).call()
        }
    }

    fun stageFile(dir: File, path: String) {
        Git.open(dir).use { git ->
            git.add().addFilepattern(path).call()
        }
    }

    fun checkoutBranch(dir: File, name: String) {
        Git.open(dir).use { git ->
            git.checkout().setName(name).call()
        }
    }
    
    fun deleteBranch(dir: File, name: String, force: Boolean = false) {
        Git.open(dir).use { git ->
            git.branchDelete().setBranchNames(name).setForce(force).call()
        }
    }
    
    fun mergeBranch(dir: File, ref: String) {
        Git.open(dir).use { git ->
            git.merge().include(git.repository.findRef(ref)).call()
        }
    }

    fun rebaseBranch(dir: File, ref: String) {
        Git.open(dir).use { git ->
            git.rebase().setUpstream(ref).call()
        }
    }

    fun commit(dir: File, message: String): String {
        Git.open(dir).use { git ->
            git.commit().setMessage(message).call()
        }
        return "Commit successful"
    }

    fun pull(dir: File): String {
        Git.open(dir).use { git ->
            val result = git.pull().call()
            return "Success: ${result.isSuccessful}"
        }
    }

    fun push(dir: File): String {
        Git.open(dir).use { git ->
            git.push().call()
        }
        return "Push command executed"
    }

    fun hasGitDir(path: String?): Boolean {
        if (path == null) return false
        return File(path, ".git").exists()
    }
}
