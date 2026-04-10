package jamgmilk.fuwagit.data.jgit

interface SshDataSource {
    suspend fun testSshConnection(
        host: String,
        privateKeyPem: String,
        passphrase: String?
    ): Result<String>
}