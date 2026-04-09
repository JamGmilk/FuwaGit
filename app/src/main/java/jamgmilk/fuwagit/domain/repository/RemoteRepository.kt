package jamgmilk.fuwagit.domain.repository

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.CloneOptions
import jamgmilk.fuwagit.domain.model.git.GitPushOptions
import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.model.git.PullResult

interface RemoteRepository {
    suspend fun pull(repoPath: String, credentials: CloneCredential? = null): AppResult<PullResult>
    suspend fun push(
        repoPath: String,
        credentials: CloneCredential? = null,
        options: GitPushOptions = GitPushOptions.default()
    ): AppResult<String>
    suspend fun fetch(repoPath: String, credentials: CloneCredential? = null): AppResult<String>
    suspend fun cloneRepository(
        uri: String,
        localPath: String,
        credentials: CloneCredential? = null,
        options: CloneOptions = CloneOptions()
    ): AppResult<String>
    suspend fun getRemoteUrl(localPath: String, name: String = "origin"): String?
    suspend fun configureRemote(localPath: String, name: String, url: String): AppResult<String>
    suspend fun getRemotes(repoPath: String): AppResult<List<GitRemote>>
    suspend fun deleteRemote(repoPath: String, remoteName: String): AppResult<String>
}