package jamgmilk.fuwagit.domain.usecase.credential

import jamgmilk.fuwagit.credential.store.SshKey
import jamgmilk.fuwagit.domain.model.AppResult
import jamgmilk.fuwagit.domain.repository.CredentialRepository

class GetSshKeysUseCase(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(): AppResult<List<SshKey>> {
        return repository.getAllSshKeys()
    }
}
