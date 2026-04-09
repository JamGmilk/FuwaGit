package jamgmilk.fuwagit.domain.usecase.git

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
        return sshRepository.testSshConnection(host, privateKeyPem, passphrase)
    }
}