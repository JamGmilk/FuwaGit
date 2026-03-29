package jamgmilk.fuwagit.data.source

import android.util.Log
import jamgmilk.fuwagit.domain.model.CloneCredential
import jamgmilk.fuwagit.domain.model.GitBranch
import jamgmilk.fuwagit.domain.model.GitChangeType
import jamgmilk.fuwagit.domain.model.GitCommit
import jamgmilk.fuwagit.domain.model.GitFileStatus
import jamgmilk.fuwagit.domain.model.GitRemote
import jamgmilk.fuwagit.domain.model.GitStash
import jamgmilk.fuwagit.domain.model.GitTag
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.errors.LockFailedException
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.Repository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GitRepoStatus(
    val isGitRepo: Boolean,
    val message: String
)

@Singleton
class JGitDataSource @Inject constructor() {
    
    private val gitMutex = Mutex()
    
    companion object {
        private const val TAG = "JGitDataSource"
    }

    suspend fun <T> withGitLock(block: suspend () -> T): T = gitMutex.withLock {
        block()
    }

    private fun <T> runGit(dir: File, block: (Git) -> T): T {
        return try {
            Git.open(dir).use { git -> block(git) }
        } catch (e: JGitInternalException) {
            val cause = e.cause
            if (cause is LockFailedException) {
                val lockFile = cause.file
                if (lockFile != null && lockFile.exists()) {
                    Log.w(TAG, "Removing stale lock file: ${lockFile.absolutePath}")
                    lockFile.delete()
                }
                Git.open(dir).use { git -> block(git) }
            } else {
                Log.e(TAG, "JGitInternalException during git operation", e)
                throw e
            }
        } catch (e: RepositoryNotFoundException) {
            Log.w(TAG, "Repository not found at ${dir.absolutePath}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during git operation", e)
            throw e
        }
    }

