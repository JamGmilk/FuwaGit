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
import jamgmilk.obsigit.domain.usecase.git.GetBranchesUseCase
import jamgmilk.obsigit.domain.usecase.git.GetCommitHistoryUseCase
import jamgmilk.obsigit.domain.usecase.git.GetRepoStatusUseCase
import jamgmilk.obsigit.domain.usecase.git.GetWorkspaceStatusUseCase
import jamgmilk.obsigit.domain.usecase.git.ManageBranchesUseCase
import jamgmilk.obsigit.domain.usecase.git.PullChangesUseCase
import jamgmilk.obsigit.domain.usecase.git.PullUseCase
import jamgmilk.obsigit.domain.usecase.git.PushChangesUseCase
import jamgmilk.obsigit.domain.usecase.git.PushUseCase
import jamgmilk.obsigit.domain.usecase.git.StageAllUseCase
import jamgmilk.obsigit.domain.usecase.git.StageFileUseCase
import jamgmilk.obsigit.domain.usecase.git.UnstageAllUseCase
import jamgmilk.obsigit.domain.usecase.git.UnstageFileUseCase
import jamgmilk.obsigit.ui.screen.branches.BranchesViewModel
import jamgmilk.obsigit.ui.screen.credentials.CredentialsViewModel
import jamgmilk.obsigit.ui.screen.history.HistoryViewModel
import jamgmilk.obsigit.ui.screen.status.StatusViewModel

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
        get() = GetRepoStatusUseCase()
    
    val getCommitHistoryUseCase: GetCommitHistoryUseCase
        get() = GetCommitHistoryUseCase()
    
    val commitChangesUseCase: CommitChangesUseCase
        get() = CommitChangesUseCase()
    
    val pullChangesUseCase: PullChangesUseCase
        get() = PullChangesUseCase()
    
    val pushChangesUseCase: PushChangesUseCase
        get() = PushChangesUseCase()
    
    val manageBranchesUseCase: ManageBranchesUseCase
        get() = ManageBranchesUseCase()
    
    val getWorkspaceStatusUseCase: GetWorkspaceStatusUseCase
        get() = GetWorkspaceStatusUseCase()
    
    val getBranchesUseCase: GetBranchesUseCase
        get() = GetBranchesUseCase()
    
    val stageAllUseCase: StageAllUseCase
        get() = StageAllUseCase()
    
    val unstageAllUseCase: UnstageAllUseCase
        get() = UnstageAllUseCase()
    
    val stageFileUseCase: StageFileUseCase
        get() = StageFileUseCase()
    
    val unstageFileUseCase: UnstageFileUseCase
        get() = UnstageFileUseCase()
    
    val pullUseCase: PullUseCase
        get() = PullUseCase()
    
    val pushUseCase: PushUseCase
        get() = PushUseCase()
    
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
    
    fun createStatusViewModel(): StatusViewModel {
        return StatusViewModel(
            getWorkspaceStatusUseCase = getWorkspaceStatusUseCase,
            getBranchesUseCase = getBranchesUseCase,
            stageAllUseCase = stageAllUseCase,
            unstageAllUseCase = unstageAllUseCase,
            stageFileUseCase = stageFileUseCase,
            unstageFileUseCase = unstageFileUseCase,
            commitChangesUseCase = commitChangesUseCase,
            pullUseCase = pullUseCase,
            pushUseCase = pushUseCase
        )
    }
    
    fun createHistoryViewModel(): HistoryViewModel {
        return HistoryViewModel()
    }
    
    fun createBranchesViewModel(): BranchesViewModel {
        return BranchesViewModel()
    }
}
