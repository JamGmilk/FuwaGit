package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.data.jgit.GitConfigManager
import jamgmilk.fuwagit.data.local.prefs.GitConfigDataStore
import jamgmilk.fuwagit.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    private val gitConfigManager: GitConfigManager,
    private val gitConfigDataStore: GitConfigDataStore
) : ConfigRepository {

    override suspend fun getGlobalUserName(): String? {
        return gitConfigDataStore.configFlow.first().userName
    }

    override suspend fun getGlobalUserEmail(): String? {
        return gitConfigDataStore.configFlow.first().userEmail
    }

    override suspend fun setGlobalUserConfig(name: String, email: String): Result<Unit> {
        return try {
            gitConfigDataStore.setUserConfig(name, email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun setRepoUserConfig(repoPath: String, name: String, email: String): Result<Unit> {
        return gitConfigManager.setRepoUserConfig(repoPath, name, email)
    }

    override fun removeRepoUserConfig(repoPath: String): Result<Unit> {
        return gitConfigManager.removeRepoUserConfig(repoPath)
    }

    override suspend fun getEffectiveUserConfig(repoPath: String): Pair<String?, String?> {
        val repoName = gitConfigManager.getRepoUserName(repoPath)
        val repoEmail = gitConfigManager.getRepoUserEmail(repoPath)

        if (repoName != null || repoEmail != null) {
            return Pair(repoName, repoEmail)
        }

        return Pair(
            gitConfigDataStore.configFlow.first().userName,
            gitConfigDataStore.configFlow.first().userEmail
        )
    }
}
