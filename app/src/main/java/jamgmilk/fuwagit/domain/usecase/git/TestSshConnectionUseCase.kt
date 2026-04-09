package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.repository.SshRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestSshConnectionUseCase @Inject constructor(
    private val sshRepository: SshRepository
) {
    suspend operator fun invoke(
        host: String,
        privateKeyPem: String,
        passphrase: String?
    ): AppResult<String> {
        val validationError = validateInputs(host, privateKeyPem)
        if (validationError != null) return AppResult.Error(validationError)

        return try {
            sshRepository.testSshConnection(host, privateKeyPem, passphrase)
        } catch (e: Exception) {
            AppResult.Error(
                AppException.GitOperationFailed(
                    operation = "SSH Connection Test",
                    message = e.message ?: "Unexpected error during SSH connection test"
                )
            )
        }
    }

    private fun validateInputs(host: String, privateKeyPem: String): AppException? {
        if (host.isBlank()) {
            return AppException.Validation("Host must not be blank")
        }
        val hostParts = host.split("@")
        if (hostParts.size != 2 || hostParts[0].isBlank() || hostParts[1].isBlank()) {
            return AppException.Validation("Host must be in user@hostname format (e.g., git@github.com)")
        }
        if (privateKeyPem.isBlank()) {
            return AppException.Validation("Private key must not be blank")
        }
        val trimmedKey = privateKeyPem.trim()
        if (!trimmedKey.startsWith("-----BEGIN") || !trimmedKey.contains("PRIVATE KEY-----")) {
            return AppException.Validation("Invalid private key format: missing PEM markers")
        }
        return null
    }
}