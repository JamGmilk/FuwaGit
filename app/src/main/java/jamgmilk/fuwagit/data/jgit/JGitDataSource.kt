package jamgmilk.fuwagit.data.jgit

import android.util.Log
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.model.git.GitRepoStatus
import jamgmilk.fuwagit.domain.model.git.PullResult
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.errors.LockFailedException
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JGitDataSource @Inject constructor() {

    companion object {
        private const val TAG = "JGitDataSource"
    }

    fun initRepo(repoPath: String): Result<String> {
        return try {
            val repoDir = File(repoPath)
            if (!repoDir.exists() && !repoDir.mkdirs()) {
                return Result.failure(Exception("Failed to create directory: $repoPath"))
            }

            val repository = FileRepositoryBuilder()
                .setGitDir(File(repoDir, ".git"))
                .setMustExist(false)
                .build()

            repository.create()

            repository.close()
            Result.success("Repository initialized at $repoPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init repository", e)
            Result.failure(e)
        }
    }

    fun hasGitDir(path: String?): Boolean {
        if (path == null) return false
        return try {
            val gitDir = File(path, ".git")
            gitDir.exists() && gitDir.isDirectory
        } catch (e: Exception) {
            false
        }
    }

    fun isValidRepository(repoPath: String): Boolean {
        return try {
            val repository = FileRepositoryBuilder()
                .setGitDir(File(repoPath, ".git"))
                .setMustExist(true)
                .build()
            repository.isBare || repository.directory.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun readRepoStatus(repoPath: String): Result<GitRepoStatus> {
        return try {
            val git = Git.open(File(repoPath))
            val status = git.status().call()

            git.close()

            Result.success(
                GitRepoStatus(
                    isGitRepo = true,
                    branch = git.repository.fullBranch ?: "",
                    hasUncommittedChanges = !status.isClean,
                    untrackedCount = status.untracked.size,
                    message = if (status.isClean) "Clean" else "Changes detected"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get status", e)
            Result.failure(e)
        }
    }

    fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>> {
        return try {
            val git = Git.open(File(repoPath))
            val status = git.status().call()

            val allFiles = mutableListOf<GitFileStatus>()

            status.added.forEach { path ->
                allFiles.add(GitFileStatus(
                    path = path,
                    name = File(path).name,
                    changeType = GitChangeType.Added,
                    isStaged = true
                ))
            }

            status.changed.forEach { path ->
                allFiles.add(GitFileStatus(
                    path = path,
                    name = File(path).name,
                    changeType = GitChangeType.Modified,
                    isStaged = false
                ))
            }

            status.untracked.forEach { path ->
                allFiles.add(GitFileStatus(
                    path = path,
                    name = File(path).name,
                    changeType = GitChangeType.Untracked,
                    isStaged = false
                ))
            }

            status.removed.forEach { path ->
                allFiles.add(GitFileStatus(
                    path = path,
                    name = File(path).name,
                    changeType = GitChangeType.Removed,
                    isStaged = true
                ))
            }

            git.close()

            Result.success(allFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get detailed status", e)
            Result.failure(e)
        }
    }

    fun getBranches(repoPath: String): Result<List<GitBranch>> {
        return try {
            val git = Git.open(File(repoPath))
            val branchList = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()

            val currentBranch = git.repository.fullBranch

            val branches = branchList.map { ref ->
                val isRemote = ref.name.startsWith("refs/remotes/")
                val name = if (isRemote) {
                    ref.name.removePrefix("refs/remotes/")
                } else {
                    ref.name.removePrefix("refs/heads/")
                }

                val isCurrent = ref.name == currentBranch

                GitBranch(
                    name = name,
                    fullRef = ref.name,
                    isRemote = isRemote,
                    isCurrent = isCurrent
                )
            }

            git.close()

            Result.success(branches)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get branches", e)
            Result.failure(e)
        }
    }

    fun getLog(repoPath: String, maxCount: Int = 100): Result<List<GitCommit>> {
        return try {
            val git = Git.open(File(repoPath))

            val log = git.log().setMaxCount(maxCount).call().toList()

            val commits = log.map { revCommit ->
                val author = revCommit.authorIdent
                GitCommit(
                    hash = revCommit.id.name(),
                    shortHash = revCommit.id.abbreviate(7).name(),
                    message = revCommit.fullMessage,
                    authorName = author.name,
                    authorEmail = author.emailAddress,
                    timestamp = author.`when`.time,
                    parentHashes = revCommit.parents.map { it.name() }
                )
            }

            git.close()

            Result.success(commits)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get commit history", e)
            Result.failure(e)
        }
    }

    fun stageAll(repoPath: String): Result<String> {
        return try {
            val git = Git.open(File(repoPath))
            git.add().addFilepattern(".").call()
            git.close()
            Result.success("All changes staged")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage all", e)
            Result.failure(e)
        }
    }

    fun unstageAll(repoPath: String): Result<String> {
        return try {
            val git = Git.open(File(repoPath))
            git.reset().setRef("HEAD").call()
            git.close()
            Result.success("All changes unstaged")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unstage all", e)
            Result.failure(e)
        }
    }

    fun stageFile(repoPath: String, filePath: String): Result<Unit> {
        return try {
            val git = Git.open(File(repoPath))
            git.add().addFilepattern(filePath).call()
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage file", e)
            Result.failure(e)
        }
    }

    fun unstageFile(repoPath: String, filePath: String): Result<Unit> {
        return try {
            val git = Git.open(File(repoPath))
            git.reset().setRef("HEAD").addPath(filePath).call()
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unstage file", e)
            Result.failure(e)
        }
    }

    fun discardChanges(repoPath: String, filePath: String): Result<Unit> {
        return try {
            val git = Git.open(File(repoPath))
            git.checkout().addPath(filePath).call()
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to discard changes", e)
            Result.failure(e)
        }
    }

    fun commit(repoPath: String, message: String): Result<String> {
        return try {
            val git = Git.open(File(repoPath))
            val commit = git.commit()
                .setMessage(message)
                .setAllowEmpty(false)
                .call()

            git.close()

            Result.success(commit.id.name())
        } catch (e: LockFailedException) {
            Result.failure(Exception("Cannot commit: repository lock failed. Another process may be operating on this repository."))
        } catch (e: JGitInternalException) {
            Result.failure(Exception("Git error: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to commit", e)
            Result.failure(e)
        }
    }

    fun cloneRepository(
        uri: String,
        localPath: String,
        branch: String? = null,
        credentials: CloneCredential? = null
    ): Result<String> {
        return try {
            val cloneCommand = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(File(localPath))
                .setCloneAllBranches(false)
                .setDepth(50)

            if (credentials is CloneCredential.Https) {
                cloneCommand.setCredentialsProvider(
                    org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(
                        credentials.username,
                        credentials.password
                    )
                )
            }

            if (branch != null) {
                cloneCommand.setBranch(branch)
            }

            val git = cloneCommand.call()
            val clonedPath = git.repository.directory.parentFile.absolutePath
            git.close()

            Result.success(clonedPath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clone repository", e)
            Result.failure(e)
        }
    }

    fun pull(repoPath: String): Result<PullResult> {
        return try {
            val git = Git.open(File(repoPath))
            val pullResult = git.pull().call()

            git.close()

            val isSuccessful = pullResult.isSuccessful
            Result.success(
                PullResult(
                    isSuccessful = isSuccessful,
                    message = if (isSuccessful) "Pull successful" else "Pull failed"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull", e)
            Result.failure(Exception(e.message ?: "Pull failed"))
        }
    }

    fun push(repoPath: String): Result<String> {
        return try {
            val git = Git.open(File(repoPath))
            git.push()
                .setPushAll()
                .call()

            git.close()

            Result.success("Push completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push", e)
            Result.failure(e)
        }
    }

    fun fetch(repoPath: String): Result<String> {
        return try {
            val git = Git.open(File(repoPath))
            git.fetch()
                .setRemoveDeletedRefs(true)
                .call()

            git.close()

            Result.success("Fetch completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch", e)
            Result.failure(e)
        }
    }

    fun createBranch(repoPath: String, branchName: String): Result<Unit> {
        return try {
            val git = Git.open(File(repoPath))
            git.branchCreate().setName(branchName).call()
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create branch", e)
            Result.failure(e)
        }
    }

    fun checkoutBranch(repoPath: String, branchName: String): Result<Unit> {
        return try {
            val git = Git.open(File(repoPath))
            git.checkout().setName(branchName).call()
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to checkout branch", e)
            Result.failure(e)
        }
    }

    fun deleteBranch(repoPath: String, branchName: String, force: Boolean = false): Result<Unit> {
        return try {
            val git = Git.open(File(repoPath))
            git.branchDelete().setBranchNames(branchName).setForce(force).call()
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete branch", e)
            Result.failure(e)
        }
    }

    fun mergeBranch(repoPath: String, branchName: String): Result<Unit> {
        return try {
            val git = Git.open(File(repoPath))
            git.merge()
                .include(git.repository.findRef(branchName))
                .setCommit(true)
                .call()
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge branch", e)
            Result.failure(e)
        }
    }

    fun rebaseBranch(repoPath: String, branchName: String): Result<Unit> {
        return try {
            val git = Git.open(File(repoPath))
            git.rebase()
                .setUpstream(branchName)
                .call()
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rebase branch", e)
            Result.failure(e)
        }
    }

    fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String> {
        return try {
            val git = Git.open(File(repoPath))
            git.branchRename().setOldName(oldName).setNewName(newName).call()
            git.close()
            Result.success("Branch renamed to $newName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename branch", e)
            Result.failure(e)
        }
    }

    fun configureRemote(repoPath: String, name: String, url: String): Result<String> {
        return try {
            val git = Git.open(File(repoPath))
            git.remoteAdd().setName(name).setUri(org.eclipse.jgit.transport.URIish(url)).call()
            git.close()

            Result.success("Remote $name configured: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure remote", e)
            Result.failure(e)
        }
    }

    fun deleteRemote(repoPath: String, remoteName: String): Result<String> {
        return try {
            val git = Git.open(File(repoPath))
            git.remoteRemove().setRemoteName(remoteName).call()
            git.close()
            Result.success("Remote $remoteName removed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete remote", e)
            Result.failure(e)
        }
    }

    fun getRemotes(repoPath: String): Result<List<GitRemote>> {
        return try {
            val git = Git.open(File(repoPath))
            val remoteList = git.remoteList().call()

            val remotes = remoteList.map { remote ->
                GitRemote(
                    name = remote.name,
                    fetchUrl = remote.name,
                    pushUrl = null
                )
            }

            git.close()

            Result.success(remotes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get remotes", e)
            Result.failure(e)
        }
    }

    fun getRemoteUrl(repoPath: String, name: String = "origin"): String? {
        return try {
            val git = Git.open(File(repoPath))
            val config = git.repository.config
            val url = config.getString("remote", name, "url")
            git.close()
            url
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get remote URL", e)
            null
        }
    }

    fun clean(repoPath: String, dryRun: Boolean = false): Result<String> {
        return try {
            val git = Git.open(File(repoPath))
            git.clean().setCleanDirectories(true).setIgnore(false).call()
            git.close()

            Result.success("Cleaned")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean", e)
            Result.failure(e)
        }
    }

    fun getRepoInfo(repoPath: String): Map<String, String> {
        val info = mutableMapOf<String, String>()

        try {
            val git = Git.open(File(repoPath))
            val repository = git.repository

            info["path"] = repoPath
            info["gitDir"] = repository.directory.absolutePath
            info["isBare"] = repository.isBare.toString()

            try {
                val config = repository.config
                info["user.name"] = config.getString("user", null, "name") ?: "Not set"
                info["user.email"] = config.getString("user", null, "email") ?: "Not set"
            } catch (e: Exception) {
                info["user.name"] = "Error reading"
                info["user.email"] = "Error reading"
            }

            try {
                val head = repository.resolve("HEAD")
                if (head != null) {
                    info["HEAD"] = head.name()
                }
            } catch (e: Exception) {
                info["HEAD"] = "No commits yet"
            }

            git.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get repo info", e)
            info["error"] = e.message ?: "Unknown error"
        }

        return info
    }
}