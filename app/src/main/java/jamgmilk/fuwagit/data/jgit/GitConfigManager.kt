package jamgmilk.fuwagit.data.jgit

import android.util.Log
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Git 配置管理器
 * 管理 global 和 repo 级别的 Git 配置
 */
@Singleton
class GitConfigManager @Inject constructor() {

    companion object {
        private const val TAG = "GitConfigManager"
    }

    /**
     * 获取 global git config 文件路径 (~/.gitconfig)
     */
    fun getGlobalConfigFile(): File {
        return try {
            val homeDir = System.getProperty("user.home")
            File(homeDir, ".gitconfig")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get home directory", e)
            // Fallback to app's files directory
            File(".", ".gitconfig")
        }
    }

    /**
     * 读取 global git config
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
     * 设置 global git user.name
     */
    fun setGlobalUserName(name: String): Result<Unit> {
        return try {
            val globalConfig = FileBasedConfig(getGlobalConfigFile(), FS.DETECTED)
            globalConfig.load()
            globalConfig.setString("user", null, "name", name)
            globalConfig.save()
            Log.d(TAG, "Set global user.name: $name")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set global user.name", e)
            Result.failure(e)
        }
    }

    /**
     * 设置 global git user.email
     */
    fun setGlobalUserEmail(email: String): Result<Unit> {
        return try {
            val globalConfig = FileBasedConfig(getGlobalConfigFile(), FS.DETECTED)
            globalConfig.load()
            globalConfig.setString("user", null, "email", email)
            globalConfig.save()
            Log.d(TAG, "Set global user.email: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set global user.email", e)
            Result.failure(e)
        }
    }

    /**
     * 设置 global git 配置（name 和 email）
     */
    fun setGlobalUserConfig(name: String, email: String): Result<Unit> {
        return try {
            val globalConfig = FileBasedConfig(getGlobalConfigFile(), FS.DETECTED)
            globalConfig.load()
            globalConfig.setString("user", null, "name", name)
            globalConfig.setString("user", null, "email", email)
            globalConfig.save()
            Log.d(TAG, "Set global user config: name=$name, email=$email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set global user config", e)
            Result.failure(e)
        }
    }

    /**
     * 获取 global git user.name
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
     * 获取 global git user.email
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
     * 设置仓库级别的 git user.name
     */
    fun setRepoUserName(repoPath: String, name: String): Result<Unit> {
        return try {
            val gitDir = File(repoPath, ".git")
            if (!gitDir.exists()) {
                return Result.failure(Exception("Not a git repository: $repoPath"))
            }
            
            val repoConfig = FileBasedConfig(File(gitDir, "config"), FS.DETECTED)
            repoConfig.load()
            repoConfig.setString("user", null, "name", name)
            repoConfig.save()
            Log.d(TAG, "Set repo user.name: $name for $repoPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set repo user.name", e)
            Result.failure(e)
        }
    }

    /**
     * 设置仓库级别的 git user.email
     */
    fun setRepoUserEmail(repoPath: String, email: String): Result<Unit> {
        return try {
            val gitDir = File(repoPath, ".git")
            if (!gitDir.exists()) {
                return Result.failure(Exception("Not a git repository: $repoPath"))
            }
            
            val repoConfig = FileBasedConfig(File(gitDir, "config"), FS.DETECTED)
            repoConfig.load()
            repoConfig.setString("user", null, "email", email)
            repoConfig.save()
            Log.d(TAG, "Set repo user.email: $email for $repoPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set repo user.email", e)
            Result.failure(e)
        }
    }

    /**
     * 设置仓库级别的 git 配置（name 和 email）
     */
    fun setRepoUserConfig(repoPath: String, name: String, email: String): Result<Unit> {
        return try {
            val gitDir = File(repoPath, ".git")
            if (!gitDir.exists()) {
                return Result.failure(Exception("Not a git repository: $repoPath"))
            }
            
            val repoConfig = FileBasedConfig(File(gitDir, "config"), FS.DETECTED)
            repoConfig.load()
            repoConfig.setString("user", null, "name", name)
            repoConfig.setString("user", null, "email", email)
            repoConfig.save()
            Log.d(TAG, "Set repo user config: name=$name, email=$email for $repoPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set repo user config", e)
            Result.failure(e)
        }
    }

    /**
     * 获取仓库级别的 git user.name
     */
    fun getRepoUserName(repoPath: String): String? {
        return try {
            val gitDir = File(repoPath, ".git")
            if (!gitDir.exists()) return null
            
            val repoConfig = FileBasedConfig(File(gitDir, "config"), FS.DETECTED)
            repoConfig.load()
            repoConfig.getString("user", null, "name")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取仓库级别的 git user.email
     */
    fun getRepoUserEmail(repoPath: String): String? {
        return try {
            val gitDir = File(repoPath, ".git")
            if (!gitDir.exists()) return null
            
            val repoConfig = FileBasedConfig(File(gitDir, "config"), FS.DETECTED)
            repoConfig.load()
            repoConfig.getString("user", null, "email")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查仓库是否有本地用户配置
     */
    fun hasRepoLocalUserConfig(repoPath: String): Boolean {
        return try {
            val gitDir = File(repoPath, ".git")
            if (!gitDir.exists()) return false
            
            val repoConfig = FileBasedConfig(File(gitDir, "config"), FS.DETECTED)
            repoConfig.load()
            repoConfig.getString("user", null, "name") != null || 
            repoConfig.getString("user", null, "email") != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 移除仓库级别的本地用户配置（使用 global 配置）
     */
    fun removeRepoLocalUserConfig(repoPath: String): Result<Unit> {
        return try {
            val gitDir = File(repoPath, ".git")
            if (!gitDir.exists()) {
                return Result.failure(Exception("Not a git repository: $repoPath"))
            }
            
            val repoConfig = FileBasedConfig(File(gitDir, "config"), FS.DETECTED)
            repoConfig.load()
            repoConfig.unset("user", null, "name")
            repoConfig.unset("user", null, "email")
            repoConfig.save()
            Log.d(TAG, "Removed repo local user config for $repoPath")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove repo local user config", e)
            Result.failure(e)
        }
    }

    /**
     * 获取当前生效的用户配置（repo > global）
     */
    fun getEffectiveUserConfig(repoPath: String): Pair<String?, String?> {
        // 先检查 repo config
        val repoName = getRepoUserName(repoPath)
        val repoEmail = getRepoUserEmail(repoPath)
        
        if (repoName != null || repoEmail != null) {
            return Pair(repoName, repoEmail)
        }
        
        // 否则使用 global config
        return Pair(getGlobalUserName(), getGlobalUserEmail())
    }
}
