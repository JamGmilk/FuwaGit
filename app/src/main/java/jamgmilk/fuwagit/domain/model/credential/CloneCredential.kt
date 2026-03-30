package jamgmilk.fuwagit.domain.model.credential

sealed class CloneCredential {
    data class Https(
        val username: String,
        val password: String
    ) : CloneCredential()

    data class Ssh(
        val privateKey: String,
        val passphrase: String?
    ) : CloneCredential()
}
