package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.SshDataSource
import jamgmilk.fuwagit.domain.repository.SshRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SshRepositoryImpl @Inject constructor(
    private val sshDataSource: SshDataSource
) : SshRepository {

    override suspend fun testSshConnection(
        host: String,
        privateKeyPem: String,
        passphrase: String?
    ): AppResult<String> = withContext(Dispatchers.IO) {
        sshDataSource.testSshConnection(host, privateKeyPem, passphrase).toAppResult()
    }
}
