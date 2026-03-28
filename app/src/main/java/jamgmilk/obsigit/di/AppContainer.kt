package jamgmilk.obsigit.di

import android.content.Context
import jamgmilk.obsigit.data.repository.CredentialRepositoryImpl
import jamgmilk.obsigit.data.repository.GitRepositoryImpl
import jamgmilk.obsigit.domain.repository.CredentialRepository
import jamgmilk.obsigit.domain.repository.GitRepository
import jamgmilk.obsigit.domain.usecase.credential.AddHttpsCredentialUseCase
import jamgmilk.obsigit.domain.usecase.credential.AddSshKeyUseCase
import jamgmilk.obsigit.domain.usecase.credential.DeleteHttpsCredentialUseCase
import jamgmilk.obsigit.domain.usecase.credential.DeleteSshKeyUseCase
import jamgmilk.obsigit.domain.usecase.credential.GetHttpsCredentialsUseCase
import jamgmilk.obsigit.domain.usecase.credential.GetHttpsPasswordUseCase
import jamgmilk.obsigit.domain.usecase.credential.GetSshKeysUseCase
import jamgmilk.obsigit.domain.usecase.credential.GetSshPrivateKeyUseCase
import jamgmilk.obsigit.domain.usecase.credential.SetupMasterPasswordUseCase
import jamgmilk.obsigit.domain.usecase.credential.UnlockWithPasswordUseCase
import jamgmilk.obsigit.domain.usecase.git.CommitChangesUseCase
import jamgmilk.obsigit.domain.usecase.git.CreateBranchUseCase
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
import jamgmilk.obsigit.ui.screen.credentials.CredentialsStoreViewModel
import jamgmilk.obsigit.ui.screen.history.HistoryViewModel
import jamgmilk.obsigit.ui.screen.status.StatusViewModel

object AppContainer {
    
    private var _context: Context? = null
    
    fun initialize(context: Context) {
        _context = context.applicationContext
    }
    
    private val context: Context
        get() = _context ?: throw IllegalStateException("AppContainer not initialized")
    
    private var _gitRepository: GitRepository? = null
    private var _credentialRepository: CredentialRepository? = null
    
    val gitRepository: GitRepository
        get() = _gitRepository ?: GitRepositoryImpl().also { _gitRepository = it }
    
    val credentialRepository: CredentialRepository
        get() = _credentialRepository ?: CredentialRepositoryImpl(context).also { _credentialRepository = it }
    
    // Git UseCases
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
    
    private val createBranchUseCase: CreateBranchUseCase
        get() = CreateBranchUseCase(gitRepository)
    
    // Credential UseCases
    private val setupMasterPasswordUseCase: SetupMasterPasswordUseCase
        get() = SetupMasterPasswordUseCase(credentialRepository)
    
    private val unlockWithPasswordUseCase: UnlockWithPasswordUseCase
        get() = UnlockWithPasswordUseCase(credentialRepository)
    
    private val getHttpsCredentialsUseCase: GetHttpsCredentialsUseCase
        get() = GetHttpsCredentialsUseCase(credentialRepository)
    
    private val addHttpsCredentialUseCase: AddHttpsCredentialUseCase
        get() = AddHttpsCredentialUseCase(credentialRepository)
    
    private val deleteHttpsCredentialUseCase: DeleteHttpsCredentialUseCase
        get() = DeleteHttpsCredentialUseCase(credentialRepository)
    
    private val getHttpsPasswordUseCase: GetHttpsPasswordUseCase
        get() = GetHttpsPasswordUseCase(credentialRepository)
    
    private val getSshKeysUseCase: GetSshKeysUseCase
        get() = GetSshKeysUseCase(credentialRepository)
    
    private val addSshKeyUseCase: AddSshKeyUseCase
        get() = AddSshKeyUseCase(credentialRepository)
    
    private val deleteSshKeyUseCase: DeleteSshKeyUseCase
        get() = DeleteSshKeyUseCase(credentialRepository)
    
    private val getSshPrivateKeyUseCase: GetSshPrivateKeyUseCase
        get() = GetSshPrivateKeyUseCase(credentialRepository)
    
    // ViewModels
    fun createCredentialsStoreViewModel(): CredentialsStoreViewModel {
        return CredentialsStoreViewModel(
            credentialRepository = credentialRepository,
            setupMasterPasswordUseCase = setupMasterPasswordUseCase,
            unlockWithPasswordUseCase = unlockWithPasswordUseCase,
            getHttpsCredentialsUseCase = getHttpsCredentialsUseCase,
            addHttpsCredentialUseCase = addHttpsCredentialUseCase,
            deleteHttpsCredentialUseCase = deleteHttpsCredentialUseCase,
            getHttpsPasswordUseCase = getHttpsPasswordUseCase,
            getSshKeysUseCase = getSshKeysUseCase,
            addSshKeyUseCase = addSshKeyUseCase,
            deleteSshKeyUseCase = deleteSshKeyUseCase,
            getSshPrivateKeyUseCase = getSshPrivateKeyUseCase
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
