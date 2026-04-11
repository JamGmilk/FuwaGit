package jamgmilk.fuwagit.ui.screen.tags

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.git.GitTag
import jamgmilk.fuwagit.ui.components.DangerousOperationType
import jamgmilk.fuwagit.ui.components.OperationResultDialog
import jamgmilk.fuwagit.ui.components.ScreenTemplate
import jamgmilk.fuwagit.ui.theme.AppShapes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TagsScreen(
    tagsViewModel: TagsViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    ScreenTemplate(
        title = stringResource(R.string.screen_tags),
        modifier = modifier,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = { tagsViewModel.loadTags() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.primaryContainer.copy(alpha = 0.3f), CircleShape)
                        .clip(CircleShape)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.tags_refresh_description),
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { tagsViewModel.showCreateDialog() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.primary, CircleShape)
                        .clip(CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.tags_create_tag_description),
                        tint = colors.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, colors.outlineVariant, AppShapes.medium),
            shape = AppShapes.medium,
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            TagsContent(tagsViewModel = tagsViewModel)
        }
    }

    TagsDialogs(tagsViewModel = tagsViewModel)
}

/**
 * 可复用的标签内容组件（不含外层 ScreenTemplate）
 * 可嵌入到其他页面中（如 BranchesScreen）
 */
@Composable
fun TagsContent(
    tagsViewModel: TagsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by tagsViewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    var tagForDetail by remember { mutableStateOf<GitTag?>(null) }

    if (uiState.tags.isEmpty()) {
        EmptyTagsState()
    } else {
        TagsListContent(
            tagsViewModel = tagsViewModel,
            uiState = uiState,
            modifier = modifier.fillMaxSize(),
            onTagDetail = { tagForDetail = it }
        )
    }

    // Tag 详情对话框
    if (tagForDetail != null) {
        TagDetailDialog(
            tag = tagForDetail!!,
            onDismiss = { tagForDetail = null }
        )
    }
}

/**
 * 标签相关对话框集合（创建、删除、推送、操作结果）
 */
@Composable
fun TagsDialogs(
    tagsViewModel: TagsViewModel
) {
    val uiState by tagsViewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var createTagType by remember { mutableStateOf(CreateTagType.Annotated) }

    // 监听 ViewModel 的创建对话框状态
    LaunchedEffect(uiState.showCreateDialog) {
        showCreateDialog = uiState.showCreateDialog
    }

    // 创建标签对话框
    if (showCreateDialog) {
        CreateTagDialog(
            tagType = createTagType,
            onTagTypeChange = { createTagType = it },
            onDismiss = {
                showCreateDialog = false
                tagsViewModel.hideCreateDialog()
            },
            onCreateLightweight = { tagName, commitHash ->
                tagsViewModel.createLightweightTag(tagName, commitHash.ifBlank { null })
                showCreateDialog = false
            },
            onCreateAnnotated = { tagName, message, commitHash ->
                tagsViewModel.createAnnotatedTag(tagName, message, commitHash.ifBlank { null })
                showCreateDialog = false
            }
        )
    }

    // 删除确认对话框
    val selectedTag = uiState.selectedTag
    if (uiState.showDeleteDialog && selectedTag != null) {
        DeleteTagDialog(
            tag = selectedTag,
            onDismiss = { tagsViewModel.hideDeleteDialog() },
            onDelete = {
                tagsViewModel.deleteTag(selectedTag.name)
            }
        )
    }

    // 推送对话框
    if (uiState.showPushDialog) {
        PushTagDialog(
            tag = selectedTag,
            onDismiss = { tagsViewModel.hidePushDialog() },
            onPushSingle = { tagName ->
                tagsViewModel.pushTag(tagName)
            },
            onPushAll = {
                tagsViewModel.pushAllTags()
            }
        )
    }

    // 操作结果反馈对话框
    val operationResult = uiState.operationResult
    if (operationResult != null) {
        val operationType = when (operationResult) {
            is jamgmilk.fuwagit.ui.components.OperationResult.Success -> {
                if (operationResult.message.contains("deleted", ignoreCase = true)) {
                    DangerousOperationType.DELETE_TAG
                } else {
                    DangerousOperationType.PUSH_TAG
                }
            }
            is jamgmilk.fuwagit.ui.components.OperationResult.Failure -> DangerousOperationType.DELETE_TAG
            else -> DangerousOperationType.PUSH_TAG
        }
        OperationResultDialog(
            result = operationResult,
            operationType = operationType,
            onDismiss = { tagsViewModel.clearOperationResult() }
        )
    }
}

