package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.toAppResult
import jamgmilk.fuwagit.data.jgit.GitRemoteDataSource
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.CloneOptions
import jamgmilk.fuwagit.domain.model.git.GitPushOptions
import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.model.git.PullResult
import jamgmilk.fuwagit.domain.repository.RemoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RemoteRepositoryImpl @Inject constructor(
    private val remoteDataSource: GitRemoteDataSource
) : RemoteRepository {

    override suspend fun pull(repoPath: String, credentials: CloneCredential?): AppResult<PullResult> =
        withContext(Dispatchers.IO) {
            remoteDataSource.pull(repoPath, credentials).toAppResult()
        }

    override suspend fun push(
        repoPath: String,
        credentials: CloneCredential?,
        options: GitPushOptions
    ): AppResult<String> =
        withContext(Dispatchers.IO) {
            remoteDataSource.push(repoPath, credentials, options).toAppResult()
        }

    override suspend fun fetch(repoPath: String, credentials: CloneCredential?): AppResult<String> =
        withContext(Dispatchers.IO) {
            remoteDataSource.fetch(repoPath, credentials).toAppResult()
        }

    override suspend fun cloneRepository(
        uri: String,
        localPath: String,
        credentials: CloneCredential?,
        options: CloneOptions
    ): AppResult<String> =
        withContext(Dispatchers.IO) {
            remoteDataSource.cloneRepository(uri, localPath, credentials, options).toAppResult()
        }

    override suspend fun getRemoteUrl(localPath: String, name: String): String? =
        withContext(Dispatchers.IO) {
            remoteDataSource.getRemoteUrl(localPath, name)
        }

    override suspend fun configureRemote(localPath: String, name: String, url: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            remoteDataSource.configureRemote(localPath, name, url).toAppResult()
        }

    override suspend fun getRemotes(repoPath: String): AppResult<List<GitRemote>> =
        withContext(Dispatchers.IO) {
            remoteDataSource.getRemotes(repoPath).toAppResult()
        }

    override suspend fun deleteRemote(repoPath: String, remoteName: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            remoteDataSource.deleteRemote(repoPath, remoteName).toAppResult()
        }
}