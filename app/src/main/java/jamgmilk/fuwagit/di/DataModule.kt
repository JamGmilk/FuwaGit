package jamgmilk.fuwagit.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jamgmilk.fuwagit.data.jgit.GitCommitDataSource
import jamgmilk.fuwagit.data.jgit.GitCoreDataSource
import jamgmilk.fuwagit.data.jgit.GitDiffDataSource
import jamgmilk.fuwagit.data.jgit.GitMergeDataSource
import jamgmilk.fuwagit.data.jgit.GitOperationCheckDataSource
import jamgmilk.fuwagit.data.jgit.GitRemoteDataSource
import jamgmilk.fuwagit.data.jgit.GitStatusDataSource
import jamgmilk.fuwagit.data.jgit.GitTagDataSource
import jamgmilk.fuwagit.data.jgit.JGitCommitDataSource
import jamgmilk.fuwagit.data.jgit.JGitCoreDataSource
import jamgmilk.fuwagit.data.jgit.JGitDiffDataSource
import jamgmilk.fuwagit.data.jgit.JGitMergeDataSource
import jamgmilk.fuwagit.data.jgit.JGitOperationCheckDataSource
import jamgmilk.fuwagit.data.jgit.JGitRemoteDataSource
import jamgmilk.fuwagit.data.jgit.JGitSshDataSource
import jamgmilk.fuwagit.data.jgit.JGitStatusDataSource
import jamgmilk.fuwagit.data.jgit.JGitTagDataSource
import jamgmilk.fuwagit.data.jgit.SshDataSource
import jamgmilk.fuwagit.data.repository.BiometricRepositoryImpl
import jamgmilk.fuwagit.data.repository.BranchRepositoryImpl
import jamgmilk.fuwagit.data.repository.CommitRepositoryImpl
import jamgmilk.fuwagit.data.repository.ConfigRepositoryImpl
import jamgmilk.fuwagit.data.repository.CoreRepositoryImpl
import jamgmilk.fuwagit.data.repository.CredentialRepositoryImpl
import jamgmilk.fuwagit.data.repository.DiffRepositoryImpl
import jamgmilk.fuwagit.data.repository.GitRepositoryImpl
import jamgmilk.fuwagit.data.repository.MergeRepositoryImpl
import jamgmilk.fuwagit.data.repository.RemoteRepositoryImpl
import jamgmilk.fuwagit.data.repository.RepoRepositoryImpl
import jamgmilk.fuwagit.data.repository.SettingsRepositoryImpl
import jamgmilk.fuwagit.data.repository.SshRepositoryImpl
import jamgmilk.fuwagit.data.repository.StatusRepositoryImpl
import jamgmilk.fuwagit.data.repository.TagRepositoryImpl
import jamgmilk.fuwagit.domain.repository.BiometricRepository
import jamgmilk.fuwagit.domain.repository.BranchRepository
import jamgmilk.fuwagit.domain.repository.CommitRepository
import jamgmilk.fuwagit.domain.repository.ConfigRepository
import jamgmilk.fuwagit.domain.repository.CoreRepository
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import jamgmilk.fuwagit.domain.repository.DiffRepository
import jamgmilk.fuwagit.domain.repository.GitRepository
import jamgmilk.fuwagit.domain.repository.MergeRepository
import jamgmilk.fuwagit.domain.repository.RemoteRepository
import jamgmilk.fuwagit.domain.repository.RepoRepository
import jamgmilk.fuwagit.domain.repository.SettingsRepository
import jamgmilk.fuwagit.domain.repository.SshRepository
import jamgmilk.fuwagit.domain.repository.StatusRepository
import jamgmilk.fuwagit.domain.repository.TagRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindGitRepository(impl: GitRepositoryImpl): GitRepository

    @Binds
    @Singleton
    abstract fun bindStatusRepository(impl: StatusRepositoryImpl): StatusRepository

    @Binds
    @Singleton
    abstract fun bindCommitRepository(impl: CommitRepositoryImpl): CommitRepository

    @Binds
    @Singleton
    abstract fun bindBranchRepository(impl: BranchRepositoryImpl): BranchRepository

    @Binds
    @Singleton
    abstract fun bindRemoteRepository(impl: RemoteRepositoryImpl): RemoteRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindDiffRepository(impl: DiffRepositoryImpl): DiffRepository

    @Binds
    @Singleton
    abstract fun bindCoreRepository(impl: CoreRepositoryImpl): CoreRepository

    @Binds
    @Singleton
    abstract fun bindMergeRepository(impl: MergeRepositoryImpl): MergeRepository

    @Binds
    @Singleton
    abstract fun bindCredentialRepository(impl: CredentialRepositoryImpl): CredentialRepository

    @Binds
    @Singleton
    abstract fun bindGitCoreDataSource(impl: JGitCoreDataSource): GitCoreDataSource

    @Binds
    @Singleton
    abstract fun bindGitStatusDataSource(impl: JGitStatusDataSource): GitStatusDataSource

    @Binds
    @Singleton
    abstract fun bindGitCommitDataSource(impl: JGitCommitDataSource): GitCommitDataSource

    @Binds
    @Singleton
    abstract fun bindGitRemoteDataSource(impl: JGitRemoteDataSource): GitRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindGitMergeDataSource(impl: JGitMergeDataSource): GitMergeDataSource

    @Binds
    @Singleton
    abstract fun bindGitTagDataSource(impl: JGitTagDataSource): GitTagDataSource

    @Binds
    @Singleton
    abstract fun bindGitDiffDataSource(impl: JGitDiffDataSource): GitDiffDataSource

    @Binds
    @Singleton
    abstract fun bindGitOperationCheckDataSource(impl: JGitOperationCheckDataSource): GitOperationCheckDataSource

    @Binds
    @Singleton
    abstract fun bindBiometricRepository(impl: BiometricRepositoryImpl): BiometricRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindRepoRepository(impl: RepoRepositoryImpl): RepoRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSshDataSource(impl: JGitSshDataSource): SshDataSource

    @Binds
    @Singleton
    abstract fun bindSshRepository(impl: SshRepositoryImpl): SshRepository
}