package jamgmilk.fuwagit.data.jgit

import android.util.Log
import jamgmilk.fuwagit.BuildConfig
import org.eclipse.jgit.lib.Config
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitConfigManager @Inject constructor() {

    companion object {
        private const val TAG = "GitConfigManager"
    }

    fun setRepoUserName(repoPath: String, name: String): Result<Unit> =
        withRepoConfig(repoPath) { config ->
            config.setString("user", null, "name", name)
            if (BuildConfig.DEBUG) Log.d(TAG, "Set repo user.name: $name for $repoPath")
        }

    fun setRepoUserEmail(repoPath: String, email: String): Result<Unit> =
        withRepoConfig(repoPath) { config ->
            config.setString("user", null, "email", email)
            if (BuildConfig.DEBUG) Log.d(TAG, "Set repo user.email: $email for $repoPath")
        }

    fun setRepoUserConfig(repoPath: String, name: String, email: String): Result<Unit> =
        withRepoConfig(repoPath) { config ->
            config.setString("user", null, "name", name)
            config.setString("user", null, "email", email)
            if (BuildConfig.DEBUG) Log.d(TAG, "Set repo user config: name=$name, email=$email for $repoPath")
        }

    fun getRepoUserName(repoPath: String): String? =
        withRepoConfig<String?>(repoPath) { it.getString("user", null, "name") }
            .getOrNull()

    fun getRepoUserEmail(repoPath: String): String? =
        withRepoConfig<String?>(repoPath) { it.getString("user", null, "email") }
            .getOrNull()

    fun hasRepoUserConfig(repoPath: String): Boolean {
        return withRepoConfig<Boolean>(repoPath) {
            val hasName = it.getString("user", null, "name") != null
            val hasEmail = it.getString("user", null, "email") != null
            hasName || hasEmail
        }.getOrNull() ?: false
    }

    fun removeRepoUserConfig(repoPath: String): Result<Unit> =
        withRepoConfig(repoPath) { config ->
            config.unset("user", null, "name")
            config.unset("user", null, "email")
            if (BuildConfig.DEBUG) Log.d(TAG, "Removed repo user config for $repoPath")
        }

    private fun <T> withRepoConfig(repoPath: String, block: (Config) -> T): Result<T> {
        if (repoPath.isBlank()) {
            return Result.failure(IllegalArgumentException("Repo path cannot be empty"))
        }
        return try {
            val repo = org.eclipse.jgit.api.Git.open(File(repoPath))
            repo.use {
                val result = block(it.repository.config)
                it.repository.config.save()
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to access repo config at $repoPath", e)
            Result.failure(e)
        }
    }
}
