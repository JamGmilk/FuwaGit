package jamgmilk.obsigit.di

import android.content.Context
import jamgmilk.obsigit.data.repository.CredentialRepositoryImpl
import jamgmilk.obsigit.data.repository.GitRepositoryImpl
import jamgmilk.obsigit.domain.repository.CredentialRepository
import jamgmilk.obsigit.domain.repository.GitRepository
import jamgmilk.obsigit.domain.usecase.credential.GetCredentialsUseCase
import jamgmilk.obsigit.domain.usecase.credential.ManageSshKeysUseCase
import jamgmilk.obsigit.domain.usecase.credential.SaveCredentialUseCase
import jamgmilk.obsigit.domain.usecase.git.CommitChangesUseCase
import jamgmilk.obsigit.domain.usecase.git.GetCommitHistoryUseCase
import jamgmilk.obsigit.domain.usecase.git.GetRepoStatusUseCase
import jamgmilk.obsigit.domain.usecase.git.ManageBranchesUseCase
import jamgmilk.obsigit.domain.usecase.git.PullChangesUseCase
import jamgmilk.obsigit.domain.usecase.git.PushChangesUseCase
import jamgmilk.obsigit.ui.screen.credentials.CredentialsViewModel

object AppContainer {
    
    private var _gitRepository: GitRepository? = null
    private var _credentialRepository: CredentialRepository? = null
    
    val gitRepository: GitRepository
        get() = _gitRepository ?: GitRepositoryImpl().also { _gitRepository = it }
    
    fun initCredentialRepository(context: Context) {
        if (_credentialRepository == null) {
            _credentialRepository = CredentialRepositoryImpl(context.applicationContext)
        }
    }
    
    val credentialRepository: CredentialRepository
        get() = _credentialRepository ?: throw IllegalStateException("CredentialRepository not initialized. Call initCredentialRepository first.")
    
    // Git UseCases
    val getRepoStatusUseCase: GetRepoStatusUseCase
        get() = GetRepoStatusUseCase(gitRepository)
    
    val getCommitHistoryUseCase: GetCommitHistoryUseCase
        get() = GetCommitHistoryUseCase(gitRepository)
    
    val commitChangesUseCase: CommitChangesUseCase
        get() = CommitChangesUseCase(gitRepository)
    
    val pullChangesUseCase: PullChangesUseCase
        get() = PullChangesUseCase(gitRepository)
    
    val pushChangesUseCase: PushChangesUseCase
        get() = PushChangesUseCase(gitRepository)
    
    val manageBranchesUseCase: ManageBranchesUseCase
        get() = ManageBranchesUseCase(gitRepository)
    
    // Credential UseCases
    val saveCredentialUseCase: SaveCredentialUseCase
        get() = SaveCredentialUseCase(credentialRepository)
    
    val getCredentialsUseCase: GetCredentialsUseCase
        get() = GetCredentialsUseCase(credentialRepository)
    
    val manageSshKeysUseCase: ManageSshKeysUseCase
        get() = ManageSshKeysUseCase(credentialRepository)
    
    // ViewModels
    fun createCredentialsViewModel(): CredentialsViewModel {
        return CredentialsViewModel(
            saveCredentialUseCase = saveCredentialUseCase,
            getCredentialsUseCase = getCredentialsUseCase,
            manageSshKeysUseCase = manageSshKeysUseCase
        )
    }
}
