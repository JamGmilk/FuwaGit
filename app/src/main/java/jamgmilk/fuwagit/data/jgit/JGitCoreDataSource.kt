package jamgmilk.fuwagit.data.jgit

import android.util.Log
import kotlinx.coroutines.flow.first
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core Git data source providing base utilities and repository initialization.
 */
@Singleton
class JGitCoreDataSource @Inject constructor(
    configDataStore: jamgmilk.fuwagit.data.local.prefs.GitConfigDataStore
) {
    // Expose for use by other data sources
    val gitConfigDataStore = configDataStore
    companion object {
        private const val TAG = "JGitCoreDataSource"

        private val currentSshKey = AtomicReference<SshKeyInfo?>()

        data class SshKeyInfo(
            val privateKey: String,
            val passphrase: String?
        )
    }

    /**
     * Executes a Git operation within a scoped Git instance.
     */
    internal inline fun <T> withGit(repoPath: String, block: (Git) -> T): Result<T> {
        return try {
            Git.open(File(repoPath)).use { git ->
                Result.success(block(git))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Git operation failed for $repoPath", e)
            Result.failure(e)
        }
    }

    /**
     * Initializes a new Git repository at the specified path.
     */
    suspend fun initRepo(repoPath: String): Result<String> {
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
                val defaultBranch = gitConfigDataStore.configFlow.first().defaultBranch

                if (defaultBranch.isNotBlank() && defaultBranch != "master") {
                    git.branchCreate().setName(defaultBranch).call()
                    git.checkout().setName(defaultBranch).call()

                    try {
                        git.branchDelete()
                            .setBranchNames("master")
                            .setForce(false)
                            .call()
                        Log.d(TAG, "Deleted original master branch")
                    } catch (e: Exception) {
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

    /**
     * Checks if a directory contains a .git folder.
     */
    fun hasGitDir(path: String?): Boolean {
        if (path == null) return false
        return try {
            val gitDir = File(path, ".git")
            gitDir.exists() && gitDir.isDirectory
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validates if a path is a valid Git repository.
     */
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

    /**
     * Gets basic repository information.
     */
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

    // ========== SSH Credential Helpers ==========

    internal fun configureCredentials(
        command: org.eclipse.jgit.api.TransportCommand<*, *>,
        credentials: jamgmilk.fuwagit.domain.model.credential.CloneCredential?
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

    internal fun clearSshCredentials() {
        currentSshKey.set(null)
    }
}
