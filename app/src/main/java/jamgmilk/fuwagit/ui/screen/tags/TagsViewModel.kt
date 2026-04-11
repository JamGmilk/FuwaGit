package jamgmilk.fuwagit.ui.screen.tags

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jamgmilk.fuwagit.domain.model.git.GitTag
import jamgmilk.fuwagit.domain.usecase.git.TagUseCase
import jamgmilk.fuwagit.ui.components.OperationResult
import jamgmilk.fuwagit.ui.state.RepoStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tags 页面 UI 状态
 */
@Stable
data class TagsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val repoPath: String? = null,
    val tags: List<GitTag> = emptyList(),
    val filteredTags: List<GitTag> = emptyList(),
    val searchQuery: String = "",
    val showCreateDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showPushDialog: Boolean = false,
    val selectedTag: GitTag? = null,
    val operationResult: OperationResult? = null,
    val isPushing: Boolean = false,
    val filterType: TagFilterType = TagFilterType.All
)

/**
 * 标签过滤类型
 */
enum class TagFilterType {
    All,
    Lightweight,
    Annotated
}

/**
 * 创建标签的类型
 */
enum class CreateTagType {
    Lightweight,
    Annotated
}

/**
 * Tags ViewModel
 * 管理标签的创建、列出、删除和推送操作
 */
@HiltViewModel
class TagsViewModel @Inject constructor(
    private val currentRepoManager: RepoStateManager,
    private val tagUseCase: TagUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    private var currentRepoPath: String? = null

    init {
        viewModelScope.launch {
            currentRepoManager.repoInfo.collectLatest { info ->
                currentRepoPath = info.repoPath
                _uiState.update { it.copy(repoPath = info.repoPath) }
                if (info.isValidGit) {
                    loadTags()
                } else {
                    _uiState.update {
                        it.copy(tags = emptyList(), filteredTags = emptyList())
                    }
                }
            }
        }
    }

    /**
     * 加载所有标签
     */
    fun loadTags() {
        val path = currentRepoPath
        if (path == null) {
            _uiState.update {
                it.copy(tags = emptyList(), filteredTags = emptyList())
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            tagUseCase.list(path)
                .onSuccess { tags ->
                    _uiState.update {
                        it.copy(
                            tags = tags,
                            filteredTags = applyFilter(tags, it.filterType, it.searchQuery),
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message,
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * 创建轻量标签
     */
    fun createLightweightTag(tagName: String, commitHash: String? = null) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            tagUseCase.createLightweight(path, tagName, commitHash)
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            showCreateDialog = false,
                            operationResult = OperationResult.Success(message)
                        )
                    }
                    loadTags()
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(e.message ?: "Failed to create tag")
                        )
                    }
                }
        }
    }

    /**
     * 创建附注标签
     */
    fun createAnnotatedTag(tagName: String, message: String, commitHash: String? = null) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            tagUseCase.createAnnotated(path, tagName, message, commitHash)
                .onSuccess { resultMessage ->
                    _uiState.update {
                        it.copy(
                            showCreateDialog = false,
                            operationResult = OperationResult.Success(resultMessage)
                        )
                    }
                    loadTags()
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(e.message ?: "Failed to create tag")
                        )
                    }
                }
        }
    }

    /**
     * 删除标签
     */
    fun deleteTag(tagName: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            tagUseCase.delete(path, tagName)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showDeleteDialog = false,
                            selectedTag = null,
                            operationResult = OperationResult.Success("Tag '$tagName' deleted successfully")
                        )
                    }
                    loadTags()
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(e.message ?: "Failed to delete tag")
                        )
                    }
                }
        }
    }

    /**
     * 推送单个标签
     */
    fun pushTag(tagName: String, remoteName: String = "origin") {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isPushing = true, showPushDialog = false) }
            tagUseCase.pushTag(path, tagName, remoteName)
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            isPushing = false,
                            selectedTag = null,
                            operationResult = OperationResult.Success(message)
                        )
                    }
                    loadTags()
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            isPushing = false,
                            operationResult = OperationResult.Failure(e.message ?: "Failed to push tag")
                        )
                    }
                }
        }
    }

    /**
     * 推送所有标签
     */
    fun pushAllTags(remoteName: String = "origin") {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isPushing = true, showPushDialog = false) }
            tagUseCase.pushAllTags(path, remoteName)
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            isPushing = false,
                            operationResult = OperationResult.Success(message)
                        )
                    }
                    loadTags()
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            isPushing = false,
                            operationResult = OperationResult.Failure(e.message ?: "Failed to push tags")
                        )
                    }
                }
        }
    }

    /**
     * 检出标签（进入 detached HEAD 状态）
     */
    fun checkoutTag(tagName: String) {
        val path = currentRepoPath ?: return

        viewModelScope.launch {
            tagUseCase.checkoutTag(path, tagName)
                .onSuccess { message ->
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Success(message)
                        )
                    }
                    loadTags()
                }
                .onError { e ->
                    _uiState.update {
                        it.copy(
                            operationResult = OperationResult.Failure(e.message ?: "Failed to checkout tag")
                        )
                    }
                }
        }
    }

    /**
     * 更新搜索查询
     */
    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredTags = applyFilter(it.tags, it.filterType, query)
            )
        }
    }

    /**
     * 更新过滤类型
     */
    fun updateFilterType(filterType: TagFilterType) {
        _uiState.update {
            it.copy(
                filterType = filterType,
                filteredTags = applyFilter(it.tags, filterType, it.searchQuery)
            )
        }
    }

    /**
     * 显示创建对话框
     */
    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    /**
     * 隐藏创建对话框
     */
    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    /**
     * 显示删除确认对话框
     */
    fun showDeleteDialog(tag: GitTag) {
        _uiState.update {
            it.copy(showDeleteDialog = true, selectedTag = tag)
        }
    }

    /**
     * 隐藏删除对话框
     */
    fun hideDeleteDialog() {
        _uiState.update {
            it.copy(showDeleteDialog = false, selectedTag = null)
        }
    }

    /**
     * 显示推送对话框
     */
    fun showPushDialog(tag: GitTag? = null) {
        _uiState.update {
            it.copy(showPushDialog = true, selectedTag = tag)
        }
    }

    /**
     * 隐藏推送对话框
     */
    fun hidePushDialog() {
        _uiState.update {
            it.copy(showPushDialog = false, selectedTag = null)
        }
    }

    /**
     * 清除操作结果
     */
    fun clearOperationResult() {
        _uiState.update { it.copy(operationResult = null) }
    }

    /**
     * 应用过滤条件
     */
    private fun applyFilter(
        tags: List<GitTag>,
        filterType: TagFilterType,
        searchQuery: String
    ): List<GitTag> {
        var filtered = tags

        // 按类型过滤
        filtered = when (filterType) {
            TagFilterType.All -> filtered
            TagFilterType.Lightweight -> filtered.filter { it.isLightweight }
            TagFilterType.Annotated -> filtered.filter { it.isAnnotated }
        }

        // 按搜索查询过滤
        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter { tag ->
                tag.name.contains(searchQuery, ignoreCase = true)
            }
        }

        return filtered
    }
}
