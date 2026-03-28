package jamgmilk.fuwagit.di

import android.content.Context
import jamgmilk.fuwagit.data.repository.CredentialRepositoryImpl
import jamgmilk.fuwagit.data.repository.GitRepositoryImpl
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import jamgmilk.fuwagit.domain.repository.GitRepository
import jamgmilk.fuwagit.domain.usecase.credential.AddHttpsCredentialUseCase
import jamgmilk.fuwagit.domain.usecase.credential.AddSshKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.DeleteHttpsCredentialUseCase
import jamgmilk.fuwagit.domain.usecase.credential.DeleteSshKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsCredentialsUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetHttpsPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshKeysUseCase
import jamgmilk.fuwagit.domain.usecase.credential.GetSshPrivateKeyUseCase
import jamgmilk.fuwagit.domain.usecase.credential.SetupMasterPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.UnlockWithPasswordUseCase
import jamgmilk.fuwagit.domain.usecase.credential.UpdateHttpsCredentialUseCase
import jamgmilk.fuwagit.domain.usecase.git.CheckoutBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.CommitChangesUseCase
import jamgmilk.fuwagit.domain.usecase.git.CreateBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.DeleteBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.DiscardChangesUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetBranchesUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetCommitHistoryUseCase
import jamgmilk.fuwagit.domain.usecase.git.GetWorkspaceStatusUseCase
import jamgmilk.fuwagit.domain.usecase.git.InitRepoUseCase
import jamgmilk.fuwagit.domain.usecase.git.MergeBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.PullUseCase
import jamgmilk.fuwagit.domain.usecase.git.PushUseCase
import jamgmilk.fuwagit.domain.usecase.git.RebaseBranchUseCase
import jamgmilk.fuwagit.domain.usecase.git.StageAllUseCase
import jamgmilk.fuwagit.domain.usecase.git.StageFileUseCase
import jamgmilk.fuwagit.domain.usecase.git.UnstageAllUseCase
import jamgmilk.fuwagit.domain.usecase.git.UnstageFileUseCase
import jamgmilk.fuwagit.domain.usecase.repo.ConfigureRemoteUseCase
import jamgmilk.fuwagit.domain.usecase.repo.GetRemoteUrlUseCase
import jamgmilk.fuwagit.domain.usecase.repo.GetRepoInfoUseCase
import jamgmilk.fuwagit.domain.usecase.repo.HasGitDirUseCase
import jamgmilk.fuwagit.ui.screen.branches.BranchesViewModel
import jamgmilk.fuwagit.ui.screen.credentials.CredentialsStoreViewModel
import jamgmilk.fuwagit.ui.screen.history.HistoryViewModel
import jamgmilk.fuwagit.ui.screen.repo.RepoViewModel
import jamgmilk.fuwagit.ui.screen.status.StatusViewModel

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
    
    private val initRepoUseCase: InitRepoUseCase
        get() = InitRepoUseCase(gitRepository)
    
    private val checkoutBranchUseCase: CheckoutBranchUseCase
        get() = CheckoutBranchUseCase(gitRepository)
    
    private val mergeBranchUseCase: MergeBranchUseCase
        get() = MergeBranchUseCase(gitRepository)
    
    private val rebaseBranchUseCase: RebaseBranchUseCase
        get() = RebaseBranchUseCase(gitRepository)
    
    private val deleteBranchUseCase: DeleteBranchUseCase
        get() = DeleteBranchUseCase(gitRepository)
    
    private val discardChangesUseCase: DiscardChangesUseCase
        get() = DiscardChangesUseCase(gitRepository)
    
    // Repo UseCases
    private val getRepoInfoUseCase: GetRepoInfoUseCase
        get() = GetRepoInfoUseCase(gitRepository)
    
    private val getRemoteUrlUseCase: GetRemoteUrlUseCase
        get() = GetRemoteUrlUseCase(gitRepository)
    
    private val configureRemoteUseCase: ConfigureRemoteUseCase
        get() = ConfigureRemoteUseCase(gitRepository)
    
    private val hasGitDirUseCase: HasGitDirUseCase
        get() = HasGitDirUseCase(gitRepository)
    
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
    
    private val updateHttpsCredentialUseCase: UpdateHttpsCredentialUseCase
        get() = UpdateHttpsCredentialUseCase(credentialRepository)
    
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
            updateHttpsCredentialUseCase = updateHttpsCredentialUseCase,
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
            initRepoUseCase = initRepoUseCase,
            discardChangesUseCase = discardChangesUseCase,
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
            checkoutBranchUseCase = checkoutBranchUseCase,
            createBranchUseCase = createBranchUseCase,
            mergeBranchUseCase = mergeBranchUseCase,
            rebaseBranchUseCase = rebaseBranchUseCase,
            deleteBranchUseCase = deleteBranchUseCase,
            gitRepository = gitRepository
        )
    }
    
    fun createRepoViewModel(): RepoViewModel {
        return RepoViewModel(
            getRepoInfoUseCase = getRepoInfoUseCase,
            getRemoteUrlUseCase = getRemoteUrlUseCase,
            configureRemoteUseCase = configureRemoteUseCase,
            hasGitDirUseCase = hasGitDirUseCase
        )
    }
}
