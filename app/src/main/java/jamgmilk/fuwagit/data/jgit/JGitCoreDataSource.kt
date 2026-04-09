package jamgmilk.fuwagit.data.jgit

import android.content.Context
import android.util.Log
import jamgmilk.fuwagit.BuildConfig
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.UserInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.util.Arrays
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JGitCoreDataSource @Inject constructor(
    configDataStore: jamgmilk.fuwagit.data.local.prefs.GitConfigDataStore,
    @ApplicationContext private val context: Context
) : GitCoreDataSource {
    override val gitConfigDataStore: jamgmilk.fuwagit.data.local.prefs.GitConfigDataStore = configDataStore

    companion object {
        private const val TAG = "JGitCoreDataSource"
        private const val KNOWN_HOSTS_FILE = "ssh_known_hosts"
        private const val REPOSITORY_ID = "fuwa-git-known-hosts-v1"
        private val currentSshKey = AtomicReference<SshKeyInfo?>()

        data class SshKeyInfo(
            val privateKey: ByteArray,
            val passphrase: ByteArray?
        ) {
            fun secureClear() {
                Arrays.fill(privateKey, 0.toByte())
                passphrase?.let { Arrays.fill(it, 0.toByte()) }
            }
        }

        private const val HOST_KEY_CHANGED = -1
        private const val HOST_KEY_NOT_FOUND = 2
        private const val HOST_KEY_OK = 0
    }

    private val knownHostsFile: File? get() = try {
        File(context.filesDir, KNOWN_HOSTS_FILE)
    } catch (e: Exception) {
        null
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

            val defaultBranch = gitConfigDataStore.configFlow.first().defaultBranch
            val branchName = if (defaultBranch.isNotBlank()) defaultBranch else "main"

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
                clearSshCredentials()

                val privateKeyBytes = credentials.privateKey.toByteArray(Charsets.UTF_8)
                val passphraseBytes = credentials.passphrase?.toByteArray(Charsets.UTF_8)
                currentSshKey.set(SshKeyInfo(privateKeyBytes, passphraseBytes))

                configureSshForCommand(command)
            }
            null -> {}
        }
    }

    private fun configureSshForCommand(command: org.eclipse.jgit.api.TransportCommand<*, *>) {
        val sshInfo = currentSshKey.get() ?: return
        try {
            java.security.Security.addProvider(BouncyCastleProvider())

            com.jcraft.jsch.JSch.setConfig("StrictHostKeyChecking", "yes")
            com.jcraft.jsch.JSch.setConfig("PreferredAuthentications", "publickey")

            val khFile = knownHostsFile

            command.setTransportConfigCallback { transport ->
                if (transport is org.eclipse.jgit.transport.SshTransport) {
                    transport.sshSessionFactory = object : org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory() {
                        override fun createDefaultJSch(fs: org.eclipse.jgit.util.FS?): com.jcraft.jsch.JSch {
                            val jsch = super.createDefaultJSch(fs)
                            try {
                                jsch.removeAllIdentity()

                                jsch.setHostKeyRepository(FuwaHostKeyRepository(khFile))

                                jsch.addIdentity(
                                    "fuwa-git-ssh-key",
                                    sshInfo.privateKey,
                                    null,
                                    sshInfo.passphrase
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to configure SSH identity or known hosts", e)
                            }
                            return jsch
                        }
                    }
                }
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "SSH configured with host key verification enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure SSH", e)
        }
    }

    override fun clearSshCredentials() {
        val oldKey = currentSshKey.getAndSet(null)
        oldKey?.secureClear()
    }

    // ========== Known Hosts Management ==========

    fun getKnownHostsCount(): Int {
        val file = knownHostsFile ?: return 0
        return try {
            if (!file.exists()) 0 else file.readLines().count { !it.startsWith("#") && it.isNotBlank() }
        } catch (e: Exception) {
            0
        }
    }

    fun clearKnownHosts(): Boolean {
        val file = knownHostsFile ?: return false
        return try {
            if (file.exists()) file.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Custom HostKeyRepository implementing accept-new security policy.
     *
     * Behavior:
     * - New host (no stored key) → auto-accept and persist to known_hosts file
     * - Known host with matching key → allow connection (return OK)
     * - Known host with CHANGED key → REJECT and throw JSchException (MITM protection!)
     *
     * This provides equivalent security to OpenSSH's `StrictHostKeyChecking=accept-new`.
     */
    private class FuwaHostKeyRepository(
        private val khFile: File?
    ) : HostKeyRepository {

        private val hostKeys = mutableListOf<HostKey>()

        init {
            loadFromFile()
        }

        private fun loadFromFile() {
            val file = khFile ?: return
            if (!file.exists()) return
            try {
                BufferedReader(file.reader()).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.trim()?.takeIf { !it.startsWith("#") && it.isNotEmpty() }?.let { rawLine ->
                            try {
                                val parts = rawLine.split("\\s+".toRegex())
                                if (parts.size >= 3) {
                                    val host = parts[0]
                                    val typeStr = parts[1]
                                    val typeInt = when (typeStr.lowercase()) {
                                        "ssh-rsa" -> 0
                                        "ssh-dss" -> 1
                                        "ecdsa-sha2-nistp256" -> 19
                                        "ecdsa-sha2-nistp384" -> 20
                                        "ecdsa-sha2-nistp521" -> 21
                                        "ssh-ed25519" -> 3
                                        else -> 0
                                    }
                                    val keyData = base64Decode(parts[2])
                                    hostKeys.add(HostKey(host, typeInt, keyData))
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Skipping invalid known_hosts entry: $rawLine")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load known_hosts from ${file.absolutePath}", e)
            }
        }

        private fun saveToFile() {
            val file = khFile ?: return
            try {
                FileWriter(file).use { writer ->
                    for (hk in hostKeys) {
                        writer.write(hk.host)
                        writer.write(" ")
                        writer.write(hk.type)
                        writer.write(" ")
                        writer.write(hk.key)
                        writer.write("\n")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save known_hosts to ${file.absolutePath}", e)
            }
        }

        private fun base64Encode(data: ByteArray): String {
            return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
        }

        private fun base64Decode(str: String): ByteArray {
            return android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
        }

        override fun getKnownHostsRepositoryID(): String {
            return REPOSITORY_ID
        }

        override fun getHostKey(): Array<out HostKey>? {
            return hostKeys.toTypedArray()
        }

        override fun getHostKey(host: String?, type: String?): Array<out HostKey>? {
            if (host == null) return null
            return if (type != null) {
                hostKeys.filter { it.host == host && it.type == type }.toTypedArray()
            } else {
                hostKeys.filter { it.host == host }.toTypedArray()
            }
        }

        override fun remove(host: String?, type: String?) {
            if (host == null) return
            val iterator = hostKeys.iterator()
            while (iterator.hasNext()) {
                val hk = iterator.next()
                if (hk.host == host && (type == null || hk.type == type)) {
                    iterator.remove()
                }
            }
            saveToFile()
        }

        override fun remove(host: String?, type: String?, key: ByteArray?) {
            if (host == null || key == null) return
            val iterator = hostKeys.iterator()
            while (iterator.hasNext()) {
                val hk = iterator.next()
                val matchesHost = hk.host == host
                val matchesType = type == null || hk.type == type
                val matchesKey = java.util.Arrays.equals(base64Decode(hk.key), key)
                if (matchesHost && matchesType && matchesKey) {
                    iterator.remove()
                }
            }
            saveToFile()
        }

        override fun add(hostKey: HostKey?, ui: UserInfo?) {
            if (hostKey == null) return

            val existingIndex = hostKeys.indexOfFirst {
                it.host == hostKey.host && it.type == hostKey.type
            }

            if (existingIndex >= 0) {
                hostKeys[existingIndex] = hostKey
            } else {
                hostKeys.add(hostKey)
            }
            saveToFile()
        }

        override fun check(host: String?, key: ByteArray?): Int {
            if (host == null || key == null) return HOST_KEY_NOT_FOUND

            val existingKeys = getHostKey(host, null)

            if (existingKeys.isNullOrEmpty()) {
                Log.i(TAG, "New SSH host detected: $host — auto-accepting (accept-new policy)")
                return HOST_KEY_NOT_FOUND
            }

            for (existing in existingKeys) {
                val existingKeyBytes = try { base64Decode(existing.key) } catch (e: Exception) { continue }
                if (java.util.Arrays.equals(existingKeyBytes, key)) {
                    return HOST_KEY_OK
                }
            }

            val keyType = when (key.size) {
                256 -> "ECDSA-256"
                384 -> "ECDSA-384"
                521 -> "ECDSA-521"
                else -> "RSA-${key.size}"
            }
            Log.e(
                TAG,
                "SSH HOST KEY MISMATCH for '$host' ($keyType)! " +
                "Possible MITM attack! Rejecting connection."
            )
            return HOST_KEY_CHANGED
        }
    }
}
