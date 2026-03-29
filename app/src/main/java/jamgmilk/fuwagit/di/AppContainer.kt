package jamgmilk.fuwagit.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jamgmilk.fuwagit.data.repository.CredentialRepositoryImpl
import jamgmilk.fuwagit.data.repository.GitRepositoryImpl
import jamgmilk.fuwagit.data.source.JGitDataSource
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
import jamgmilk.fuwagit.domain.usecase.git.CheckRepoStatusUseCase
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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideJGitDataSource(): JGitDataSource = JGitDataSource()

    @Provides
    @Singleton
    fun provideGitRepository(jGitDataSource: JGitDataSource): GitRepository = GitRepositoryImpl(jGitDataSource)

    @Provides
    @Singleton
    fun provideCredentialRepository(
        @ApplicationContext context: Context
    ): CredentialRepository = CredentialRepositoryImpl(context)
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideSetupMasterPasswordUseCase(repository: CredentialRepository) =
        SetupMasterPasswordUseCase(repository)

    @Provides
    @Singleton
    fun provideUnlockWithPasswordUseCase(repository: CredentialRepository) =
        UnlockWithPasswordUseCase(repository)

    @Provides
    @Singleton
    fun provideGetHttpsCredentialsUseCase(repository: CredentialRepository) =
        GetHttpsCredentialsUseCase(repository)

    @Provides
    @Singleton
    fun provideAddHttpsCredentialUseCase(repository: CredentialRepository) =
        AddHttpsCredentialUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateHttpsCredentialUseCase(repository: CredentialRepository) =
        UpdateHttpsCredentialUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteHttpsCredentialUseCase(repository: CredentialRepository) =
        DeleteHttpsCredentialUseCase(repository)

    @Provides
    @Singleton
    fun provideGetHttpsPasswordUseCase(repository: CredentialRepository) =
        GetHttpsPasswordUseCase(repository)

    @Provides
    @Singleton
    fun provideGetSshKeysUseCase(repository: CredentialRepository) =
        GetSshKeysUseCase(repository)

    @Provides
    @Singleton
    fun provideAddSshKeyUseCase(repository: CredentialRepository) =
        AddSshKeyUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteSshKeyUseCase(repository: CredentialRepository) =
        DeleteSshKeyUseCase(repository)

    @Provides
    @Singleton
    fun provideGetSshPrivateKeyUseCase(repository: CredentialRepository) =
        GetSshPrivateKeyUseCase(repository)

    @Provides
    @Singleton
    fun provideGetWorkspaceStatusUseCase(repository: GitRepository) =
        GetWorkspaceStatusUseCase(repository)

    @Provides
    @Singleton
    fun provideGetBranchesUseCase(repository: GitRepository) =
        GetBranchesUseCase(repository)

    @Provides
    @Singleton
    fun provideGetCommitHistoryUseCase(repository: GitRepository) =
        GetCommitHistoryUseCase(repository)

    @Provides
    @Singleton
    fun provideStageAllUseCase(repository: GitRepository) =
        StageAllUseCase(repository)

    @Provides
    @Singleton
    fun provideUnstageAllUseCase(repository: GitRepository) =
        UnstageAllUseCase(repository)

    @Provides
    @Singleton
    fun provideStageFileUseCase(repository: GitRepository) =
        StageFileUseCase(repository)

    @Provides
    @Singleton
    fun provideUnstageFileUseCase(repository: GitRepository) =
        UnstageFileUseCase(repository)

    @Provides
    @Singleton
    fun provideCommitChangesUseCase(repository: GitRepository) =
        CommitChangesUseCase(repository)

    @Provides
    @Singleton
    fun providePullUseCase(repository: GitRepository) =
        PullUseCase(repository)

    @Provides
    @Singleton
    fun providePushUseCase(repository: GitRepository) =
        PushUseCase(repository)

    @Provides
    @Singleton
    fun provideCreateBranchUseCase(repository: GitRepository) =
        CreateBranchUseCase(repository)

    @Provides
    @Singleton
    fun provideInitRepoUseCase(repository: GitRepository) =
        InitRepoUseCase(repository)

    @Provides
    @Singleton
    fun provideCheckoutBranchUseCase(repository: GitRepository) =
        CheckoutBranchUseCase(repository)

    @Provides
    @Singleton
    fun provideMergeBranchUseCase(repository: GitRepository) =
        MergeBranchUseCase(repository)

    @Provides
    @Singleton
    fun provideRebaseBranchUseCase(repository: GitRepository) =
        RebaseBranchUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteBranchUseCase(repository: GitRepository) =
        DeleteBranchUseCase(repository)

    @Provides
    @Singleton
    fun provideDiscardChangesUseCase(repository: GitRepository) =
        DiscardChangesUseCase(repository)

    @Provides
    @Singleton
    fun provideGetRepoInfoUseCase(repository: GitRepository) =
        GetRepoInfoUseCase(repository)

    @Provides
    @Singleton
    fun provideGetRemoteUrlUseCase(repository: GitRepository) =
        GetRemoteUrlUseCase(repository)

    @Provides
    @Singleton
    fun provideConfigureRemoteUseCase(repository: GitRepository) =
        ConfigureRemoteUseCase(repository)

    @Provides
    @Singleton
    fun provideHasGitDirUseCase(repository: GitRepository) =
        HasGitDirUseCase(repository)

    @Provides
    @Singleton
    fun provideCheckRepoStatusUseCase(repository: GitRepository) =
        CheckRepoStatusUseCase(repository)
}
