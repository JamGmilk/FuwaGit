package jamgmilk.fuwagit.data.jgit

import android.util.Log
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.*
import org.eclipse.jgit.api.Git
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Git remote operations (clone, pull, push, fetch, remotes).
 */
@Singleton
class JGitRemoteDataSource @Inject constructor(
    private val core: GitCoreDataSource
) : GitRemoteDataSource {
    /**
     * Clones a remote repository.
     */
    override fun cloneRepository(
        uri: String,
        localPath: String,
        credentials: CloneCredential?,
        options: CloneOptions
    ): Result<String> {
        return try {
            val targetDir = java.io.File(localPath)

            if (targetDir.exists() && targetDir.isDirectory) {
                val files = targetDir.listFiles()
                    ?: throw Exception("Cannot access target directory: permission denied or directory deleted")
                if (files.isNotEmpty()) {
                    val hasGitDir = files.any { it.name == ".git" }
                    if (hasGitDir) {
                        throw Exception("Target directory already contains a Git repository. Choose a different directory or remove the existing repository first.")
                    } else {
                        throw Exception("Target directory is not empty. Choose a different directory or remove existing files first.")
                    }
                }
            }

            val cloneCommand = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(targetDir)
                .setCloneAllBranches(options.cloneAllBranches)

            if (options.depth != null && options.depth > 0) {
                cloneCommand.setDepth(options.depth)
            }
            if (options.isBare) {
                cloneCommand.setBare(true)
            }

            core.configureCredentials(cloneCommand, credentials)
            if (options.branch != null) {
                cloneCommand.setBranch(options.branch)
            }

            val result = cloneCommand.call().use { git ->
                git.repository.directory?.parentFile?.absolutePath ?: localPath
            }
            Result.success(result)
        } catch (e: Exception) {
            Log.e("JGitRemoteDataSource", "Failed to clone repository", e)
            Result.failure(e)
        } finally {
            core.clearSshCredentials()
        }
    }

    /**
     * Pulls changes from the remote repository.
     */
    override fun pull(repoPath: String, credentials: CloneCredential?): Result<PullResult> {
        return try {
            core.withGit(repoPath) { git ->
                val status = git.status().call()
                val gitDir = git.repository.directory

                val lockStatus = core.isRepositoryLocked(repoPath)
                if (lockStatus.isLocked) {
                    throw Exception("Cannot pull: ${lockStatus.message}")
                }

                val fullBranch = git.repository.fullBranch
                val isDetached = fullBranch == null || !fullBranch.startsWith("refs/heads/")
                if (isDetached) {
                    throw Exception("Cannot pull in detached HEAD state. Checkout a branch first.")
                }

                val inRebase = File(gitDir, "rebase-apply").exists() ||
                               File(gitDir, "rebase-merge").exists()
                if (inRebase) {
                    throw Exception("Cannot pull: a rebase is in progress. Complete or abort the rebase first.")
                }

                val inMerge = File(gitDir, "MERGE_HEAD").exists()
                if (inMerge) {
                    throw Exception("Cannot pull: a merge is in progress. Complete or abort the merge first.")
                }

                if (status.conflicting.isNotEmpty()) {
                    throw Exception("Cannot pull: you have unresolved merge conflicts. Resolve them first.")
                }

                if (status.hasUncommittedChanges() && (status.added.isNotEmpty() || status.changed.isNotEmpty() || status.removed.isNotEmpty())) {
                    throw Exception("Cannot pull: you have staged changes that would be overwritten by merge. Commit or unstage them first.")
                }

                val remoteConfigured = git.repository.config.getSubsections("remote").isNotEmpty()
                if (!remoteConfigured) {
                    throw Exception("No remote configured. Add a remote with 'git remote add origin <url>' before pulling.")
                }

                val pullCommand = git.pull()
                core.configureCredentials(pullCommand, credentials)
                val pullResult = pullCommand.call()

                val postPullStatus = git.status().call()
                val conflictFiles = postPullStatus.conflicting.map { path ->
                    ConflictFileInfo(
                        path = path,
                        name = java.io.File(path).name
                    )
                }

                val mergeResultInfo = pullResult.mergeResult?.let { merge ->
                    val mergeStatusName = merge.mergeStatus.name
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
                        commitCount = merge.mergedCommits?.size ?: 0,
                        fastForward = mergeStatusName == "FAST_FORWARD",
                        conflicts = if (conflictFiles.isNotEmpty()) {
                            conflictFiles.associate { it.path to 0 }
                        } else {
                            emptyMap()
                        }
                    )
                }

                val rebaseResultInfo = pullResult.rebaseResult?.let { rebase ->
                    RebaseResultDetail(
                        status = when (rebase.status.name) {
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

                val detailMessage = buildString {
                    if (pullResult.isSuccessful) {
                        append("Pull successful. ")
                        mergeResultInfo?.let { mr ->
                            when (mr.mergeStatus) {
                                MergeStatus.ALREADY_UP_TO_DATE -> append("Already up-to-date.")
                                MergeStatus.FAST_FORWARD -> append("Fast-forward merge with ${mr.commitCount} commit(s).")
                                MergeStatus.MERGED -> append("Merged ${mr.commitCount} commit(s).")
                                MergeStatus.CONFLICTING -> append("Merge conflicts detected in ${conflictFiles.size} file(s): ${conflictFiles.joinToString { it.name }}")
                                else -> append("Merge status: ${mr.mergeStatus}.")
                            }
                        }
                        if (conflictFiles.isNotEmpty()) {
                            append(" Conflict files: ${conflictFiles.map { it.path }.joinToString(", ")}")
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
                    hasConflicts = mergeResultInfo?.mergeStatus == MergeStatus.CONFLICTING || conflictFiles.isNotEmpty(),
                    detailMessage = detailMessage
                )
            }
        } finally {
            core.clearSshCredentials()
        }
    }

    /**
     * Pushes changes to the remote repository.
     */
    override fun push(
        repoPath: String,
        credentials: CloneCredential?,
        options: GitPushOptions
    ): Result<String> {
        return try {
            val result = core.withGit(repoPath) { git ->
                val status = git.status().call()

                if (status.hasUncommittedChanges()) {
                    throw Exception("Cannot push: you have uncommitted changes. Commit them first before pushing.")
                }

                if (status.conflicting.isNotEmpty()) {
                    throw Exception("Cannot push: you have unresolved merge conflicts. Resolve them first.")
                }

                val lockStatus = core.isRepositoryLocked(repoPath)
                if (lockStatus.isLocked) {
                    throw Exception("Cannot push: ${lockStatus.message}")
                }

                val remotes = git.repository.config.getSubsections("remote")
                val targetRemote = options.remote
                if (!remotes.contains(targetRemote)) {
                    throw Exception("Remote '$targetRemote' not found. Add it with 'git remote add $targetRemote <url>'.")
                }

                if (!options.forceWithLease && !options.forcePush) {
                    val localRef = git.repository.exactRef("refs/heads/${git.repository.branch}")
                    val remoteRef = git.repository.exactRef("refs/remotes/$targetRemote/${git.repository.branch}")
                    if (localRef != null && remoteRef != null) {
                        val revWalk = org.eclipse.jgit.revwalk.RevWalk(git.repository)
                        val localCommit = revWalk.parseCommit(localRef.objectId)
                        val remoteCommit = revWalk.parseCommit(remoteRef.objectId)
                        val localAncestor = revWalk.isMergedInto(localCommit, remoteCommit)
                        val remoteAncestor = revWalk.isMergedInto(remoteCommit, localCommit)
                        revWalk.dispose()
                        if (!localAncestor && !remoteAncestor) {
                            throw Exception("Branch has diverged from remote '$targetRemote'. Fetch and merge the remote changes, or use force push to overwrite.")
                        }
                    } else if (localRef != null && remoteRef == null) {
                        // remote ref doesn't exist yet — this is a first push, allowed
                    } else if (localRef == null) {
                        val headRevision = try { git.repository.resolve("HEAD") } catch (_: Exception) { null }
                        if (headRevision == null) {
                            throw Exception("Repository has no commits. Make at least one commit before pushing.")
                        }
                    }
                }

                val pushCommand = git.push().setRemote(options.remote)

                // Set branch to push
                when {
                    options.pushAllBranches -> pushCommand.setPushAll()
                    options.branch != null -> pushCommand.add(options.branch)
                    options.pushCurrentBranch -> pushCommand.add(git.repository.branch)
                }

                // Set tag push
                if (options.pushTags) pushCommand.setPushTags()

                if (options.forceWithLease || options.forcePush) {
                    pushCommand.setForce(true)
                }

                core.configureCredentials(pushCommand, credentials)
                pushCommand.call()

                val forceIndicator = when {
                    options.forceWithLease -> " (force-with-lease)"
                    options.forcePush -> " (force)"
                    else -> ""
                }
                "Push completed$forceIndicator"
            }
            result
        } catch (e: org.eclipse.jgit.api.errors.TransportException) {
            // Handle force-with-lease failure (remote has new commits)
            if (options.forceWithLease && e.message?.contains("rejected") == true) {
                Result.failure(Exception("Push rejected: remote branch has new commits. Fetch and merge first, or use force push to override."))
            } else {
                Result.failure(Exception("Push failed: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Push failed: ${e.message}"))
        } finally {
            core.clearSshCredentials()
        }
    }

    /**
     * Fetches changes from the remote repository.
     */
    override fun fetch(repoPath: String, credentials: CloneCredential?): Result<String> =
        core.withGit(repoPath) { git ->
            try {
                if (git.repository.config.getSubsections("remote").isEmpty()) {
                    throw Exception("No remote configured. Add a remote before fetching.")
                }
                val fetchCommand = git.fetch().setRemoveDeletedRefs(true)
                core.configureCredentials(fetchCommand, credentials)
                fetchCommand.call()
                "Fetch completed"
            } finally {
                core.clearSshCredentials()
            }
        }

    /**
     * Configures a remote repository.
     */
    override fun configureRemote(repoPath: String, name: String, url: String): Result<String> =
        core.withGit(repoPath) { git ->
            val config = git.repository.config
            val exists = config.getSubsections("remote").contains(name)
            config.setString("remote", name, "url", url)
            if (!exists) {
                config.setString("remote", name, "fetch", "+refs/heads/*:refs/remotes/$name/*")
            }
            config.save()
            if (exists) "Remote $name updated: $url" else "Remote $name added: $url"
        }

    /**
     * Deletes a remote configuration.
     */
    override fun deleteRemote(repoPath: String, remoteName: String): Result<String> =
        core.withGit(repoPath) { git ->
            git.remoteRemove().setRemoteName(remoteName).call()
            "Remote $remoteName removed"
        }

    /**
     * Lists all configured remotes.
     */
    override fun getRemotes(repoPath: String): Result<List<GitRemote>> =
        core.withGit(repoPath) { git ->
            git.remoteList().call().map { remote ->
                GitRemote(
                    name = remote.name,
                    fetchUrl = remote.urIs.firstOrNull()?.toString() ?: remote.name,
                    pushUrl = remote.pushURIs.firstOrNull()?.toString()
                )
            }
        }

    /**
     * Gets the URL of a specific remote.
     */
    override fun getRemoteUrl(repoPath: String, name: String): String? {
        return core.withGit(repoPath) { git ->
            git.repository.config.getString("remote", name, "url")
        }.getOrNull()
    }
}
