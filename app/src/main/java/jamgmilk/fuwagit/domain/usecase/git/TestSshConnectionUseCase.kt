package jamgmilk.fuwagit.domain.usecase.git

import android.util.Log
import jamgmilk.fuwagit.BuildConfig
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.security.Security
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext

@Singleton
class TestSshConnectionUseCase @Inject constructor() {

    private val sshTimeout = 15000

    suspend operator fun invoke(
        host: String,
        privateKeyPem: String,
        passphrase: String?
    ): AppResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                debugLog("Testing SSH connection to $host")

                Security.addProvider(BouncyCastleProvider())
                debugLog("BouncyCastle provider registered")

                val keyFormat = validateKeyFormat(privateKeyPem)
                debugLog("Validated key format: $keyFormat")

                val pemObject = parsePemObject(privateKeyPem)
                debugLog("PEM object type: ${pemObject.type}, size: ${pemObject.content.size} bytes")

                val userHost = host.split("@")
                if (userHost.size != 2) return@withContext AppResult.Error(AppException.Validation("Invalid host format"))

                val username = userHost[0]
                val hostname = userHost[1]

                debugLog("Testing SSH connection via JSch to $hostname")

                withTimeout(sshTimeout.toLong()) {
                    testSshConnectionDirect(username, hostname, privateKeyPem, passphrase)
                }
            } catch (e: Exception) {
                Log.e("TestSshConnection", "SSH test failed: ${e.message}", e)
                AppResult.Error(AppException.GitOperationFailed("SSH Test", e.message ?: "Connection failed"))
            }
        }
    }

    private fun testSshConnectionDirect(
        username: String,
        hostname: String,
        privateKeyPem: String,
        passphrase: String?
    ): AppResult<String> {
        val privateKeyBytes = privateKeyPem.replace("\r\n", "\n").trim().toByteArray(Charsets.UTF_8)
        val passphraseBytes = passphrase?.toByteArray(Charsets.UTF_8)

        val jsch = com.jcraft.jsch.JSch()
        try {
            jsch.addIdentity("fuwa-test-ssh-key", privateKeyBytes, null, passphraseBytes)
            debugLog("JSch identity added")
        } catch (e: Exception) {
            debugLog("JSch addIdentity failed: ${e.message}")
            return AppResult.Error(AppException.GitOperationFailed("SSH Test", "Invalid private key: ${e.message}"))
        }

        val session = jsch.getSession(username, hostname, 22)
        session.setConfig("StrictHostKeyChecking", "no")
        session.setConfig("PreferredAuthentications", "publickey")

        try {
            session.connect(sshTimeout)
            debugLog("SSH session connected successfully to $hostname")
            session.disconnect()
            return AppResult.Success("SSH connection successful to $username@$hostname")
        } catch (e: com.jcraft.jsch.JSchException) {
            debugLog("SSH connection failed: ${e.message}")
            session.disconnect()
            return when {
                e.message?.contains("Auth fail") == true ->
                    AppResult.Error(AppException.GitOperationFailed("SSH Test", "Authentication failed - key may not be authorized on $hostname"))
                e.message?.contains("UnknownHostKey") == true || e.message?.contains("reject HostKey") == true ->
                    AppResult.Error(AppException.GitOperationFailed("SSH Test", "Host key verification failed for $hostname"))
                else ->
                    AppResult.Error(AppException.GitOperationFailed("SSH Test", e.message ?: "Connection failed"))
            }
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
            Log.e("TestSshConnection", "Key format validation failed: ${e.message}", e)
            throw IllegalArgumentException("Invalid private key format: ${e.message}")
        }
    }

    private fun debugLog(msg: String) { if (BuildConfig.DEBUG) Log.d("TestSshConnection", msg) }
}