    fun readRepoStatus(dir: File): GitRepoStatus {
        if (!dir.exists()) {
            dir.mkdirs()
        }

        return try {
            runGit(dir) { git ->
                val status = git.status().call()
                GitRepoStatus(
                    isGitRepo = true,
                    message = "Path: ${RepoPathUtils.shortDisplayPath(dir)}\n" +
                        "Branch: ${git.repository.branch}\n" +
                        "Uncommitted: ${status.hasUncommittedChanges()}\n" +
                        "Untracked: ${status.untracked.size}"
                )
            }
        } catch (e: RepositoryNotFoundException) {
            GitRepoStatus(
                isGitRepo = false,
                message = "Path: ${RepoPathUtils.shortDisplayPath(dir)}\nNot a Git repository"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading repository status at ${dir.absolutePath}", e)
            GitRepoStatus(
                isGitRepo = false,
                message = "Path: ${RepoPathUtils.shortDisplayPath(dir)}\nError reading status: ${e.message}"
            )
        }
    }

    fun getDetailedStatus(dir: File): List<GitFileStatus> {
        val result = mutableListOf<GitFileStatus>()
        try {
            runGit(dir) { git ->
                val status = git.status().call()

                status.added.forEach { result.add(GitFileStatus(it, File(it).name, true, GitChangeType.Added)) }
                status.changed.forEach { result.add(GitFileStatus(it, File(it).name, true, GitChangeType.Modified)) }
                status.removed.forEach { result.add(GitFileStatus(it, File(it).name, true, GitChangeType.Removed)) }

                status.modified.forEach { result.add(GitFileStatus(it, File(it).name, false, GitChangeType.Modified)) }
                status.untracked.forEach { result.add(GitFileStatus(it, File(it).name, false, GitChangeType.Untracked)) }
                status.missing.forEach { result.add(GitFileStatus(it, File(it).name, false, GitChangeType.Removed)) }
                
                status.conflicting.forEach { result.add(GitFileStatus(it, File(it).name, false, GitChangeType.Conflicting)) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting detailed status at ${dir.absolutePath}", e)
            return emptyList()
        }
        return result.distinctBy { it.path + it.isStaged }.sortedBy { it.path.lowercase() }
    }

    fun getLog(dir: File, maxCount: Int = 100): List<GitCommit> {
        return try {
            runGit(dir) { git ->
                git.log().setMaxCount(maxCount).call().map { rev ->
                    GitCommit(
                        hash = rev.name,
                        shortHash = rev.name.take(7),
                        authorName = rev.authorIdent.name,
                        authorEmail = rev.authorIdent.emailAddress,
                        message = rev.shortMessage,
                        timestamp = rev.commitTime.toLong() * 1000L,
                        parentHashes = rev.parents.map { it.name }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting log at ${dir.absolutePath}", e)
            return emptyList()
        }
    }

    fun getBranches(dir: File): List<GitBranch> {
        val result = mutableListOf<GitBranch>()
        try {
            runGit(dir) { git ->
                val repo = git.repository
                val currentBranch = try { repo.branch } catch(_: Exception) { null }

                git.branchList().call().forEach { ref ->
                    val name = Repository.shortenRefName(ref.name)
                    result.add(GitBranch(name, ref.name, false, name == currentBranch))
                }

                git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call().forEach { ref ->
                    val name = Repository.shortenRefName(ref.name)
                    result.add(GitBranch(name, ref.name, true, false))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting branches at ${dir.absolutePath}", e)
            return emptyList()
        }
        return result
    }

    fun terminalStatus(dir: File): String {
        return try {
            runGit(dir) { git ->
                val status = git.status().call()
                "Branch: ${git.repository.branch}\n" +
                    "Added: ${status.added.size}\n" +
                    "Modified: ${status.modified.size}\n" +
                    "Untracked: ${status.untracked.size}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting terminal status at ${dir.absolutePath}", e)
            "Error: ${e.message}"
        }
    }

    fun initRepo(dir: File): String {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        Git.init().setDirectory(dir).call().use { }
        return "Initialized repository in ${RepoPathUtils.shortDisplayPath(dir)}"
    }

    fun stageAll(dir: File): String {
        runGit(dir) { git ->
            git.add().addFilepattern(".").call()
        }
        return "All files staged"
    }

    fun unstageAll(dir: File): String {
        runGit(dir) { git ->
            git.reset().call()
        }
        return "Unstaged all changes"
    }

    fun unstageFile(dir: File, path: String) {
        runGit(dir) { git ->
            git.reset().addPath(path).call()
        }
    }
    
    fun discardChanges(dir: File, path: String) {
        runGit(dir) { git ->
            git.checkout().addPath(path).call()
        }
    }

    fun stageFile(dir: File, path: String) {
        runGit(dir) { git ->
            git.add().addFilepattern(path).call()
        }
    }

    fun checkoutBranch(dir: File, name: String) {
        runGit(dir) { git ->
            git.checkout().setName(name).call()
        }
    }
    
    fun createBranch(dir: File, name: String) {
        runGit(dir) { git ->
            git.branchCreate().setName(name).call()
        }
    }
    
    fun deleteBranch(dir: File, name: String, force: Boolean = false) {
        runGit(dir) { git ->
            git.branchDelete().setBranchNames(name).setForce(force).call()
        }
    }
    
    fun mergeBranch(dir: File, ref: String) {
        runGit(dir) { git ->
            git.merge().include(git.repository.findRef(ref)).call()
        }
    }

    fun rebaseBranch(dir: File, ref: String) {
        runGit(dir) { git ->
            git.rebase().setUpstream(ref).call()
        }
    }

    fun commit(dir: File, message: String): String {
        runGit(dir) { git ->
            git.commit().setMessage(message).call()
        }
        return "Commit successful"
    }

    fun pull(dir: File): String {
        return runGit(dir) { git ->
            val result = git.pull().call()
            "Success: ${result.isSuccessful}"
        }
    }

    fun push(dir: File): String {
        runGit(dir) { git ->
            git.push().call()
        }
        return "Push command executed"
    }

    fun fetch(dir: File): String {
        runGit(dir) { git ->
            git.fetch().call()
        }
        return "Fetch completed successfully"
    }

    fun getRepoInfo(dir: File): Map<String, String> {
        val info = mutableMapOf<String, String>()
        try {
            runGit(dir) { git ->
                val repo = git.repository
                val config = repo.config
                val remotes = config.getSubsections("remote")
                
                info["Current Branch"] = try { repo.branch ?: "N/A" } catch(_: Exception) { "N/A" }
                info["Status"] = try { if (git.status().call().isClean) "Clean" else "Modified" } catch(_: Exception) { "Unknown" }
                
                remotes.forEach { remote ->
                    val url = config.getString("remote", remote, "url") ?: "No URL"
                    info["Remote: $remote"] = url
                }
                
                val lastCommit = try { git.log().setMaxCount(1).call().firstOrNull() } catch(_: Exception) { null }
                if (lastCommit != null) {
                    info["Last Commit"] = lastCommit.shortMessage
                    info["Last Commit Hash"] = lastCommit.name.take(7)
                    info["Last Commit Date"] = java.util.Date(lastCommit.commitTime.toLong() * 1000L).toString()
                } else {
                    info["Last Commit"] = "None"
                }
                
                info["Local Path"] = dir.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting repo info at ${dir.absolutePath}", e)
            info["Error"] = e.message ?: "Unknown error"
        }
        return info
    }

    fun getRemoteUrl(dir: File, name: String = "origin"): String? {
        return try {
            runGit(dir) { git ->
                git.repository.config.getString("remote", name, "url")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting remote URL at ${dir.absolutePath}", e)
            null
        }
    }

    fun configureRemote(dir: File, name: String, url: String): String {
        return try {
            runGit(dir) { git ->
                val config = git.repository.config
                config.setString("remote", name, "url", url)
                config.setString("remote", name, "fetch", "+refs/heads/*:refs/remotes/$name/*")
                config.save()
                "Remote '$name' configured with URL: $url"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring remote at ${dir.absolutePath}", e)
            "Error: ${e.message}"
        }
    }

    fun hasGitDir(path: String?): Boolean {
        if (path == null) return false
        return File(path, ".git").exists()
    }

    fun cloneRepository(
        uri: String,
        localPath: File,
        branch: String? = null,
        credentials: CloneCredential? = null
    ): String {
        if (localPath.exists() && localPath.listFiles()?.isNotEmpty() == true) {
            throw IllegalStateException("Target directory is not empty")
        }
        if (!localPath.exists()) {
            localPath.mkdirs()
        }

        return try {
            val cloneCommand = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(localPath)
                .setCloneAllBranches(false)
                .setDepth(50)

            branch?.let {
                cloneCommand.setBranch(it)
            }

            credentials?.let { cred ->
                when (cred) {
                    is CloneCredential.Https -> {
                        cloneCommand.setCredentialsProvider(
                            org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(
                                cred.username,
                                cred.password
                            )
                        )
                    }
                    is CloneCredential.Ssh -> {
                        // SSH cloning uses the system's SSH configuration
                        // Private key authentication is handled by JSch
                    }
                }
            }

            cloneCommand.call().use { git ->
                "Cloned repository to ${RepoPathUtils.shortDisplayPath(localPath)}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cloning repository from $uri to ${localPath.absolutePath}", e)
            if (localPath.exists() && localPath.listFiles()?.isEmpty() == true) {
                localPath.delete()
            }
            throw e
        }
    }

    fun getStashList(repoPath: File): List<GitStash> {
        return try {
            runGit(repoPath) { git ->
                val stashList = git.stashList().call()
                stashList.mapIndexed { index, ref ->
                    GitStash(
                        index = index,
                        message = ref.name,
                        branch = "",
                        timestamp = 0L
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stash list at ${repoPath.absolutePath}", e)
            emptyList()
        }
    }

    fun stashChanges(repoPath: File, message: String?): String {
        return try {
            runGit(repoPath) { git ->
                val ref = git.stashCreate().call()
                "Stashed changes: ${ref.name}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stashing changes at ${repoPath.absolutePath}", e)
            throw e
        }
    }

    fun applyStash(repoPath: File, stashIndex: Int, dropAfterApply: Boolean): String {
        return try {
            runGit(repoPath) { git ->
                val stashRef = "refs/stash/$stashIndex"
                if (dropAfterApply) {
                    git.stashApply().setStashRef(stashRef).call()
                    git.stashDrop().setStashRef(stashIndex).call()
                    "Applied and dropped stash $stashIndex"
                } else {
                    git.stashApply().setStashRef(stashRef).call()
                    "Applied stash $stashIndex"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying stash $stashIndex at ${repoPath.absolutePath}", e)
            throw e
        }
    }

    fun dropStash(repoPath: File, stashIndex: Int): String {
        return try {
            runGit(repoPath) { git ->
                git.stashDrop().setStashRef(stashIndex).call()
                "Dropped stash $stashIndex"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dropping stash $stashIndex at ${repoPath.absolutePath}", e)
            throw e
        }
    }

    fun getTags(repoPath: File): List<GitTag> {
        return try {
            runGit(repoPath) { git ->
                val tagList = git.tagList().call()
                tagList.map { ref ->
                    val peel = ref.peeledObjectId
                    val commitId = peel?.name ?: ref.objectId?.name ?: ""
                    GitTag(
                        name = ref.name.removePrefix("refs/tags/"),
                        commitHash = commitId.take(7),
                        message = null,
                        tagger = null,
                        timestamp = 0L
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tags at ${repoPath.absolutePath}", e)
            emptyList()
        }
    }

    fun createTag(repoPath: File, tagName: String, message: String?, commitHash: String?): String {
        return try {
            runGit(repoPath) { git ->
                val repo = git.repository
                val refUpdate = repo.updateRef("refs/tags/$tagName")
                refUpdate.setNewObjectId(repo.resolve(commitHash ?: "HEAD"))
                refUpdate.setRefLogMessage("Tag created by FuwaGit", true)
                refUpdate.update()
                "Tag '$tagName' created"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating tag '$tagName' at ${repoPath.absolutePath}", e)
            throw e
        }
    }

    fun deleteTag(repoPath: File, tagName: String): String {
        return try {
            runGit(repoPath) { git ->
                git.tagDelete().setTags(tagName).call()
                "Tag '$tagName' deleted"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting tag '$tagName' at ${repoPath.absolutePath}", e)
            throw e
        }
    }

    fun getRemotes(repoPath: File): List<GitRemote> {
        return try {
            runGit(repoPath) { git ->
                val config = git.repository.config
                val remoteNames = config.getSubsections("remote")
                remoteNames.map { name ->
                    val fetchUrl = config.getString("remote", name, "url") ?: ""
                    val pushUrl = config.getString("remote", name, "pushurl")
                    GitRemote(
                        name = name,
                        fetchUrl = fetchUrl,
                        pushUrl = pushUrl
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting remotes at ${repoPath.absolutePath}", e)
            emptyList()
        }
    }

    fun deleteRemote(repoPath: File, remoteName: String): String {
        return try {
            runGit(repoPath) { git ->
                git.remoteRemove().setRemoteName(remoteName).call()
                "Remote '$remoteName' deleted"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting remote '$remoteName' at ${repoPath.absolutePath}", e)
            throw e
        }
    }

    fun renameBranch(repoPath: File, oldName: String, newName: String): String {
        return try {
            runGit(repoPath) { git ->
                git.branchRename().setOldName(oldName).setNewName(newName).call()
                "Branch renamed from '$oldName' to '$newName'"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming branch from '$oldName' to '$newName' at ${repoPath.absolutePath}", e)
            throw e
        }
    }

    fun revertCommit(repoPath: File, commitHash: String): String {
        return try {
            runGit(repoPath) { git ->
                val objectId = git.repository.resolve(commitHash)
                git.revert().include(objectId).call()
                "Reverted commit $commitHash"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reverting commit $commitHash at ${repoPath.absolutePath}", e)
            throw e
        }
    }

    fun cherryPick(repoPath: File, commitHash: String): String {
        return try {
            runGit(repoPath) { git ->
                val objectId = git.repository.resolve(commitHash)
                git.cherryPick().include(objectId).call()
                "Cherry-picked commit $commitHash"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cherry-picking commit $commitHash at ${repoPath.absolutePath}", e)
            throw e
        }
    }

    fun clean(repoPath: File, dryRun: Boolean): String {
        return try {
            runGit(repoPath) { git ->
                val result = git.clean().setDryRun(dryRun).call()
                if (dryRun) {
                    "Would clean: ${result.size} items"
                } else {
                    "Cleaned: ${result.size} items"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning at ${repoPath.absolutePath}", e)
            throw e
        }
    }
}
