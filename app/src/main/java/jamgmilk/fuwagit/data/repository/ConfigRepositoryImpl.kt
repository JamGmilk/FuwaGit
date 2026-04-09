package jamgmilk.fuwagit.data.repository

import jamgmilk.fuwagit.data.jgit.GitConfigManager
import jamgmilk.fuwagit.domain.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    private val gitConfigManager: GitConfigManager
) : ConfigRepository {

    override fun getGlobalUserName(): String? {
        return gitConfigManager.getGlobalUserName()
    }

    override fun getGlobalUserEmail(): String? {
        return gitConfigManager.getGlobalUserEmail()
    }

    override fun setGlobalUserConfig(name: String, email: String): Result<Unit> {
        return gitConfigManager.setGlobalUserConfig(name, email)
    }

    override fun setRepoUserConfig(repoPath: String, name: String, email: String): Result<Unit> {
        return gitConfigManager.setRepoUserConfig(repoPath, name, email)
    }

    override fun removeRepoUserConfig(repoPath: String): Result<Unit> {
        return gitConfigManager.removeRepoUserConfig(repoPath)
    }

    override fun getEffectiveUserConfig(repoPath: String): Pair<String?, String?> {
        return gitConfigManager.getEffectiveUserConfig(repoPath)
    }
}
