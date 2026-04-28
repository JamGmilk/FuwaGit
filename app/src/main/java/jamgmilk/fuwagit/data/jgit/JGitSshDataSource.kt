package jamgmilk.fuwagit.data.jgit

import android.content.Context
import android.util.Log
import jamgmilk.fuwagit.BuildConfig
import jamgmilk.fuwagit.core.result.AppException
import dagger.hilt.android.qualifiers.ApplicationContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.security.Security
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JGitSshDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : SshDataSource {

    private val sshTimeout = 15000

    override suspend fun testSshConnection(
        host: String,
        privateKeyPem: String,
        passphrase: String?
    ): Result<String> {
        return try {
            debugLog("Testing SSH connection to $host")

            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
                debugLog("BouncyCastle provider registered")
            } else {
                debugLog("BouncyCastle provider already registered")
            }

            val keyType = validateKeyFormat(privateKeyPem)
            debugLog("Validated key format: $keyType")

            val userHost = host.split("@")
            if (userHost.size != 2) {
                return Result.failure(AppException.Validation("Invalid host format"))
            }

            val username = userHost[0]
            val hostname = userHost[1]

            debugLog("Testing SSH connection to $hostname")

            testSshConnectionDirect(username, hostname, privateKeyPem, passphrase)
        } catch (e: Exception) {
            Log.e("JGitSshDataSource", "SSH test failed: ${e.message}", e)
            Result.failure(AppException.GitOperationFailed("SSH Test", e.message ?: "Connection failed"))
        }
    }

    private fun testSshConnectionDirect(
        username: String,
        hostname: String,
        privateKeyPem: String,
        passphrase: String?
    ): Result<String> {
        val privateKeyBytes = SecureByteArray(privateKeyPem.replace("\r\n", "\n").trim().toByteArray(Charsets.UTF_8))
        val passphraseBytes = passphrase?.let { SecureByteArray(it.toByteArray(Charsets.UTF_8)) }

        val jsch = com.jcraft.jsch.JSch()
        try {
            jsch.addIdentity("fuwa-test-ssh-key", privateKeyBytes.get(), null, passphraseBytes?.get())
            debugLog("JSch identity added")
        } catch (e: Exception) {
            debugLog("JSch addIdentity failed: ${e.message}")
            privateKeyBytes.secureZero()
            passphraseBytes?.secureZero()
            return Result.failure(AppException.GitOperationFailed("SSH Test", "Invalid private key: ${e.message}"))
        }

        val session = jsch.getSession(username, hostname, 22)
        session.setConfig("PreferredAuthentications", "publickey")
        session.setConfig("MaxAuthTries", "3")
        session.timeout = sshTimeout

        jsch.hostKeyRepository = HostKeyAskHelper.createRepository(context)

        var serverBanner = ""

        session.userInfo = object : com.jcraft.jsch.UserInfo {
            override fun getPassphrase(): String? = passphrase
            override fun getPassword(): String? = null
            override fun promptPassword(message: String): Boolean = false
            override fun promptPassphrase(message: String): Boolean = false
            override fun promptYesNo(message: String): Boolean = false
            override fun showMessage(message: String) {
                serverBanner = message
                debugLog("Server banner via showMessage: $message")
            }
        }

        var channel: com.jcraft.jsch.ChannelExec? = null
        var connected = false
        try {
            session.connect(sshTimeout)
            connected = true
            debugLog("SSH session connected to $hostname")

            serverBanner = getServerBanner(session) ?: serverBanner

            channel = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
            val repoPath = getTestRepoPath(hostname)
            channel.setCommand("git-upload-pack $repoPath")
            debugLog("Testing SSH with repo path: $repoPath")

            channel.connect(sshTimeout)
            debugLog("Git exec channel connected to $hostname")

            val exitCode = channel.exitStatus
            debugLog("Channel exit code: $exitCode")

            cleanupConnection(channel, session)
            privateKeyBytes.secureZero()
            passphraseBytes?.secureZero()
            return if (serverBanner.isNotBlank()) {
                Result.success(serverBanner)
            } else {
                Result.success("SSH connection successful to $username@$hostname")
            }
        } catch (e: com.jcraft.jsch.JSchException) {
            debugLog("SSH connection failed: ${e.message}")
            cleanupConnection(channel, session)
            privateKeyBytes.secureZero()
            passphraseBytes?.secureZero()
            return Result.failure(parseJSchException(e, hostname))
        } catch (e: java.net.SocketTimeoutException) {
            debugLog("SSH socket timeout: ${e.message}")
            cleanupConnection(channel, session)
            privateKeyBytes.secureZero()
            passphraseBytes?.secureZero()
            return Result.failure(AppException.GitOperationFailed("SSH Test", "Connection timed out to $hostname - server is not responding on port 22"))
        } catch (e: java.net.ConnectException) {
            debugLog("SSH connection refused: ${e.message}")
            cleanupConnection(channel, session)
            privateKeyBytes.secureZero()
            passphraseBytes?.secureZero()
            return Result.failure(AppException.GitOperationFailed("SSH Test", "Connection refused by $hostname - SSH service may not be running on port 22"))
        } catch (e: java.net.UnknownHostException) {
            debugLog("SSH unknown host: ${e.message}")
            cleanupConnection(channel, session)
            privateKeyBytes.secureZero()
            passphraseBytes?.secureZero()
            return Result.failure(AppException.GitOperationFailed("SSH Test", "Unknown host: $hostname - check hostname spelling"))
        } catch (e: java.io.IOException) {
            debugLog("SSH IO error: ${e.message}")
            cleanupConnection(channel, session)
            privateKeyBytes.secureZero()
            passphraseBytes?.secureZero()
            val msg = e.message ?: "Connection failed"
            val exception = when {
                msg.contains("Connection reset") ->
                    AppException.GitOperationFailed("SSH Test", "Connection was reset by $hostname - try again")
                msg.contains("Broken pipe") ->
                    AppException.GitOperationFailed("SSH Test", "Connection broken - $hostname closed connection")
                else ->
                    AppException.GitOperationFailed("SSH Test", "Network error: $msg")
            }
            return Result.failure(exception)
        } catch (e: Exception) {
            Log.e("JGitSshDataSource", "SSH test unexpected error: ${e.message}", e)
            cleanupConnection(channel, session)
            privateKeyBytes.secureZero()
            passphraseBytes?.secureZero()
            return Result.failure(AppException.GitOperationFailed("SSH Test", "Unexpected error: ${e.message ?: "Unknown error"}"))
        }
    }

    private fun cleanupConnection(channel: com.jcraft.jsch.ChannelExec?, session: com.jcraft.jsch.Session?) {
        try { channel?.disconnect() } catch (_: Exception) { }
        try { session?.disconnect() } catch (_: Exception) { }
    }

    private class SecureByteArray(private val bytes: ByteArray) {
        fun get(): ByteArray = bytes
        fun secureZero() {
            java.util.Arrays.fill(bytes, 0.toByte())
        }
    }

    private fun getServerBanner(session: com.jcraft.jsch.Session): String? {
        val jschSessionClass = session.javaClass
        for (methodName in listOf("getServerBanner", "getBanner", "getLastMessage")) {
            try {
                val method = jschSessionClass.getMethod(methodName)
                val result = method.invoke(session) as? String
                if (!result.isNullOrBlank()) {
                    debugLog("Server banner via $methodName: $result")
                    return result
                }
            } catch (_: NoSuchMethodException) {
            } catch (_: SecurityException) {
            } catch (e: Exception) {
                debugLog("Failed to get server banner via $methodName: ${e.message}")
            }
        }
        return null
    }

    private fun getTestRepoPath(hostname: String): String {
        return when {
            hostname.contains("github.com") -> "/github/gitignore.git"
            hostname.contains("gitlab.com") -> "/gitlab/gitlabhq/gitlab-ce.git"
            hostname.contains("bitbucket.org") -> "/BitBucket/gitignore.git"
            hostname.contains("gitee.com") -> "/git/gitignore.git"
            hostname.contains("sourceforge.net") -> "/p/gitignore/gitignore.git"
            else -> "/$hostname/gitignore.git"
        }
    }

    private fun debugLog(msg: String) { if (BuildConfig.DEBUG) Log.d("JGitSshDataSource", msg) }

    private fun parseJSchException(e: com.jcraft.jsch.JSchException, hostname: String): AppException {
        val message = e.message ?: "Unknown SSH error"

        return when {
            message.contains("Auth fail") || message.contains("authentication failed") ->
                parseAuthenticationFailure(message, hostname)
            message.contains("UnknownHostKey") || message.contains("reject HostKey") || message.contains("HostKeyException") ->
                AppException.GitOperationFailed("SSH Test", "Host key verification failed for $hostname - the server's host key is not trusted")
            message.contains("timeout") || message.contains("Timeout") ->
                AppException.GitOperationFailed("SSH Test", "Connection timed out to $hostname - server is not responding on port 22")
            message.contains("Connection refused") || message.contains("refused") ->
                AppException.GitOperationFailed("SSH Test", "Connection refused by $hostname - SSH service may not be running on port 22")
            message.contains("SocketTimeoutException") ->
                AppException.GitOperationFailed("SSH Test", "Connection timed out to $hostname - server is not responding")
            message.contains("java.net.UnknownHostException") || message.contains("UnknownHostException") ->
                AppException.GitOperationFailed("SSH Test", "Unknown host: $hostname - check hostname spelling")
            message.contains("key is encrypted") || message.contains("passphrase") ->
                AppException.GitOperationFailed("SSH Test", "Private key is encrypted - please provide the correct passphrase")
            message.contains("invalid privatekey") || message.contains("not a valid") || message.contains("not recognized") ->
                AppException.GitOperationFailed("SSH Test", "Invalid private key format - the key cannot be parsed by SSH")
            message.contains("cannot load") || message.contains("Unable to parse") ->
                AppException.GitOperationFailed("SSH Test", "Failed to load private key - format may be invalid: ${e.message}")
            message.contains("USERAUTH fail") || message.contains("Auth cancel") ->
                AppException.GitOperationFailed("SSH Test", "Authentication cancelled or failed - key may not be authorized on $hostname")
            message.contains("channel") && message.contains("closed") ->
                AppException.GitOperationFailed("SSH Test", "Server closed the connection - repository path may not exist on $hostname")
            message.contains("java.net") || message.contains("Socket") ->
                AppException.GitOperationFailed("SSH Test", "Network error: ${e.message}")
            else ->
                AppException.GitOperationFailed("SSH Test", message)
        }
    }

    private fun parseAuthenticationFailure(message: String, hostname: String): AppException {
        return when {
            message.contains("agent") || message.contains("Agent") ->
                AppException.GitOperationFailed("SSH Test", "SSH agent authentication failed - key not available")
            message.contains("identity") || message.contains("Identity") ->
                AppException.GitOperationFailed("SSH Test", "Failed to load private key - the key may be corrupted or have an invalid format")
            message.contains("passphrase") || message.contains("Passphrase") ->
                AppException.GitOperationFailed("SSH Test", "Passphrase is incorrect or required but not provided")
            else ->
                AppException.GitOperationFailed("SSH Test", "Authentication failed - the SSH key may not be authorized on $hostname, or passphrase is incorrect")
        }
    }

    private fun validateKeyFormat(privateKeyPem: String): String {
        try {
            StringReader(privateKeyPem).use { reader ->
                PemReader(reader).use { pemReader ->
                    val pemObject = pemReader.readPemObject()
                        ?: throw IllegalArgumentException("Invalid PEM format - no PEM object found")

                    val type = pemObject.type
                    return when {
                        type.contains("RSA PRIVATE KEY") -> "RSA (PKCS#1)"
                        type == "PRIVATE KEY" -> "Ed25519 or PKCS#8"
                        type.contains("OPENSSH PRIVATE KEY") -> "OpenSSH format"
                        type.contains("EC PRIVATE KEY") -> "EC (ECDSA)"
                        type.contains("DSA PRIVATE KEY") -> "DSA"
                        else -> "Unknown ($type)"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("JGitSshDataSource", "Key format validation failed: ${e.message}", e)
            throw IllegalArgumentException("Invalid private key format: ${e.message}")
        }
    }
}
