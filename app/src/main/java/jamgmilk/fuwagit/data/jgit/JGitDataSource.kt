package jamgmilk.fuwagit.data.jgit

import android.util.Log
import jamgmilk.fuwagit.data.local.prefs.GitConfigStore
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.CleanResult
import jamgmilk.fuwagit.domain.model.git.CloneOptions
import jamgmilk.fuwagit.domain.model.git.GitBranch
import jamgmilk.fuwagit.domain.model.git.GitPushOptions
import jamgmilk.fuwagit.domain.model.git.GitChangeType
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitConflict
import jamgmilk.fuwagit.domain.model.git.GitFileStatus
import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.model.git.GitRepoStatus
import jamgmilk.fuwagit.domain.model.git.PullResult
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.git.ConflictStatus
import jamgmilk.fuwagit.domain.model.git.GitResetMode
import jamgmilk.fuwagit.domain.model.git.MergeResultDetail
import jamgmilk.fuwagit.domain.model.git.MergeStatus
import jamgmilk.fuwagit.domain.model.git.RebaseResultDetail
import jamgmilk.fuwagit.domain.model.git.RebaseStatus
import jamgmilk.fuwagit.domain.model.git.GitCommitFileChange
import jamgmilk.fuwagit.domain.model.git.GitCommitDetail
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.errors.LockFailedException
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.merge.MergeResult
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JGitDataSource @Inject constructor(
    private val gitConfigStore: GitConfigStore
) {

    companion object {
        private const val TAG = "JGitDataSource"

        private val currentSshKey = AtomicReference<SshKeyInfo?>()

        data class SshKeyInfo(
            val privateKey: String,
            val passphrase: String?
        )
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

            Git.open(File(repoPath)).use { git ->
                val defaultBranch = gitConfigStore.getConfig().defaultBranch
                
                if (defaultBranch.isNotBlank() && defaultBranch != "master") {
                    // 创建自定义默认分支
                    git.branchCreate().setName(defaultBranch).call()
                    git.checkout().setName(defaultBranch).call()
                    
                    // 删除原始的 master 分支
                    try {
                        git.branchDelete()
                            .setBranchNames("master")
                            .setForce(false)
                            .call()
                        Log.d(TAG, "Deleted original master branch")
                    } catch (e: Exception) {
                        // master 分支可能不存在或无法删除，忽略错误
                        Log.w(TAG, "Could not delete master branch: ${e.message}")
                    }
                    
                    Log.d(TAG, "Created and checked out default branch: $defaultBranch")
                }
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

    /**
     * 获取 commit 的文件变更列表
     */
    fun getCommitFileChanges(repoPath: String, commitHash: String): Result<GitCommitDetail> = withGit(repoPath) { git ->
        val repository = git.repository
        val objectId = repository.resolve(commitHash)
            ?: throw Exception("Commit not found: $commitHash")
        
        val revCommit = repository.parseCommit(objectId)
        
        val fileChanges = mutableListOf<GitCommitFileChange>()
        
        try {
            // 使用 RevWalk 和 Diff 获取文件变更
            val revWalk = org.eclipse.jgit.revwalk.RevWalk(repository)
            try {
                val commit = revWalk.parseCommit(objectId)
                val tree = commit.tree
                
                // 获取父 commit
                val parentCommit = if (commit.parentCount > 0) {
                    revWalk.parseCommit(commit.getParent(0).id)
                } else {
                    null
                }
                
                val parentTree = parentCommit?.tree
                
                if (parentTree != null) {
                    // 使用 DiffFormatter 扫描两个树
                    val outputStream = java.io.ByteArrayOutputStream()
                    val diffFormatter = org.eclipse.jgit.diff.DiffFormatter(outputStream)
                    
                    try {
                        diffFormatter.setRepository(repository)
                        val diffEntries = diffFormatter.scan(parentTree.id, tree.id)
                        
                        for (diffEntry in diffEntries) {
                            val changeTypeName = diffEntry.changeType.name
                            val changeType = when (changeTypeName) {
                                "ADD" -> GitChangeType.Added
                                "DELETE" -> GitChangeType.Removed
                                "MODIFY" -> GitChangeType.Modified
                                "RENAME" -> GitChangeType.Renamed
                                else -> GitChangeType.Modified
                            }
                            
                            val path = diffEntry.newPath.ifBlank { diffEntry.oldPath }
                            val fileName = java.io.File(path).name
                            
                            fileChanges.add(
                                GitCommitFileChange(
                                    path = path,
                                    name = fileName,
                                    changeType = changeType,
                                    additions = 0,
                                    deletions = 0
                                )
                            )
                        }
                    } finally {
                        diffFormatter.close()
                    }
                } else {
                    // 初始 commit，列出所有文件
                    val walk = org.eclipse.jgit.treewalk.TreeWalk(repository)
                    walk.addTree(tree)
                    walk.isRecursive = true
                    
                    while (walk.next()) {
                        val path = walk.pathString
                        fileChanges.add(
                            GitCommitFileChange(
                                path = path,
                                name = java.io.File(path).name,
                                changeType = GitChangeType.Added,
                                additions = 0,
                                deletions = 0
                            )
                        )
                    }
                }
            } finally {
                revWalk.dispose()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get commit file changes: ${e.message}")
        }
        
        // 获取 commit 详情
        val author = revCommit.authorIdent
        val commit = GitCommit(
            hash = revCommit.id.name(),
            shortHash = revCommit.id.abbreviate(7).name(),
            message = revCommit.fullMessage,
            authorName = author.name,
            authorEmail = author.emailAddress,
            timestamp = author.`when`.time,
            parentHashes = revCommit.parents.map { it.name() }
        )
        
        GitCommitDetail(
            commit = commit,
            fileChanges = fileChanges,
            totalAdditions = 0,
            totalDeletions = 0,
            totalFiles = fileChanges.size
        )
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
            val config = gitConfigStore.getConfig()
            if (config.userName.isNotBlank() || config.userEmail.isNotBlank()) {
                val storedConfig = git.repository.config
                if (config.userName.isNotBlank()) {
                    storedConfig.setString("user", null, "name", config.userName)
                }
                if (config.userEmail.isNotBlank()) {
                    storedConfig.setString("user", null, "email", config.userEmail)
                }
                storedConfig.save()
                Log.d(TAG, "Set user config: name=${config.userName}, email=${config.userEmail}")
            }

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

    fun reset(repoPath: String, commitHash: String, mode: GitResetMode): Result<String> = withGit(repoPath) { git ->
        val resetCommand = git.reset()
            .setRef(commitHash)

        // 根据模式设置 reset 类型
        when (mode) {
            GitResetMode.SOFT -> resetCommand.setMode(org.eclipse.jgit.api.ResetCommand.ResetType.SOFT)
            GitResetMode.MIXED -> resetCommand.setMode(org.eclipse.jgit.api.ResetCommand.ResetType.MIXED)
            GitResetMode.HARD -> resetCommand.setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
        }

        resetCommand.call()

        when (mode) {
            GitResetMode.SOFT -> "Reset to $commitHash (soft): HEAD moved, changes kept staged"
            GitResetMode.MIXED -> "Reset to $commitHash (mixed): HEAD moved, changes unstaged"
            GitResetMode.HARD -> "Reset to $commitHash (hard): All changes discarded"
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
                currentSshKey.set(SshKeyInfo(credentials.privateKey, credentials.passphrase))
                configureSshForCommand(command)
            }
            null -> {}
        }
    }

    private fun configureSshForCommand(command: org.eclipse.jgit.api.TransportCommand<*, *>) {
        val sshInfo = currentSshKey.get() ?: return
        try {
            com.jcraft.jsch.JSch.setConfig("StrictHostKeyChecking", "no")
            com.jcraft.jsch.JSch.setConfig("PreferredAuthentications", "publickey")

            command.setTransportConfigCallback { transport ->
                if (transport is org.eclipse.jgit.transport.SshTransport) {
                    transport.sshSessionFactory = object : org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory() {
                        override fun createDefaultJSch(fs: org.eclipse.jgit.util.FS?): com.jcraft.jsch.JSch {
                            val defaultJsch = super.createDefaultJSch(fs)
                            try {
                                defaultJsch.removeAllIdentity()
                                defaultJsch.addIdentity(
                                    "fuwa-git-ssh-key",
                                    sshInfo.privateKey.toByteArray(),
                                    null,
                                    sshInfo.passphrase?.toByteArray()
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to configure default JSch", e)
                            }
                            return defaultJsch
                        }
                    }
                }
            }
            Log.d(TAG, "SSH configured for operation with custom key")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure SSH", e)
        }
    }

    private fun clearSshCredentials() {
        currentSshKey.set(null)
    }

    fun cloneRepository(
        uri: String,
        localPath: String,
        credentials: CloneCredential? = null,
        options: CloneOptions = CloneOptions()
    ): Result<String> {
        return try {
            val cloneCommand = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(File(localPath))
                .setCloneAllBranches(options.cloneAllBranches)

            if (options.depth != null && options.depth > 0) {
                cloneCommand.setDepth(options.depth)
            }

            if (options.isBare) {
                cloneCommand.setBare(true)
            }

            configureCredentials(cloneCommand, credentials)

            if (options.branch != null) {
                cloneCommand.setBranch(options.branch)
            }

            val result = cloneCommand.call().use { git ->
                git.repository.directory?.parentFile?.absolutePath ?: localPath
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clone repository", e)
            Result.failure(e)
        } finally {
            clearSshCredentials()
        }
    }

    fun pull(repoPath: String, credentials: CloneCredential? = null): Result<PullResult> = withGit(repoPath) { git ->
        try {
            val pullCommand = git.pull()
            configureCredentials(pullCommand, credentials)
            val pullResult = pullCommand.call()
            
            // 提取 Merge 结果
            val mergeResultInfo = pullResult.mergeResult?.let { merge ->
                val mergeStatusName = merge.mergeStatus.name
                val commitCount = merge.mergedCommits?.size ?: 0
                val isFastForward = mergeStatusName == "FAST_FORWARD"
                val hasConflicts = mergeStatusName == "CONFLICTING"
                
                MergeResultDetail(
                    mergeStatus = when (mergeStatusName) {
                        "ALREADY_UP_TO_DATE" -> MergeStatus.ALREADY_UP_TO_DATE
                        "FAST_FORWARD" -> MergeStatus.FAST_FORWARD
                        "MERGED" -> MergeStatus.MERGED
                        "FAILED" -> MergeStatus.FAILED
                        "CONFLICTING" -> MergeStatus.CONFLICTING
                        "ABORTED" -> MergeStatus.ABORTED
                        else -> MergeStatus.UNKNOWN
                    },
                    commitCount = commitCount,
                    fastForward = isFastForward,
                    conflicts = emptyMap()
                )
            }
            
            // 提取 Rebase 结果（如果使用了 rebase）
            val rebaseResultInfo = pullResult.rebaseResult?.let { rebase ->
                val rebaseStatusName = rebase.status.name
                
                RebaseResultDetail(
                    status = when (rebaseStatusName) {
                        "UP_TO_DATE" -> RebaseStatus.UP_TO_DATE
                        "FAST_FORWARD" -> RebaseStatus.FAST_FORWARD
                        "OK" -> RebaseStatus.OK
                        "CONFLICTING" -> RebaseStatus.CONFLICTING
                        "ABORTED" -> RebaseStatus.ABORTED
                        "FAILED" -> RebaseStatus.FAILED
                        else -> RebaseStatus.UNKNOWN
                    },
                    commitCount = 0,
                    conflicts = emptyList()
                )
            }
            
            // 构建详细消息
            val detailMessage = buildString {
                if (pullResult.isSuccessful) {
                    append("Pull successful. ")
                    mergeResultInfo?.let { mr ->
                        when (mr.mergeStatus) {
                            MergeStatus.ALREADY_UP_TO_DATE -> append("Already up-to-date.")
                            MergeStatus.FAST_FORWARD -> append("Fast-forward merge with ${mr.commitCount} commit(s).")
                            MergeStatus.MERGED -> append("Merged ${mr.commitCount} commit(s).")
                            MergeStatus.CONFLICTING -> append("Merge conflicts detected: ${mr.conflicts.size} file(s).")
                            else -> append("Merge status: ${mr.mergeStatus}.")
                        }
                    }
                    rebaseResultInfo?.let { rr ->
                        append(" Rebase status: ${rr.status}.")
                    }
                } else {
                    append("Pull failed.")
                }
            }
            
            PullResult(
                isSuccessful = pullResult.isSuccessful,
                message = if (pullResult.isSuccessful) "Pull successful" else "Pull failed",
                mergeResult = mergeResultInfo,
                rebaseResult = rebaseResultInfo,
                hasConflicts = mergeResultInfo?.mergeStatus == MergeStatus.CONFLICTING || 
                            rebaseResultInfo?.status == RebaseStatus.CONFLICTING,
                detailMessage = detailMessage
            )
        } finally {
            clearSshCredentials()
        }
    }

    fun push(
        repoPath: String,
        credentials: CloneCredential? = null,
        options: GitPushOptions = GitPushOptions.default()
    ): Result<String> = withGit(repoPath) { git ->
        try {
            val pushCommand = git.push().setRemote(options.remote)

            when {
                options.pushAllBranches -> {
                    pushCommand.setPushAll()
                }
                options.branch != null -> {
                    pushCommand.add(options.branch)
                }
                options.pushCurrentBranch -> {
                    pushCommand.add(git.repository.branch)
                }
            }

            if (options.pushTags) {
                pushCommand.setPushTags()
            }

            configureCredentials(pushCommand, credentials)
            pushCommand.call()
            "Push completed"
        } finally {
            clearSshCredentials()
        }
    }

    fun fetch(repoPath: String, credentials: CloneCredential? = null): Result<String> = withGit(repoPath) { git ->
        try {
            val fetchCommand = git.fetch().setRemoveDeletedRefs(true)
            configureCredentials(fetchCommand, credentials)
            fetchCommand.call()
            "Fetch completed"
        } finally {
            clearSshCredentials()
        }
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

    fun mergeBranch(repoPath: String, branchName: String): Result<ConflictResult> = withGit(repoPath) { git ->
        try {
            val mergeResult = git.merge()
                .include(git.repository.findRef(branchName))
                .setCommit(true)
                .call()

            // 检查是否有冲突
            val conflicts = mergeResult.conflicts
            val hasConflicts = conflicts != null && conflicts.isNotEmpty()
            
            if (hasConflicts) {
                // 有冲突
                val conflictFiles = getConflictFiles(git)
                ConflictResult(
                    isConflicting = true,
                    operationType = "MERGE",
                    conflicts = conflictFiles,
                    message = "Merge conflict: ${conflictFiles.size} file(s) need resolution"
                )
            } else if (mergeResult.mergeStatus.name == "FAST_FORWARD" || mergeResult.mergeStatus.name == "MERGED") {
                // 成功合并
                ConflictResult(
                    isConflicting = false,
                    operationType = "MERGE",
                    message = "Merge successful"
                )
            } else {
                ConflictResult(
                    isConflicting = false,
                    operationType = "MERGE",
                    message = "Merge completed: ${mergeResult.mergeStatus.name}"
                )
            }
        } catch (e: Exception) {
            throw Exception("Merge failed: ${e.message}")
        }
    }

    fun rebaseBranch(repoPath: String, branchName: String): Result<ConflictResult> = withGit(repoPath) { git ->
        try {
            val rebaseResult = git.rebase()
                .setUpstream(branchName)
                .call()

            // 检查是否有冲突
            val hasConflicts = rebaseResult.status.name == "CONFLICTING"
            
            if (hasConflicts) {
                // 有冲突
                val conflictFiles = getConflictFiles(git)
                ConflictResult(
                    isConflicting = true,
                    operationType = "REBASE",
                    conflicts = conflictFiles,
                    message = "Rebase conflict: ${conflictFiles.size} file(s) need resolution"
                )
            } else if (rebaseResult.status.name == "UP_TO_DATE" || 
                       rebaseResult.status.name == "FAST_FORWARD" || 
                       rebaseResult.status.name == "OK") {
                // 成功
                ConflictResult(
                    isConflicting = false,
                    operationType = "REBASE",
                    message = "Rebase successful"
                )
            } else {
                ConflictResult(
                    isConflicting = false,
                    operationType = "REBASE",
                    message = "Rebase completed: ${rebaseResult.status.name}"
                )
            }
        } catch (e: Exception) {
            throw Exception("Rebase failed: ${e.message}")
        }
    }

    /**
     * 获取冲突文件列表
     */
    private fun getConflictFiles(git: Git): List<GitConflict> {
        val status = git.status().call()
        val conflicts = mutableListOf<GitConflict>()

        // JGit 中冲突文件会在 conflicting 列表中
        status.conflicting.forEach { path ->
            conflicts.add(
                GitConflict(
                    path = path,
                    name = java.io.File(path).name,
                    status = ConflictStatus.UNRESOLVED,
                    description = "Conflict"
                )
            )
        }

        return conflicts
    }

    /**
     * 检查当前是否有未解决的冲突
     */
    fun getConflictStatus(repoPath: String): Result<ConflictResult> = withGit(repoPath) { git ->
        val status = git.status().call()

        if (status.conflicting.isNotEmpty()) {
            // 尝试检测是 merge 还是 rebase
            val gitDir = git.repository.directory
            val isMerge = java.io.File(gitDir, "MERGE_HEAD").exists()
            val isRebase = java.io.File(gitDir, "rebase-apply").exists() || java.io.File(gitDir, "rebase-merge").exists()
            
            val operationType = when {
                isMerge -> "MERGE"
                isRebase -> "REBASE"
                else -> "UNKNOWN"
            }
            
            val conflicts = getConflictFiles(git)
            ConflictResult(
                isConflicting = true,
                operationType = operationType,
                conflicts = conflicts,
                message = "${conflicts.size} conflict(s) need resolution"
            )
        } else {
            ConflictResult(
                isConflicting = false,
                message = "No conflicts"
            )
        }
    }

    /**
     * 标记冲突文件为已解决（添加到暂存区）
     */
    fun markConflictResolved(repoPath: String, filePath: String): Result<Unit> = withGit(repoPath) { git ->
        // 将文件添加到暂存区表示冲突已解决
        git.add().addFilepattern(filePath).call()
        Unit
    }

    /**
     * 取消 rebase 操作
     */
    fun abortRebase(repoPath: String): Result<String> = withGit(repoPath) { git ->
        git.rebase().setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT).call()
        "Rebase aborted"
    }

    fun renameBranch(repoPath: String, oldName: String, newName: String): Result<String> = withGit(repoPath) { git ->
        git.branchRename().setOldName(oldName).setNewName(newName).call()
        "Branch renamed to $newName"
    }

    fun configureRemote(repoPath: String, name: String, url: String): Result<String> = withGit(repoPath) { git ->
        val config = git.repository.config
        val exists = config.getSubsections("remote").contains(name)

        // 设置远程仓库 URL
        config.setString("remote", name, "url", url)
        
        // 如果是新远程仓库，同时设置 fetch 配置
        if (!exists) {
            config.setString("remote", name, "fetch", "+refs/heads/*:refs/remotes/$name/*")
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

    fun clean(repoPath: String, dryRun: Boolean = false): Result<CleanResult> = withGit(repoPath) { git ->
        val cleanedPaths = git.clean()
            .setCleanDirectories(true)
            .setIgnore(false)
            .setDryRun(dryRun)
            .call()
        CleanResult(files = cleanedPaths.toList(), isDryRun = dryRun)
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