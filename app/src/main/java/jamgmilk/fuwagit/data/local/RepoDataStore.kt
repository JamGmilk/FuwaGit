package jamgmilk.fuwagit.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jamgmilk.fuwagit.domain.model.repo.RepoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepoDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val DATA_FILE = "repo_data.json"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val dataFile: File by lazy {
        File(context.filesDir, DATA_FILE)
    }

    private val mutex = Mutex()

    suspend fun loadRepoData(): RepoDataList = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (!dataFile.exists()) {
                    RepoDataList()
                } else {
                    val content = dataFile.readText()
                    if (content.isBlank()) {
                        RepoDataList()
                    } else {
                        json.decodeFromString(content)
                    }
                }
            } catch (e: Exception) {
                RepoDataList()
            }
        }
    }

    suspend fun saveRepoData(data: RepoDataList) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val updatedData = data.copy(updatedAt = System.currentTimeMillis())
                dataFile.writeText(json.encodeToString(updatedData))
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    suspend fun addRepo(repo: RepoData): Boolean = withContext(Dispatchers.IO) {
        val data = loadRepoData()
        if (data.repos.any { it.path == repo.path }) {
            false
        } else {
            saveRepoData(data.copy(repos = data.repos + repo))
            true
        }
    }

    suspend fun removeRepo(path: String): Boolean = withContext(Dispatchers.IO) {
        val data = loadRepoData()
        val removed = data.repos.filterNot { it.path == path }
        if (removed.size == data.repos.size) {
            false
        } else {
            saveRepoData(data.copy(repos = removed))
            true
        }
    }

    suspend fun updateRepo(path: String, update: (RepoData) -> RepoData): Boolean = withContext(Dispatchers.IO) {
        val data = loadRepoData()
        val updated = data.repos.map { if (it.path == path) update(it) else it }
        if (updated == data.repos) {
            false
        } else {
            saveRepoData(data.copy(repos = updated))
            true
        }
    }

    suspend fun getRepo(path: String): RepoData? = withContext(Dispatchers.IO) {
        loadRepoData().repos.find { it.path == path }
    }

    suspend fun getAllRepos(): List<RepoData> = withContext(Dispatchers.IO) {
        loadRepoData().repos
    }

    suspend fun updateLastAccessed(path: String) = withContext(Dispatchers.IO) {
        updateRepo(path) { it.copy(lastAccessedAt = System.currentTimeMillis()) }
    }

    suspend fun toggleFavorite(path: String) = withContext(Dispatchers.IO) {
        updateRepo(path) { it.copy(isFavorite = !it.isFavorite) }
    }

    suspend fun setCurrentRepo(path: String?): Boolean = withContext(Dispatchers.IO) {
        val data = loadRepoData()
        if (path != null && data.repos.none { it.path == path }) {
            false
        } else {
            saveRepoData(data.copy(currentRepoPath = path))
            true
        }
    }

    suspend fun getCurrentRepo(): RepoData? = withContext(Dispatchers.IO) {
        val data = loadRepoData()
        data.currentRepoPath?.let { path ->
            data.repos.find { it.path == path }
        }
    }

    suspend fun getCurrentRepoPath(): String? = withContext(Dispatchers.IO) {
        loadRepoData().currentRepoPath
    }
}

@Serializable
data class RepoDataList(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val repos: List<RepoData> = emptyList(),
    val currentRepoPath: String? = null
)
