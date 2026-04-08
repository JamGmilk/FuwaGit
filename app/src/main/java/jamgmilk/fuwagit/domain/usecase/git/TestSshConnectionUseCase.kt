package jamgmilk.fuwagit.domain.usecase.git

import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to test SSH key connectivity by connecting to a remote SSH server.
 * Similar to `ssh -T git@github.com`
 */
@Singleton
class TestSshConnectionUseCase @Inject constructor() {

    private val sshTimeout = 15000 // 15 seconds

    /**
     * Test SSH connection to the given host using the provided private key.
     *
     * @param host The SSH host to connect to (e.g., "git@github.com")
     * @param privateKey The private key content
     * @param passphrase Optional passphrase for the private key
     * @return AppResult containing the server response message
     */
    suspend operator fun invoke(
        host: String,
        privateKey: String,
        passphrase: String?
    ): AppResult<String> {
        return try {
            Log.d("TestSshConnection", "Testing SSH connection to $host")
            
            val jsch = JSch()
            
            // Add the private key
            val keyName = "test-ssh-key"
            if (passphrase.isNullOrBlank()) {
                jsch.addIdentity(keyName, privateKey.toByteArray(), null, null)
            } else {
                jsch.addIdentity(keyName, privateKey.toByteArray(), null, passphrase.toByteArray())
            }
            
            // Parse host and user
            val userHost = host.split("@")
            if (userHost.size != 2) {
                return AppResult.Error(AppException.Validation("Invalid host format. Expected: user@host"))
            }
            
            val user = userHost[0]
            val hostname = userHost[1]
            
            Log.d("TestSshConnection", "Connecting to $hostname as $user")
            
            // Create session
            val session: Session = jsch.getSession(user, hostname, 22)
            
            // Disable strict host key checking for testing
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect(sshTimeout)
            
            Log.d("TestSshConnection", "SSH connection established")
            
            // Try to execute a simple command or get server banner
            val channel = session.openChannel("exec")
            (channel as com.jcraft.jsch.ChannelExec).setCommand("echo 'SSH connection successful'")
            channel.setInputStream(null)
            
            val outputStream = java.io.ByteArrayOutputStream()
            channel.setOutputStream(outputStream)
            
            channel.connect()
            
            // Wait for channel to close
            while (!channel.isClosed) {
                Thread.sleep(100)
            }
            
            val response = outputStream.toString("UTF-8").trim()
            val exitStatus = channel.exitStatus
            
            channel.disconnect()
            session.disconnect()
            
            Log.d("TestSshConnection", "SSH test completed. Exit status: $exitStatus")
            
            if (exitStatus == 0) {
                AppResult.Success(response.ifBlank { "SSH connection successful" })
            } else {
                AppResult.Success("Connected (exit status: $exitStatus)\n$response")
            }
            
        } catch (e: com.jcraft.jsch.JSchException) {
            Log.e("TestSshConnection", "SSH test failed: ${e.message}", e)
            AppResult.Error(AppException.GitOperationFailed("SSH Test", e.message ?: "Connection failed"))
        } catch (e: Exception) {
            Log.e("TestSshConnection", "Unexpected error during SSH test: ${e.message}", e)
            AppResult.Error(AppException.Unknown(e.message ?: "Unexpected error"))
        }
    }
}
