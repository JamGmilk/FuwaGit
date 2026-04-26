package jamgmilk.fuwagit.data.local.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jamgmilk.fuwagit.domain.model.repo.RepoData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
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
    val reposFlow: Flow<List<RepoData>> = _reposFlow.asStateFlow()

    fun getAllReposFlow(): Flow<List<RepoData>> = reposFlow

    private var cachedWrapper: RepoListWrapper = loadFromFile()

    init {
        _reposFlow.value = cachedWrapper.repos
    }

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
        } catch (_: Exception) {
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

    private fun persistAndNotify(repos: List<RepoData>, currentPath: String?): List<RepoData> {
        val wrapper = RepoListWrapper(repos, currentPath)
        saveToFile(wrapper)
        cachedWrapper = wrapper
        _reposFlow.value = repos
        return repos
    }

    fun getAllRepos(): List<RepoData> = cachedWrapper.repos

    fun setCurrentRepo(path: String?) {
        persistAndNotify(cachedWrapper.repos, path)
    }

    fun getCurrentRepoPath(): String? = cachedWrapper.currentRepoPath

    fun addRepo(repo: RepoData): Boolean {
        return try {
            val currentList = cachedWrapper.repos.toMutableList()
            currentList.removeAll { it.path == repo.path }
            currentList.add(repo)
            persistAndNotify(currentList, cachedWrapper.currentRepoPath)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun removeRepo(path: String): Boolean {
        return try {
            val currentList = cachedWrapper.repos.toMutableList()
            currentList.removeAll { it.path == path }
            val newCurrentPath = if (cachedWrapper.currentRepoPath == path) null else cachedWrapper.currentRepoPath
            persistAndNotify(currentList, newCurrentPath)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun updateRepo(path: String, update: (RepoData) -> RepoData) {
        val currentList = cachedWrapper.repos.toMutableList()
        val index = currentList.indexOfFirst { it.path == path }
        if (index >= 0) {
            currentList[index] = update(currentList[index])
            persistAndNotify(currentList, cachedWrapper.currentRepoPath)
        }
    }

    fun toggleFavorite(path: String) {
        val currentList = cachedWrapper.repos.toMutableList()
        val index = currentList.indexOfFirst { it.path == path }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(isFavorite = !currentList[index].isFavorite)
            persistAndNotify(currentList, cachedWrapper.currentRepoPath)
        }
    }

    fun updateLastAccessed(path: String) {
        val currentList = cachedWrapper.repos.toMutableList()
        val index = currentList.indexOfFirst { it.path == path }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(lastAccessedAt = System.currentTimeMillis())
            persistAndNotify(currentList, cachedWrapper.currentRepoPath)
        }
    }

    companion object {
        private const val DATA_FILE = "repo_data.json"
    }
}
