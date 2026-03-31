package jamgmilk.fuwagit.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jamgmilk.fuwagit.data.repository.CredentialRepositoryImpl
import jamgmilk.fuwagit.data.repository.GitRepositoryImpl
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import jamgmilk.fuwagit.domain.repository.GitRepository
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
}
