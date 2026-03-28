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
import jamgmilk.obsigit.domain.usecase.git.GetWorkspaceStatusUseCase
import jamgmilk.obsigit.domain.usecase.git.PullUseCase
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
    
    // Git UseCases - all inject gitRepository
    private val getWorkspaceStatusUseCase: GetWorkspaceStatusUseCase
        get() = GetWorkspaceStatusUseCase(gitRepository)
    
    private val getBranchesUseCase: GetBranchesUseCase
        get() = GetBranchesUseCase(gitRepository)
    
    private val getCommitHistoryUseCase: GetCommitHistoryUseCase
        get() = GetCommitHistoryUseCase(gitRepository)
    
    private val stageAllUseCase: StageAllUseCase
        get() = StageAllUseCase(gitRepository)
    
    private val unstageAllUseCase: UnstageAllUseCase
        get() = UnstageAllUseCase(gitRepository)
    
    private val stageFileUseCase: StageFileUseCase
        get() = StageFileUseCase(gitRepository)
    
    private val unstageFileUseCase: UnstageFileUseCase
        get() = UnstageFileUseCase(gitRepository)
    
    private val commitChangesUseCase: CommitChangesUseCase
        get() = CommitChangesUseCase(gitRepository)
    
    private val pullUseCase: PullUseCase
        get() = PullUseCase(gitRepository)
    
    private val pushUseCase: PushUseCase
        get() = PushUseCase(gitRepository)
    
    // Credential UseCases
    private val saveCredentialUseCase: SaveCredentialUseCase
        get() = SaveCredentialUseCase(credentialRepository)
    
    private val getCredentialsUseCase: GetCredentialsUseCase
        get() = GetCredentialsUseCase(credentialRepository)
    
    private val manageSshKeysUseCase: ManageSshKeysUseCase
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
            pushUseCase = pushUseCase,
            gitRepository = gitRepository
        )
    }
    
    fun createHistoryViewModel(): HistoryViewModel {
        return HistoryViewModel(
            getCommitHistoryUseCase = getCommitHistoryUseCase
        )
    }
    
    fun createBranchesViewModel(): BranchesViewModel {
        return BranchesViewModel(
            getBranchesUseCase = getBranchesUseCase,
            gitRepository = gitRepository
        )
    }
}
