package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult

interface SshRepository {
    suspend fun testSshConnection(
        host: String,
        privateKeyPem: String,
        passphrase: String?
    ): AppResult<String>
}