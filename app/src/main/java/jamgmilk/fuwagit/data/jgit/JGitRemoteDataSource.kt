package jamgmilk.fuwagit.data.jgit

import android.util.Log
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.*
import org.eclipse.jgit.api.Git
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
            val cloneCommand = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(java.io.File(localPath))
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
            android.util.Log.e("JGitRemoteDataSource", "Failed to clone repository", e)
            Result.failure(e)
        } finally {
            core.clearSshCredentials()
        }
    }

    /**
     * Pulls changes from the remote repository.
     */
    override fun pull(repoPath: String, credentials: CloneCredential?): Result<PullResult> =
        core.withGit(repoPath) { git ->
            try {
                val pullCommand = git.pull()
                core.configureCredentials(pullCommand, credentials)
                val pullResult = pullCommand.call()

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
                        conflicts = emptyMap()
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
                                MergeStatus.CONFLICTING -> append("Merge conflicts detected.")
                                else -> append("Merge status: ${mr.mergeStatus}.")
                            }
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
                    hasConflicts = mergeResultInfo?.mergeStatus == MergeStatus.CONFLICTING,
                    detailMessage = detailMessage
                )
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
    ): Result<String> = core.withGit(repoPath) { git ->
        try {
            val pushCommand = git.push().setRemote(options.remote)
            when {
                options.pushAllBranches -> pushCommand.setPushAll()
                options.branch != null -> pushCommand.add(options.branch)
                options.pushCurrentBranch -> pushCommand.add(git.repository.branch)
            }
            if (options.pushTags) pushCommand.setPushTags()
            core.configureCredentials(pushCommand, credentials)
            pushCommand.call()
            "Push completed"
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
        return try {
            Git.open(java.io.File(repoPath)).use { git ->
                git.repository.config.getString("remote", name, "url")
            }
        } catch (e: Exception) {
            android.util.Log.e("JGitRemoteDataSource", "Failed to get remote URL", e)
            null
        }
    }
}
