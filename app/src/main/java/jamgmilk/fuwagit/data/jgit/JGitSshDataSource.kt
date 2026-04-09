package jamgmilk.fuwagit.data.jgit

import android.util.Log
import jamgmilk.fuwagit.BuildConfig
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemReader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader
import java.security.Security
import javax.inject.Inject
import javax.inject.Singleton

interface SshDataSource {
    suspend fun testSshConnection(
        host: String,
        privateKeyPem: String,
        passphrase: String?
    ): Result<String>
}

@Singleton
class JGitSshDataSource @Inject constructor() : SshDataSource {

    private val sshTimeout = 15000

    override suspend fun testSshConnection(
        host: String,
        privateKeyPem: String,
        passphrase: String?
    ): Result<String> {
        return try {
            debugLog("Testing SSH connection to $host")

            Security.addProvider(BouncyCastleProvider())
            debugLog("BouncyCastle provider registered")

            validateKeyFormat(privateKeyPem)
            debugLog("Validated key format")

            parsePemObject(privateKeyPem)
            debugLog("PEM object parsed")

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

    private suspend fun testSshConnectionDirect(
        username: String,
        hostname: String,
        privateKeyPem: String,
        passphrase: String?
    ): Result<String> {
        val privateKeyBytes = privateKeyPem.replace("\r\n", "\n").trim().toByteArray(Charsets.UTF_8)
        val passphraseBytes = passphrase?.toByteArray(Charsets.UTF_8)

        val jsch = com.jcraft.jsch.JSch()
        try {
            jsch.addIdentity("fuwa-test-ssh-key", privateKeyBytes, null, passphraseBytes)
            debugLog("JSch identity added")
        } catch (e: Exception) {
            debugLog("JSch addIdentity failed: ${e.message}")
            return Result.failure(AppException.GitOperationFailed("SSH Test", "Invalid private key: ${e.message}"))
        }

        val session = jsch.getSession(username, hostname, 22)
        session.setConfig("StrictHostKeyChecking", "no")
        session.setConfig("PreferredAuthentications", "publickey")

        var serverBanner = ""

        session.setUserInfo(object : com.jcraft.jsch.UserInfo {
            override fun getPassphrase(): String? = passphrase
            override fun getPassword(): String? = null
            override fun promptPassword(message: String): Boolean = false
            override fun promptPassphrase(message: String): Boolean = false
            override fun promptYesNo(message: String): Boolean = false
            override fun showMessage(message: String) {
                serverBanner = message
                debugLog("Server banner via showMessage: $message")
            }
        })

        var channel: com.jcraft.jsch.ChannelExec? = null
        return try {
            session.connect(sshTimeout)
            debugLog("SSH session connected to $hostname")

            try {
                val getBannerMethod = session.javaClass.getMethod("getServerBanner")
                val banner = getBannerMethod.invoke(session) as? String
                if (!banner.isNullOrBlank()) {
                    debugLog("Server banner via getServerBanner: $banner")
                    if (serverBanner.isBlank()) serverBanner = banner
                }
            } catch (_: Exception) { }

            try {
                val getBannerMethod = session.javaClass.getMethod("getBanner")
                val banner = getBannerMethod.invoke(session) as? String
                if (!banner.isNullOrBlank()) {
                    debugLog("Server banner via getBanner: $banner")
                    if (serverBanner.isBlank()) serverBanner = banner
                }
            } catch (_: Exception) { }

            try {
                val getMsgMethod = session.javaClass.getMethod("getLastMessage")
                val msg = getMsgMethod.invoke(session) as? String
                if (!msg.isNullOrBlank()) {
                    debugLog("Last message: $msg")
                    if (serverBanner.isBlank()) serverBanner = msg
                }
            } catch (_: Exception) { }

            channel = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
            channel.setCommand("git-upload-pack /github/gitignore.git")

            val outputBuilder = StringBuilder()
            val stdoutReader = BufferedReader(InputStreamReader(channel.inputStream))

            val workerScope = CoroutineScope(Dispatchers.IO + Job())
            val readerJob: Job = workerScope.launch {
                try {
                    var line: String?
                    while (stdoutReader.readLine().also { line = it } != null) {
                        outputBuilder.appendLine(line)
                    }
                } catch (_: Exception) { }
            }

            channel.connect(sshTimeout)

            readerJob.join()

            val exitCode = channel.exitStatus
            debugLog("Channel exit code: $exitCode")

            channel.disconnect()
            channel = null
            session.disconnect()

            val output = outputBuilder.toString().trim()
            debugLog("Git protocol output length: ${output.length}")

            if (serverBanner.isNotBlank()) {
                Result.success(serverBanner)
            } else {
                Result.success("SSH connection successful to $username@$hostname")
            }
        } catch (e: com.jcraft.jsch.JSchException) {
            debugLog("SSH connection failed: ${e.message}")
            channel?.disconnect()
            channel = null
            try { session.disconnect() } catch (_: Exception) { }
            val result = when {
                e.message?.contains("Auth fail") == true ->
                    AppException.GitOperationFailed("SSH Test", "Authentication failed - key may not be authorized on $hostname")
                e.message?.contains("UnknownHostKey") == true || e.message?.contains("reject HostKey") == true ->
                    AppException.GitOperationFailed("SSH Test", "Host key verification failed for $hostname")
                else ->
                    AppException.GitOperationFailed("SSH Test", e.message ?: "Connection failed")
            }
            Result.failure(result)
        }
    }

    private fun parsePemObject(pemContent: String): org.bouncycastle.util.io.pem.PemObject {
        return StringReader(pemContent).use { reader ->
            PemReader(reader).use { pemReader ->
                pemReader.readPemObject()
                    ?: throw IllegalArgumentException("No PEM object found")
            }
        }
    }

    private fun validateKeyFormat(privateKeyPem: String): String {
        try {
            StringReader(privateKeyPem).use { reader ->
                PemReader(reader).use { pemReader ->
                    val pemObject = pemReader.readPemObject()
                    if (pemObject == null) {
                        throw IllegalArgumentException("Invalid PEM format - no PEM object found")
                    }

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

    private fun debugLog(msg: String) { if (BuildConfig.DEBUG) Log.d("JGitSshDataSource", msg) }
}