@Composable
private fun EmptyTagsState() {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Label,
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                stringResource(R.string.tags_no_tags_found),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                stringResource(R.string.tags_init_repo_message),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TagsListContent(
    tagsViewModel: TagsViewModel,
    uiState: TagsUiState,
    modifier: Modifier = Modifier,
    onTagDetail: (GitTag) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        // 搜索和过滤栏
        SearchFilterBar(
            searchQuery = uiState.searchQuery,
            filterType = uiState.filterType,
            onSearchQueryChange = { tagsViewModel.updateSearchQuery(it) },
            onFilterTypeChange = { tagsViewModel.updateFilterType(it) }
        )

        // 标签列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
        ) {
            if (uiState.filteredTags.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.tags_no_tags_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(uiState.filteredTags, key = { it.name }) { tag ->
                    TagItem(
                        tag = tag,
                        onDelete = { tagsViewModel.showDeleteDialog(tag) },
                        onPush = { tagsViewModel.showPushDialog(tag) },
                        onPushAll = { tagsViewModel.showPushDialog() },
                        onCheckout = { tagsViewModel.checkoutTag(tag.name) },
                        onViewDetail = { onTagDetail(tag) }
                    )
                }
            }

            // 底部推送所有按钮
            if (uiState.filteredTags.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { tagsViewModel.showPushDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.secondaryContainer,
                            contentColor = colors.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.tags_push_all_tags))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterBar(
    searchQuery: String,
    filterType: TagFilterType,
    onSearchQueryChange: (String) -> Unit,
    onFilterTypeChange: (TagFilterType) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showFilters by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.tags_search_hint)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                focusedLabelColor = colors.primary
            )
        )

        Spacer(Modifier.height(8.dp))

        // 过滤选项
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterType == TagFilterType.All,
                onClick = { onFilterTypeChange(TagFilterType.All) },
                label = { Text(stringResource(R.string.tags_filter_all)) }
            )
            FilterChip(
                selected = filterType == TagFilterType.Lightweight,
                onClick = { onFilterTypeChange(TagFilterType.Lightweight) },
                label = { Text(stringResource(R.string.tags_filter_lightweight)) }
            )
            FilterChip(
                selected = filterType == TagFilterType.Annotated,
                onClick = { onFilterTypeChange(TagFilterType.Annotated) },
                label = { Text(stringResource(R.string.tags_filter_annotated)) }
            )
        }
    }
}

@Composable
private fun TagItem(
    tag: GitTag,
    onDelete: () -> Unit,
    onPush: () -> Unit,
    onPushAll: () -> Unit,
    onCheckout: () -> Unit,
    onViewDetail: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    
    // Pre-fetch strings for use in non-composable contexts
    val strTagNameLabel = stringResource(R.string.tags_tag_name_label)
    val strTagNameCopied = stringResource(R.string.tags_tag_name_copied)

    val accentColor = if (tag.isAnnotated) colors.secondary else colors.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    accentColor.copy(alpha = 0.1f)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 标签类型指示器
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            // 标签信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onViewDetail)
            ) {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 类型徽章
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (tag.isAnnotated) stringResource(R.string.tags_annotated_badge) else stringResource(R.string.tags_lightweight_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontSize = 10.sp
                        )
                    }

                    // 目标提交
                    Text(
                        text = stringResource(R.string.tags_target_commit, tag.targetHash.take(7)),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }

                // 附注标签显示消息预览
                if (tag.isAnnotated && tag.message != null) {
                    Text(
                        text = tag.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // 时间戳
                if (tag.timestamp != null) {
                    val dateString = remember(tag.timestamp) {
                        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            .format(Date(tag.timestamp))
                    }
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }

            // 更多操作菜单
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.status_file_actions),
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // 检出标签
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tags_checkout_tag)) },
                    onClick = {
                        onCheckout()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )

                // 推送标签
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tags_push_tag)) },
                    onClick = {
                        onPush()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )

                // 复制标签名称
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_copy)) },
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(strTagNameLabel, tag.name)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, strTagNameCopied, Toast.LENGTH_SHORT).show()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )

                // 删除标签
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    onClick = {
                        onDelete()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.error)
                    }
                )
            }
        }
    }
}

