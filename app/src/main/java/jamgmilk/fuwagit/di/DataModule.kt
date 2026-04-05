package jamgmilk.fuwagit.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jamgmilk.fuwagit.data.jgit.GitCommitDataSource
import jamgmilk.fuwagit.data.jgit.GitCoreDataSource
import jamgmilk.fuwagit.data.jgit.GitMergeDataSource
import jamgmilk.fuwagit.data.jgit.GitRemoteDataSource
import jamgmilk.fuwagit.data.jgit.GitStatusDataSource
import jamgmilk.fuwagit.data.jgit.JGitCommitDataSource
import jamgmilk.fuwagit.data.jgit.JGitCoreDataSource
import jamgmilk.fuwagit.data.jgit.JGitMergeDataSource
import jamgmilk.fuwagit.data.jgit.JGitRemoteDataSource
import jamgmilk.fuwagit.data.jgit.JGitStatusDataSource
import jamgmilk.fuwagit.data.repository.BiometricRepositoryImpl
import jamgmilk.fuwagit.data.repository.ConfigRepositoryImpl
import jamgmilk.fuwagit.data.repository.CredentialRepositoryImpl
import jamgmilk.fuwagit.data.repository.GitRepositoryImpl
import jamgmilk.fuwagit.data.repository.RepoRepositoryImpl
import jamgmilk.fuwagit.data.repository.SettingsRepositoryImpl
import jamgmilk.fuwagit.domain.repository.BiometricRepository
import jamgmilk.fuwagit.domain.repository.ConfigRepository
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import jamgmilk.fuwagit.domain.repository.GitRepository
import jamgmilk.fuwagit.domain.repository.RepoRepository
import jamgmilk.fuwagit.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindGitRepository(impl: GitRepositoryImpl): GitRepository

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
}
