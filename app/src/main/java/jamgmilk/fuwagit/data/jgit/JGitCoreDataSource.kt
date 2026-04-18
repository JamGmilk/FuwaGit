package jamgmilk.fuwagit.data.jgit

import android.content.Context
import android.util.Log
import jamgmilk.fuwagit.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JGitCoreDataSource @Inject constructor(
    private val configDataStore: jamgmilk.fuwagit.data.local.prefs.GitConfigDataStore,
    @param:ApplicationContext private val context: Context
) : GitCoreDataSource {
    override val gitConfigDataStore: jamgmilk.fuwagit.data.local.prefs.GitConfigDataStore = configDataStore

    companion object {
        private const val TAG = "JGitCoreDataSource"
    }

    override fun <T> withGit(repoPath: String, block: (Git) -> T): Result<T> {
        return try {
            Git.open(File(repoPath)).use { git ->
                Result.success(block(git))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Git operation failed for $repoPath", e)
            Result.failure(e)
        }
    }

    override suspend fun initRepo(repoPath: String): Result<String> {
        return try {
            val repoDir = File(repoPath)
            if (!repoDir.exists() && !repoDir.mkdirs()) {
                return Result.failure(Exception("Failed to create directory: $repoPath"))
            }

            val branchName = configDataStore.configFlow.first().defaultBranch.ifBlank { "main" }

            FileRepositoryBuilder()
                .setGitDir(File(repoPath, ".git"))
                .setMustExist(false)
                .build().use { repository ->
                    repository.create()

                    // Set default branch name
                    // Note: Before creating the initial commit, HEAD will point to a non-existent branch ref:refs/heads/<branch>
                    // This is normal Git behavior. Before performing any Git operations,
                    // the user must first create an initial commit via git commit to actually create this branch.
                    // Otherwise, operations that reference this branch may fail or produce unexpected results.
                    val headFile = File(repository.directory, "HEAD")
                    headFile.writeText("ref: refs/heads/$branchName\n")

                    if (BuildConfig.DEBUG) Log.d(TAG, "Repository initialized with default branch: $branchName. Remember to create an initial commit!")
                }

            Result.success("Repository initialized at $repoPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init repository", e)
            Result.failure(e)
        }
    }

    override fun hasGitDir(path: String?): Boolean {
        if (path == null) return false
        return try {
            val gitDir = File(path, ".git")
            gitDir.exists() && gitDir.isDirectory
        } catch (e: Exception) {
            false
        }
    }

    override fun isValidRepository(repoPath: String): Boolean {
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

    override fun isRepositoryLocked(repoPath: String): RepositoryLockStatus {
        val gitDir = File(repoPath, ".git")

        if (!gitDir.exists()) {
            return RepositoryLockStatus(
                isLocked = false,
                lockType = LockType.NONE,
                message = ""
            )
        }

        if (File(gitDir, "index.lock").exists()) {
            return RepositoryLockStatus(
                isLocked = true,
                lockType = LockType.INDEX_LOCK,
                message = "Repository is locked by another Git operation (index.lock exists). Close any other Git processes and try again."
            )
        }

        if (File(gitDir, "MERGE_HEAD").exists()) {
            return RepositoryLockStatus(
                isLocked = true,
                lockType = LockType.MERGE_IN_PROGRESS,
                message = "A merge is in progress. Complete or abort the merge first."
            )
        }

        if (File(gitDir, "rebase-apply").exists() || File(gitDir, "rebase-merge").exists()) {
            val isInteractive = File(gitDir, "rebase-interactive").exists()
            return RepositoryLockStatus(
                isLocked = true,
                lockType = LockType.REBASE_IN_PROGRESS,
                message = if (isInteractive) "An interactive rebase is in progress." else "A rebase is in progress. Complete or abort the rebase first."
            )
        }

        if (File(gitDir, "CHERRY_PICK_HEAD").exists()) {
            return RepositoryLockStatus(
                isLocked = true,
                lockType = LockType.CHERRY_PICK_IN_PROGRESS,
                message = "A cherry-pick is in progress. Complete or abort it first."
            )
        }

        if (File(gitDir, "REVERT_HEAD").exists()) {
            return RepositoryLockStatus(
                isLocked = true,
                lockType = LockType.REVERT_IN_PROGRESS,
                message = "A revert is in progress. Complete or abort it first."
            )
        }

        if (File(gitDir, "BISECT_LOG").exists()) {
            return RepositoryLockStatus(
                isLocked = true,
                lockType = LockType.BISECT_IN_PROGRESS,
                message = "A bisect session is in progress. Complete or abort it first."
            )
        }

        if (File(gitDir, "COMMIT_EDITMSG").exists()) {
            return RepositoryLockStatus(
                isLocked = true,
                lockType = LockType.COMMIT_IN_PROGRESS,
                message = "A commit message is being edited. Complete or cancel the commit first."
            )
        }

        if (File(gitDir, "PATCH_HEADER").exists() || File(gitDir, " sequencer").exists()) {
            return RepositoryLockStatus(
                isLocked = true,
                lockType = LockType.PATCH_APPLY_IN_PROGRESS,
                message = "A patch is being applied. Complete or abort the operation first."
            )
        }

        if (File(gitDir, "rebase-apply Sequencing").exists() ||
            File(gitDir, "rebase-merge Sequencing").exists()) {
            return RepositoryLockStatus(
                isLocked = true,
                lockType = LockType.REBASE_SEQUENCE_IN_PROGRESS,
                message = "A rebase sequence is in progress (e.g., exec, label, reset). Complete or abort the rebase first."
            )
        }

        if (File(gitDir, "refs/stash").exists() && File(gitDir, "refs/stash").readText().isNotBlank()) {
            val hasApplying = File(gitDir, "applying").exists()
            if (hasApplying) {
                return RepositoryLockStatus(
                    isLocked = true,
                    lockType = LockType.STASH_APPLY_IN_PROGRESS,
                    message = "A stash apply is in progress. Complete or abort it first."
                )
            }
        }

        return RepositoryLockStatus(
            isLocked = false,
            lockType = LockType.NONE,
            message = ""
        )
    }

    override fun getRepoInfo(repoPath: String): Map<String, String> {
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

    // ========== SSH Credential Helpers ==========

    override fun configureCredentials(
        command: org.eclipse.jgit.api.TransportCommand<*, *>,
        credentials: jamgmilk.fuwagit.domain.model.credential.CloneCredential?,
        skipHostKeyCheck: Boolean
    ) {
        when (credentials) {
            is jamgmilk.fuwagit.domain.model.credential.CloneCredential.Https -> {
                command.setCredentialsProvider(
                    org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(
                        credentials.username,
                        credentials.password
                    )
                )
            }
            is jamgmilk.fuwagit.domain.model.credential.CloneCredential.Ssh -> {
                val privateKeyBytes = credentials.privateKey.toByteArray(Charsets.UTF_8)
                val passphraseBytes = credentials.passphrase?.toByteArray(Charsets.UTF_8)
                configureSshForCommand(command, privateKeyBytes, passphraseBytes, skipHostKeyCheck)
            }
            null -> {}
        }
    }

    private fun configureSshForCommand(
        command: org.eclipse.jgit.api.TransportCommand<*, *>,
        privateKeyBytes: ByteArray,
        passphraseBytes: ByteArray?,
        skipHostKeyCheck: Boolean
    ) {
        try {
            java.security.Security.addProvider(BouncyCastleProvider())

            command.setTransportConfigCallback { transport ->
                if (transport is org.eclipse.jgit.transport.SshTransport) {
                    transport.sshSessionFactory = object : org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory() {
                        override fun createDefaultJSch(fs: org.eclipse.jgit.util.FS?): com.jcraft.jsch.JSch {
                            val jsch = super.createDefaultJSch(fs)
                            try {
                                jsch.removeAllIdentity()
                                Log.i(TAG, "Configuring SSH: skipHostKeyCheck=$skipHostKeyCheck")
                                jsch.hostKeyRepository = HostKeyAskHelper.createRepository(context, skipHostKeyCheck)

                                jsch.addIdentity(
                                    "fuwa-git-ssh-key",
                                    privateKeyBytes,
                                    null,
                                    passphraseBytes
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to configure SSH identity or known hosts", e)
                            } finally {
                                Arrays.fill(privateKeyBytes, 0.toByte())
                                passphraseBytes?.let { Arrays.fill(it, 0.toByte()) }
                            }
                            return jsch
                        }
                    }
                }
            }
            Log.i(TAG, "SSH transport configured for command")
        } catch (e: Exception) {
            Arrays.fill(privateKeyBytes, 0.toByte())
            passphraseBytes?.let { Arrays.fill(it, 0.toByte()) }
            Log.e(TAG, "Failed to configure SSH", e)
        }
    }

    override fun clearSshCredentials() {
        // No-op: credentials are no longer cached, zeroed immediately after use
    }
}
