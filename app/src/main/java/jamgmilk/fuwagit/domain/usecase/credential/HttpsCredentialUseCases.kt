package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.data.local.credential.HttpsCredential
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class GetHttpsCredentialsUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(): AppResult<List<HttpsCredential>> {
        return repository.getAllHttpsCredentials()
    }
}

class AddHttpsCredentialUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(host: String, username: String, password: String): AppResult<String> {
        return repository.addHttpsCredential(host, username, password)
    }
}

class UpdateHttpsCredentialUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(
        uuid: String,
        host: String? = null,
        username: String? = null,
        password: String? = null
    ): AppResult<Unit> {
        return repository.updateHttpsCredential(uuid, host, username, password)
    }
}

class DeleteHttpsCredentialUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(uuid: String): AppResult<Unit> {
        return repository.deleteHttpsCredential(uuid)
    }
}

class GetHttpsPasswordUseCase @Inject constructor(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(uuid: String): AppResult<String> {
        return repository.getHttpsPassword(uuid)
    }
}
