package jamgmilk.obsigit.ui

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.RepositoryNotFoundException
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

    fun terminalStatus(dir: File): String {
        Git.open(dir).use { git ->
            val status = git.status().call()
            return "Branch: ${git.repository.branch}\n" +
                "Added: ${status.added.size}\n" +
                "Modified: ${status.modified.size}\n" +
                "Untracked: ${status.untracked.size}"
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
