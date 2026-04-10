package jamgmilk.fuwagit.data.local.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jamgmilk.fuwagit.domain.model.repo.RepoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class RepoListWrapper(
    val repos: List<RepoData> = emptyList(),
    val currentRepoPath: String? = null
)

@Singleton
class RepoDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val dataFile: File by lazy {
        File(context.filesDir, DATA_FILE)
    }

    private val _reposFlow = MutableStateFlow<List<RepoData>>(emptyList())

    init {
        _reposFlow.value = getSavedRepos()
    }

    fun getSavedReposFlow(): Flow<List<RepoData>> = _reposFlow.asStateFlow()

    fun getSavedRepos(): List<RepoData> {
        return loadFromFile().repos
    }

    suspend fun getAllRepos(): List<RepoData> = getSavedRepos()

    private fun loadFromFile(): RepoListWrapper {
        return try {
            if (!dataFile.exists()) {
                RepoListWrapper()
            } else {
                val content = dataFile.readText()
                if (content.isBlank()) {
                    RepoListWrapper()
                } else {
                    json.decodeFromString<RepoListWrapper>(content)
                }
            }
        } catch (e: Exception) {
            RepoListWrapper()
        }
    }

    private fun saveToFile(wrapper: RepoListWrapper) {
        val tempFile = File(context.filesDir, "$DATA_FILE.tmp")
        try {
            val jsonStr = json.encodeToString(wrapper)
            tempFile.writeText(jsonStr)
            if (!tempFile.renameTo(dataFile)) {
                tempFile.copyTo(dataFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    private suspend fun updateAndSave(repos: List<RepoData>, currentPath: String?): List<RepoData> {
        saveToFile(RepoListWrapper(repos, currentPath))
        _reposFlow.value = repos
        return repos
    }

    suspend fun setCurrentRepo(path: String?) {
        val wrapper = loadFromFile()
        updateAndSave(wrapper.repos, path)
    }

    suspend fun getCurrentRepoPath(): String? {
        return loadFromFile().currentRepoPath
    }

    suspend fun addRepo(repo: RepoData): Boolean {
        val wrapper = loadFromFile()
        val currentList = wrapper.repos.toMutableList()
        currentList.removeAll { it.path == repo.path }
        currentList.add(repo)
        updateAndSave(currentList, wrapper.currentRepoPath)
        return true
    }

    suspend fun removeRepo(path: String): Boolean {
        val wrapper = loadFromFile()
        val currentList = wrapper.repos.toMutableList()
        currentList.removeAll { it.path == path }
        val newCurrentPath = if (wrapper.currentRepoPath == path) null else wrapper.currentRepoPath
        updateAndSave(currentList, newCurrentPath)
        return true
    }

    suspend fun updateRepo(path: String, update: (RepoData) -> RepoData) {
        val wrapper = loadFromFile()
        val currentList = wrapper.repos.toMutableList()
        val index = currentList.indexOfFirst { it.path == path }
        if (index >= 0) {
            currentList[index] = update(currentList[index])
            updateAndSave(currentList, wrapper.currentRepoPath)
        }
    }

    suspend fun toggleFavorite(path: String) {
        val wrapper = loadFromFile()
        val currentList = wrapper.repos.toMutableList()
        val index = currentList.indexOfFirst { it.path == path }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(isFavorite = !currentList[index].isFavorite)
            updateAndSave(currentList, wrapper.currentRepoPath)
        }
    }

    suspend fun updateLastAccessed(path: String) {
        val wrapper = loadFromFile()
        val currentList = wrapper.repos.toMutableList()
        val index = currentList.indexOfFirst { it.path == path }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(lastAccessedAt = System.currentTimeMillis())
            updateAndSave(currentList, wrapper.currentRepoPath)
        }
    }

    suspend fun getLastAccessed(path: String): Long {
        return loadFromFile().repos.find { it.path == path }?.lastAccessedAt ?: 0L
    }

    companion object {
        private const val DATA_FILE = "repo_data.json"
    }
}
