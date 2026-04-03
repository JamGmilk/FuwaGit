package jamgmilk.fuwagit.data.jgit

import jamgmilk.fuwagit.data.local.prefs.GitConfigDataStore
import jamgmilk.fuwagit.domain.model.credential.CloneCredential

/**
 * Interface for core Git data source operations.
 */
interface GitCoreDataSource {
    val gitConfigDataStore: GitConfigDataStore

    fun <T> withGit(repoPath: String, block: (org.eclipse.jgit.api.Git) -> T): Result<T>

    suspend fun initRepo(repoPath: String): Result<String>

    fun hasGitDir(path: String?): Boolean

    fun isValidRepository(repoPath: String): Boolean

    fun getRepoInfo(repoPath: String): Map<String, String>

    fun configureCredentials(
        command: org.eclipse.jgit.api.TransportCommand<*, *>,
        credentials: CloneCredential?
    )

    fun clearSshCredentials()
}
