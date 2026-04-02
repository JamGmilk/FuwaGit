package jamgmilk.fuwagit.data.jgit

import android.util.Log
import com.jcraft.jsch.JSch
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
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.TransportGitSsh
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JGitDataSource @Inject constructor() {

    companion object {
        private const val TAG = "JGitDataSource"
    }

    private fun configureSshSessionFactory(privateKey: String, passphrase: String?) {
        try {
            val jsch = JSch()
            jsch.addIdentity(
                "ssh-key",
                privateKey.toByteArray(),
                null,
                if (passphrase.isNullOrEmpty()) null else passphrase.toByteArray()
            )
            JSch.setConfig("StrictHostKeyChecking", "no")
            JSch.setConfig("PreferredAuthentications", "publickey")

            SshSessionFactory.setInstance(object : SshSessionFactory() {
                private val delegate = SshSessionFactory.getInstance()

                override fun getSession(uri: org.eclipse.jgit.transport.URIish?, credentialsProvider: org.eclipse.jgit.transport.CredentialsProvider?, fs: FS?, tms: Int): org.eclipse.jgit.transport.RemoteSession {
                    return delegate.getSession(uri, credentialsProvider, fs, tms)
                }

                override fun getType(): String = delegate.type
                override fun releaseSession(session: org.eclipse.jgit.transport.RemoteSession?) {
                    delegate.releaseSession(session)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure SSH session factory", e)
        }
    }

    private inline fun <T> withGit(repoPath: String, block: (Git) -> T): Result<T> {
        return try {
            Git.open(File(repoPath)).use { git ->
                Result.success(block(git))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Git operation failed for $repoPath", e)
            Result.failure(e)
        }
    }

    fun initRepo(repoPath: String): Result<String> {
        return try {
            val repoDir = File(repoPath)
            if (!repoDir.exists() && !repoDir.mkdirs()) {
                return Result.failure(Exception("Failed to create directory: $repoPath"))
            }

            FileRepositoryBuilder()
                .setGitDir(File(repoDir, ".git"))
                .setMustExist(false)
                .build().use { repository ->
                    repository.create()
                }

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
            FileRepositoryBuilder()
                .setGitDir(File(repoPath, ".git"))
                .setMustExist(true)
                .build().use { repository ->
                    repository.isBare || repository.directory.exists()
                }
        } catch (e: Exception) {
            false
        }
    }

    fun readRepoStatus(repoPath: String): Result<GitRepoStatus> = withGit(repoPath) { git ->
        val status = git.status().call()
        val repository = git.repository
        
        GitRepoStatus(
            isGitRepo = true,
            branch = repository.fullBranch ?: "",
            hasUncommittedChanges = !status.isClean,
            untrackedCount = status.untracked.size,
            message = if (status.isClean) "Clean" else "Changes detected"
        )
    }

    fun getDetailedStatus(repoPath: String): Result<List<GitFileStatus>> = withGit(repoPath) { git ->
        val status = git.status().call()
        val allFiles = mutableListOf<GitFileStatus>()

        // Staged Changes
        status.added.forEach { path ->
            allFiles.add(GitFileStatus(path, File(path).name, true, GitChangeType.Added))
        }
        status.changed.forEach { path ->
            allFiles.add(GitFileStatus(path, File(path).name, true, GitChangeType.Modified))
        }
        status.removed.forEach { path ->
            allFiles.add(GitFileStatus(path, File(path).name, true, GitChangeType.Removed))
        }

        // Unstaged Changes
        status.modified.forEach { path ->
            allFiles.add(GitFileStatus(path, File(path).name, false, GitChangeType.Modified))
        }
        status.missing.forEach { path ->
            allFiles.add(GitFileStatus(path, File(path).name, false, GitChangeType.Removed))
        }
        status.untracked.forEach { path ->
            allFiles.add(GitFileStatus(path, File(path).name, false, GitChangeType.Untracked))
        }
        status.conflicting.forEach { path ->
            allFiles.add(GitFileStatus(path, File(path).name, false, GitChangeType.Conflicting))
        }

        allFiles
    }

    fun getBranches(repoPath: String): Result<List<GitBranch>> = withGit(repoPath) { git ->
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

    fun getLog(repoPath: String, maxCount: Int = 100): Result<List<GitCommit>> = withGit(repoPath) { git ->
        git.log().setMaxCount(maxCount).call().map { revCommit ->
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
    }

    fun stageAll(repoPath: String): Result<String> = withGit(repoPath) { git ->
        // Use add with update=true to include deletions, and another add for untracked files
        git.add().addFilepattern(".").setUpdate(true).call()
        git.add().addFilepattern(".").call()
        "All changes staged"
    }

    fun unstageAll(repoPath: String): Result<String> = withGit(repoPath) { git ->
        try {
            git.reset().setRef("HEAD").call()
        } catch (e: Exception) {
            // Fallback if HEAD doesn't exist (initial repo)
            git.reset().call()
        }
        "All changes unstaged"
    }

    fun stageFile(repoPath: String, filePath: String): Result<Unit> = withGit(repoPath) { git ->
        val status = git.status().addPath(filePath).call()
        if (status.missing.contains(filePath) || status.removed.contains(filePath)) {
            git.rm().addFilepattern(filePath).call()
        } else {
            git.add().addFilepattern(filePath).call()
        }
        Unit
    }

    fun unstageFile(repoPath: String, filePath: String): Result<Unit> = withGit(repoPath) { git ->
        git.reset().setRef("HEAD").addPath(filePath).call()
        Unit
    }

    fun discardChanges(repoPath: String, filePath: String): Result<Unit> = withGit(repoPath) { git ->
        git.checkout().addPath(filePath).call()
        Unit
    }

    fun commit(repoPath: String, message: String): Result<String> = withGit(repoPath) { git ->
        try {
            val commit = git.commit()
                .setMessage(message)
                .setAllowEmpty(false)
                .call()
            commit.id.name()
        } catch (e: LockFailedException) {
            throw Exception("Cannot commit: repository lock failed. Another process may be operating on this repository.")
        } catch (e: JGitInternalException) {
            throw Exception("Git error: ${e.message}")
        }
    }

    private fun configureCredentials(
        command: org.eclipse.jgit.api.TransportCommand<*, *>,
        credentials: CloneCredential?
    ) {
        when (credentials) {
            is CloneCredential.Https -> {
                command.setCredentialsProvider(
                    UsernamePasswordCredentialsProvider(
                        credentials.username,
                        credentials.password
                    )
                )
            }
            is CloneCredential.Ssh -> {
                configureSshSessionFactory(credentials.privateKey, credentials.passphrase)
            }
            null -> {}
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

            configureCredentials(cloneCommand, credentials)

            if (branch != null) {
                cloneCommand.setBranch(branch)
            }

            val result = cloneCommand.call().use { git ->
                git.repository.directory.parentFile.absolutePath
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clone repository", e)
            Result.failure(e)
        }
    }

    fun pull(repoPath: String, credentials: CloneCredential? = null): Result<PullResult> = withGit(repoPath) { git ->
        val pullCommand = git.pull()
        configureCredentials(pullCommand, credentials)
        val pullResult = pullCommand.call()
        PullResult(
            isSuccessful = pullResult.isSuccessful,
            message = if (pullResult.isSuccessful) "Pull successful" else "Pull failed"
        )
    }

    fun push(repoPath: String, credentials: CloneCredential? = null): Result<String> = withGit(repoPath) { git ->
        val pushCommand = git.push().setPushAll()
        configureCredentials(pushCommand, credentials)
        pushCommand.call()
        "Push completed"
    }

    fun fetch(repoPath: String, credentials: CloneCredential? = null): Result<String> = withGit(repoPath) { git ->
        val fetchCommand = git.fetch().setRemoveDeletedRefs(true)
        configureCredentials(fetchCommand, credentials)
        fetchCommand.call()
        "Fetch completed"
    }

    fun createBranch(repoPath: String, branchName: String): Result<Unit> = withGit(repoPath) { git ->
        git.branchCreate().setName(branchName).call()
        Unit
    }

    fun checkoutBranch(repoPath: String, branchName: String): Result<Unit> = withGit(repoPath) { git ->
        // 检测是否是远程分支 (e.g., "origin/main", "upstream/develop")
        val remoteBranchRegex = Regex("^([^/]+)/(.+)$")
        val matchResult = remoteBranchRegex.find(branchName)
        
        if (matchResult != null) {
            val remoteName = matchResult.groupValues[1]
            val shortBranchName = matchResult.groupValues[2]
            val remoteRefName = "refs/remotes/$branchName"
            
            // 检查该远程分支是否存在
            val remoteRef = git.repository.findRef(remoteRefName)
            if (remoteRef != null) {
                // 检查是否已存在同名的本地分支
                val localRef = git.repository.findRef("refs/heads/$shortBranchName")
                
                if (localRef != null) {
                    // 本地分支已存在，直接 checkout
                    git.checkout().setName(shortBranchName).call()
                } else {
                    // 本地分支不存在，创建 tracking branch 并 checkout
                    git.checkout()
                        .setName(shortBranchName)
                        .setCreateBranch(true)
                        .setStartPoint(remoteRefName)
                        .setUpstreamMode(org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                        .call()
                }
            } else {
                // 远程分支引用不存在，尝试直接 checkout（可能分支名本身包含斜杠）
                git.checkout().setName(branchName).call()
            }
        } else {
            // 本地分支，直接 checkout
            git.checkout().setName(branchName).call()
        }
        Unit
    }

    fun deleteBranch(repoPath: String, branchName: String, force: Boolean = false): Result<Unit> = withGit(repoPath) { git ->
        git.branchDelete().setBranchNames(branchName).setForce(force).call()
        Unit
    }

    fun mergeBranch(repoPath: String, branchName: String): Result<Unit> = withGit(repoPath) { git ->
        git.merge()
            .include(git.repository.findRef(branchName))
            .setCommit(true)
            .call()
        Unit
    }

    fun rebaseBranch(repoPath: String, branchName: String): Result<Unit> = withGit(repoPath) { git ->
        git.rebase().setUpstream(branchName).call()
        Unit
    }

    fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String> = withGit(repoPath) { git ->
        git.branchRename().setOldName(oldName).setNewName(newName).call()
        "Branch renamed to $newName"
    }

    fun configureRemote(repoPath: String, name: String, url: String): Result<String> = withGit(repoPath) { git ->
        val config = git.repository.config
        val exists = config.getSubsections("remote").contains(name)
        
        if (exists) {
            config.setString("remote", name, "url", url)
        } else {
            config.setString("remote", name, "url", url)
        }
        config.save()
        
        if (exists) "Remote $name updated: $url" else "Remote $name added: $url"
    }

    fun deleteRemote(repoPath: String, remoteName: String): Result<String> = withGit(repoPath) { git ->
        git.remoteRemove().setRemoteName(remoteName).call()
        "Remote $remoteName removed"
    }

    fun getRemotes(repoPath: String): Result<List<GitRemote>> = withGit(repoPath) { git ->
        git.remoteList().call().map { remote ->
            GitRemote(
                name = remote.name,
                fetchUrl = remote.urIs.firstOrNull()?.toString() ?: remote.name,
                pushUrl = remote.pushURIs.firstOrNull()?.toString()
            )
        }
    }

    fun getRemoteUrl(repoPath: String, name: String = "origin"): String? {
        return try {
            Git.open(File(repoPath)).use { git ->
                git.repository.config.getString("remote", name, "url")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get remote URL", e)
            null
        }
    }

    fun clean(repoPath: String, dryRun: Boolean = false): Result<String> = withGit(repoPath) { git ->
        git.clean().setCleanDirectories(true).setIgnore(false).setDryRun(dryRun).call()
        "Cleaned"
    }

    fun getRepoInfo(repoPath: String): Map<String, String> {
        val info = mutableMapOf<String, String>()
        try {
            Git.open(File(repoPath)).use { git ->
                val repository = git.repository
                info["path"] = repoPath
                info["gitDir"] = repository.directory.absolutePath
                info["isBare"] = repository.isBare.toString()

                val config = repository.config
                info["user.name"] = config.getString("user", null, "name") ?: "Not set"
                info["user.email"] = config.getString("user", null, "email") ?: "Not set"

                val head = repository.resolve("HEAD")
                if (head != null) {
                    info["HEAD"] = head.name()
                } else {
                    info["HEAD"] = "No commits yet"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get repo info", e)
            info["error"] = e.message ?: "Unknown error"
        }
        return info
    }
}