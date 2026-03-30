package jamgmilk.fuwagit.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jamgmilk.fuwagit.data.local.prefs.RepoDataStore
import jamgmilk.fuwagit.data.local.security.MasterKeyManager
import jamgmilk.fuwagit.data.local.security.SecureCredentialStore
import jamgmilk.fuwagit.data.repository.CredentialRepositoryImpl
import jamgmilk.fuwagit.data.repository.GitRepositoryImpl
import jamgmilk.fuwagit.data.source.JGitDataSource
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import jamgmilk.fuwagit.domain.repository.GitRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideJGitDataSource(): JGitDataSource = JGitDataSource()

    @Provides
    @Singleton
    fun provideGitRepository(jGitDataSource: JGitDataSource): GitRepository = GitRepositoryImpl(jGitDataSource)

    @Provides
    @Singleton
    fun provideSecureCredentialStore(
        @ApplicationContext context: Context
    ): SecureCredentialStore = SecureCredentialStore(context)

    @Provides
    @Singleton
    fun provideMasterKeyManager(
        @ApplicationContext context: Context
    ): MasterKeyManager = MasterKeyManager(context)

    @Provides
    @Singleton
    fun provideCredentialRepository(
        @ApplicationContext context: Context
    ): CredentialRepository = CredentialRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideRepoDataStore(
        @ApplicationContext context: Context
    ): RepoDataStore = RepoDataStore(context)
}
