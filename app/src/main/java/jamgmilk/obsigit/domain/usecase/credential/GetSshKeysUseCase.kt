package jamgmilk.obsigit.domain.usecase.credential

import jamgmilk.obsigit.credential.store.SshKey
import jamgmilk.obsigit.domain.model.AppResult
import jamgmilk.obsigit.domain.repository.CredentialRepository

class GetSshKeysUseCase(
    private val repository: CredentialRepository
) {
    suspend operator fun invoke(): AppResult<List<SshKey>> {
        return repository.getAllSshKeys()
    }
}