@Composable
private fun CreateTagDialog(
    tagType: CreateTagType,
    onTagTypeChange: (CreateTagType) -> Unit,
    onDismiss: () -> Unit,
    onCreateLightweight: (String, String) -> Unit,
    onCreateAnnotated: (String, String, String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var tagName by remember { mutableStateOf("") }
    var tagMessage by remember { mutableStateOf("") }
    var commitHash by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Label,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = if (tagType == CreateTagType.Annotated) stringResource(R.string.tags_create_annotated_tag) else stringResource(R.string.tags_create_lightweight_tag),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 标签类型选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = tagType == CreateTagType.Lightweight,
                        onClick = { onTagTypeChange(CreateTagType.Lightweight) },
                        label = { Text(stringResource(R.string.tags_filter_lightweight)) }
                    )
                    FilterChip(
                        selected = tagType == CreateTagType.Annotated,
                        onClick = { onTagTypeChange(CreateTagType.Annotated) },
                        label = { Text(stringResource(R.string.tags_filter_annotated)) }
                    )
                }

                // 标签名称
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(stringResource(R.string.tags_tag_name)) },
                    placeholder = { Text(stringResource(R.string.tags_tag_name_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary
                    )
                )

                // 附注标签需要消息
                if (tagType == CreateTagType.Annotated) {
                    OutlinedTextField(
                        value = tagMessage,
                        onValueChange = { tagMessage = it },
                        label = { Text(stringResource(R.string.tags_tag_message)) },
                        placeholder = { Text(stringResource(R.string.tags_tag_message_placeholder)) },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            focusedLabelColor = colors.primary
                        )
                    )
                }

                // 提交哈希（可选）
                OutlinedTextField(
                    value = commitHash,
                    onValueChange = { commitHash = it },
                    label = { Text(stringResource(R.string.tags_commit_hash)) },
                    placeholder = { Text(stringResource(R.string.tags_commit_hash_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        focusedLabelColor = colors.primary
                    )
                )

                Text(
                    text = stringResource(R.string.tags_tag_name_validation_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tagName.isNotBlank()) {
                        if (tagType == CreateTagType.Annotated && tagMessage.isNotBlank()) {
                            onCreateAnnotated(tagName, tagMessage, commitHash)
                        } else if (tagType == CreateTagType.Lightweight) {
                            onCreateLightweight(tagName, commitHash)
                        }
                    }
                },
                enabled = tagName.isNotBlank() && (tagType == CreateTagType.Lightweight || tagMessage.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun DeleteTagDialog(
    tag: GitTag,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.error.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.tags_delete_tag),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.tags_delete_confirmation, tag.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tags_delete_warning),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun PushTagDialog(
    tag: GitTag?,
    onDismiss: () -> Unit,
    onPushSingle: (String) -> Unit,
    onPushAll: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.secondary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    tint = colors.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = if (tag != null) stringResource(R.string.tags_push_tag) else stringResource(R.string.tags_push_all_tags),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (tag != null) {
                Text(
                    text = stringResource(R.string.tags_push_single_tag, tag.name, stringResource(R.string.tags_remote_origin)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.tags_push_all_confirmation, stringResource(R.string.tags_remote_origin)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tag != null) {
                        onPushSingle(tag.name)
                    } else {
                        onPushAll()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (tag != null) stringResource(R.string.tags_push_tag) else stringResource(R.string.tags_push_all_tags))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
