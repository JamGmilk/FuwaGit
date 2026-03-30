package jamgmilk.fuwagit.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import jamgmilk.fuwagit.domain.model.repo.RepoData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class RepoListWrapper(val repos: List<RepoData>)

@Singleton
class RepoDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _reposFlow = MutableStateFlow<List<RepoData>>(emptyList())

    init {
        _reposFlow.value = getSavedRepos()
    }

    fun getSavedReposFlow(): Flow<List<RepoData>> = _reposFlow.asStateFlow()

    fun getSavedRepos(): List<RepoData> {
        val jsonStr = prefs.getString(KEY_REPO_LIST, null) ?: return emptyList()
        return try {
            json.decodeFromString<RepoListWrapper>(jsonStr).repos
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllRepos(): List<RepoData> = getSavedRepos()

    suspend fun setCurrentRepo(path: String?) {
        if (path != null) {
            prefs.edit().putString(KEY_CURRENT_REPO, path).apply()
        } else {
            prefs.edit().remove(KEY_CURRENT_REPO).apply()
        }
    }

    suspend fun getCurrentRepoPath(): String? {
        return prefs.getString(KEY_CURRENT_REPO, null)
    }

    suspend fun addRepo(repo: RepoData): Boolean {
        val currentList = getSavedRepos().toMutableList()
        currentList.removeAll { it.path == repo.path }
        currentList.add(repo)
        saveRepoList(currentList)
        _reposFlow.value = currentList
        return true
    }

    suspend fun removeRepo(path: String): Boolean {
        val currentList = getSavedRepos().toMutableList()
        currentList.removeAll { it.path == path }
        saveRepoList(currentList)
        _reposFlow.value = currentList
        return true
    }

    suspend fun updateRepo(path: String, update: (RepoData) -> RepoData) {
        val currentList = getSavedRepos().toMutableList()
        val index = currentList.indexOfFirst { it.path == path }
        if (index >= 0) {
            currentList[index] = update(currentList[index])
            saveRepoList(currentList)
            _reposFlow.value = currentList
        }
    }

    suspend fun toggleFavorite(path: String) {
        val currentList = getSavedRepos().toMutableList()
        val index = currentList.indexOfFirst { it.path == path }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(isFavorite = !currentList[index].isFavorite)
            saveRepoList(currentList)
            _reposFlow.value = currentList
        }
    }

    suspend fun updateLastAccessed(path: String) {
        prefs.edit().putLong(KEY_LAST_ACCESSED + path, System.currentTimeMillis()).apply()
    }

    suspend fun getLastAccessed(path: String): Long {
        return prefs.getLong(KEY_LAST_ACCESSED + path, 0L)
    }

    private fun saveRepoList(repos: List<RepoData>) {
        val jsonStr = json.encodeToString(RepoListWrapper(repos))
        prefs.edit().putString(KEY_REPO_LIST, jsonStr).apply()
    }

    companion object {
        private const val PREFS_NAME = "repo_prefs"
        private const val KEY_REPO_LIST = "repo_list"
        private const val KEY_CURRENT_REPO = "current_repo"
        private const val KEY_LAST_ACCESSED = "last_accessed_"
    }
}
