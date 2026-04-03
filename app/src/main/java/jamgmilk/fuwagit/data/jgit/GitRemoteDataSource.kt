package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.domain.model.credential.CloneCredential
import jamgmilk.fuwagit.domain.model.git.CloneOptions
import jamgmilk.fuwagit.domain.model.git.GitPushOptions
import jamgmilk.fuwagit.domain.model.git.GitRemote
import jamgmilk.fuwagit.domain.model.git.PullResult

/**
 * Interface for Git remote operations.
 */
interface GitRemoteDataSource {
    fun cloneRepository(
        uri: String,
        localPath: String,
        credentials: CloneCredential?,
        options: CloneOptions
    ): Result<String>

    fun pull(repoPath: String, credentials: CloneCredential?): Result<PullResult>

    fun push(
        repoPath: String,
        credentials: CloneCredential?,
        options: GitPushOptions
    ): Result<String>

    fun fetch(repoPath: String, credentials: CloneCredential?): Result<String>

    fun configureRemote(repoPath: String, name: String, url: String): Result<String>

    fun deleteRemote(repoPath: String, remoteName: String): Result<String>

    fun getRemotes(repoPath: String): Result<List<GitRemote>>

    fun getRemoteUrl(repoPath: String, name: String): String?
}
