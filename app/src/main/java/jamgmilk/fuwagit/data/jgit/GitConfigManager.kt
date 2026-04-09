package jamgmilk.fuwagit.data.jgit

import android.util.Log
import jamgmilk.fuwagit.BuildConfig
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Git configuration manager
 * Manages Git config at both global and repository levels
 */
@Singleton
class GitConfigManager @Inject constructor() {

    companion object {
        private const val TAG = "GitConfigManager"
    }

    /**
     * Get global git config file path (~/.gitconfig)
     */
    fun getGlobalConfigFile(): File {
        return try {
            val homeDir = System.getProperty("user.home")
            File(homeDir, ".gitconfig")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get home directory", e)
            File(".", ".gitconfig")
        }
    }

    /**
     * Read global git config
     */
    fun getGlobalConfig(): Config {
        return try {
            val globalConfig = FileBasedConfig(getGlobalConfigFile(), FS.DETECTED)
            globalConfig.load()
            globalConfig
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load global config, using default", e)
            Config()
        }
    }

    /**
     * Set global git user.name
     */
    fun setGlobalUserName(name: String): Result<Unit> {
        return try {
            val globalConfig = FileBasedConfig(getGlobalConfigFile(), FS.DETECTED)
            globalConfig.load()
            globalConfig.setString("user", null, "name", name)
            globalConfig.save()
            if (BuildConfig.DEBUG) Log.d(TAG, "Set global user.name: $name")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set global user.name", e)
            Result.failure(e)
        }
    }

    /**
     * Set global git user.email
     */
    fun setGlobalUserEmail(email: String): Result<Unit> {
        return try {
            val globalConfig = FileBasedConfig(getGlobalConfigFile(), FS.DETECTED)
            globalConfig.load()
            globalConfig.setString("user", null, "email", email)
            globalConfig.save()
            if (BuildConfig.DEBUG) Log.d(TAG, "Set global user.email: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set global user.email", e)
            Result.failure(e)
        }
    }

    /**
     * Set global git config (name and email)
     */
    fun setGlobalUserConfig(name: String, email: String): Result<Unit> {
        return try {
            val globalConfig = FileBasedConfig(getGlobalConfigFile(), FS.DETECTED)
            globalConfig.load()
            globalConfig.setString("user", null, "name", name)
            globalConfig.setString("user", null, "email", email)
            globalConfig.save()
            if (BuildConfig.DEBUG) Log.d(TAG, "Set global user config: name=$name, email=$email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set global user config", e)
            Result.failure(e)
        }
    }

    /**
     * Get global git user.name
     */
    fun getGlobalUserName(): String? {
        return try {
            val config = getGlobalConfig()
            config.getString("user", null, "name")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get global git user.email
     */
    fun getGlobalUserEmail(): String? {
        return try {
            val config = getGlobalConfig()
            config.getString("user", null, "email")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Set repository-level git user.name
     */
    fun setRepoUserName(repoPath: String, name: String): Result<Unit> {
        return try {
            val repo = org.eclipse.jgit.api.Git.open(File(repoPath))
            val config = repo.repository.config
            config.setString("user", null, "name", name)
            config.save()
            repo.close()
            if (BuildConfig.DEBUG) Log.d(TAG, "Set repo user.name: $name for $repoPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set repo user.name", e)
            Result.failure(e)
        }
    }

    /**
     * Set repository-level git user.email
     */
    fun setRepoUserEmail(repoPath: String, email: String): Result<Unit> {
        return try {
            val repo = org.eclipse.jgit.api.Git.open(File(repoPath))
            val config = repo.repository.config
            config.setString("user", null, "email", email)
            config.save()
            repo.close()
            if (BuildConfig.DEBUG) Log.d(TAG, "Set repo user.email: $email for $repoPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set repo user.email", e)
            Result.failure(e)
        }
    }

    /**
     * Set repository-level git config (name and email)
     */
    fun setRepoUserConfig(repoPath: String, name: String, email: String): Result<Unit> {
        return try {
            val repo = org.eclipse.jgit.api.Git.open(File(repoPath))
            val config = repo.repository.config
            config.setString("user", null, "name", name)
            config.setString("user", null, "email", email)
            config.save()
            repo.close()
            if (BuildConfig.DEBUG) Log.d(TAG, "Set repo user config: name=$name, email=$email for $repoPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set repo user config", e)
            Result.failure(e)
        }
    }

    /**
     * Get repository-level git user.name
     */
    fun getRepoUserName(repoPath: String): String? {
        return try {
            val repo = org.eclipse.jgit.api.Git.open(File(repoPath))
            val name = repo.repository.config.getString("user", null, "name")
            repo.close()
            name
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get repository-level git user.email
     */
    fun getRepoUserEmail(repoPath: String): String? {
        return try {
            val repo = org.eclipse.jgit.api.Git.open(File(repoPath))
            val email = repo.repository.config.getString("user", null, "email")
            repo.close()
            email
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if repository has local user config
     */
    fun hasRepoUserConfig(repoPath: String): Boolean {
        return try {
            val repo = org.eclipse.jgit.api.Git.open(File(repoPath))
            val config = repo.repository.config
            val hasName = config.getString("user", null, "name") != null
            val hasEmail = config.getString("user", null, "email") != null
            repo.close()
            hasName || hasEmail
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Remove repository-level local user config (use global config instead)
     */
    fun removeRepoUserConfig(repoPath: String): Result<Unit> {
        return try {
            val repo = org.eclipse.jgit.api.Git.open(File(repoPath))
            val config = repo.repository.config
            config.unset("user", null, "name")
            config.unset("user", null, "email")
            config.save()
            repo.close()
            if (BuildConfig.DEBUG) Log.d(TAG, "Removed repo user config for $repoPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove repo user config", e)
            Result.failure(e)
        }
    }

    /**
     * Get currently effective user config (repo > global)
     */
    fun getEffectiveUserConfig(repoPath: String): Pair<String?, String?> {
        // First check repo config
        val repoName = getRepoUserName(repoPath)
        val repoEmail = getRepoUserEmail(repoPath)

        if (repoName != null || repoEmail != null) {
            return Pair(repoName, repoEmail)
        }

        // Otherwise use global config
        return Pair(getGlobalUserName(), getGlobalUserEmail())
    }
}
