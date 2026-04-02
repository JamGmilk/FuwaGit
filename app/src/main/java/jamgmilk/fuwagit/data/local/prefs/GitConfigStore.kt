package jamgmilk.fuwagit.data.local.prefs

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GitConfig(
    @SerialName("user.name")
    val userName: String = "",
    @SerialName("user.email")
    val userEmail: String = "",
    @SerialName("init.defaultbranch")
    val defaultBranch: String = "main"
)

@Singleton
class GitConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val configFile: File by lazy {
        File(context.filesDir, CONFIG_FILE)
    }

    private val _configFlow = MutableStateFlow(GitConfig())
    val configFlow: StateFlow<GitConfig> = _configFlow.asStateFlow()

    init {
        _configFlow.value = loadFromFile()
    }

    fun getConfig(): GitConfig = _configFlow.value

    suspend fun reloadFromFile() {
        _configFlow.value = loadFromFile()
    }

    suspend fun getUserName(): String = getConfig().userName

    suspend fun getUserEmail(): String = getConfig().userEmail

    suspend fun setUserName(name: String) {
        val current = getConfig()
        val updated = current.copy(userName = name)
        saveToFile(updated)
        _configFlow.value = updated
    }

    suspend fun setUserEmail(email: String) {
        val current = getConfig()
        val updated = current.copy(userEmail = email)
        saveToFile(updated)
        _configFlow.value = updated
    }

    suspend fun setUserConfig(name: String, email: String) {
        val current = getConfig()
        val updated = current.copy(userName = name, userEmail = email)
        saveToFile(updated)
        _configFlow.value = updated
    }

    suspend fun setDefaultBranch(branch: String) {
        val current = getConfig()
        val updated = current.copy(defaultBranch = branch)
        saveToFile(updated)
        _configFlow.value = updated
    }

    private fun loadFromFile(): GitConfig {
        return try {
            if (!configFile.exists()) {
                GitConfig()
            } else {
                val content = configFile.readText()
                if (content.isBlank()) {
                    GitConfig()
                } else {
                    json.decodeFromString<GitConfig>(content)
                }
            }
        } catch (e: Exception) {
            GitConfig()
        }
    }

    private suspend fun saveToFile(config: GitConfig) {
        withContext(Dispatchers.IO) {
            try {
                val jsonStr = json.encodeToString(config)
                configFile.parentFile?.mkdirs()
                configFile.writeText(jsonStr)
            } catch (e: Exception) {
                Log.e("GitConfigStore", "Failed to save config", e)
                throw e
            }
        }
    }

    companion object {
        private const val CONFIG_FILE = "git_config.json"
    }
}