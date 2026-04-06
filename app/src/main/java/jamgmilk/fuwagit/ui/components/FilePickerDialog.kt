package jamgmilk.fuwagit.ui.components

import android.os.Environment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.ui.theme.AppShapes
import jamgmilk.fuwagit.ui.theme.DialogShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private val externalStorageDirPrefix: String = Environment.getExternalStorageDirectory().absolutePath

/**
 * 禁止选择的目录列表，包括根目录和重要系统目录
 * 这些目录不应该被用户选择为仓库路径
 * 注意：这些目录仍然可以在文件浏览器中显示和导航，但不能被选中
 */
private val restrictedPaths: Set<String> by lazy {
    val externalStorage = Environment.getExternalStorageDirectory().absolutePath
    setOf(
        externalStorage, // 根目录
        "$externalStorage/Android",
        "$externalStorage/Pictures",
        "$externalStorage/DCIM",
        "$externalStorage/Download",
        "$externalStorage/Downloads",
        "$externalStorage/Movies",
        "$externalStorage/Music",
        "$externalStorage/Notifications",
        "$externalStorage/Alarms",
        "$externalStorage/Ringtones",
        "$externalStorage/Podcasts",
        "$externalStorage/Documents"
    )
}

/**
 * 检查路径是否被限制选择（用于验证用户是否可以选中该路径）
 */
private fun isPathRestrictedForSelection(path: String): Boolean {
    val normalizedPath = path.trimEnd(File.separatorChar)
    return restrictedPaths.any { restricted ->
        normalizedPath == restricted.trimEnd(File.separatorChar)
    }
}

/**
 * 检查目录是否应该对用户隐藏（用于过滤目录列表）
 */
private fun shouldHideDirectory(file: File): Boolean {
    val externalStorage = Environment.getExternalStorageDirectory().absolutePath
    // 隐藏外部存储根目录下的一些系统目录
    if (file.absolutePath == externalStorage) {
        // 在根目录下，隐藏系统目录
        val hiddenNames = setOf(
            "Android", "Pictures", "DCIM", "Download", "Downloads",
            "Movies", "Music", "Notifications", "Alarms", 
            "Ringtones", "Podcasts", "Documents", "LOST.DIR",
            "obb", "data"
        )
        return file.name in hiddenNames
    }
    return false
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0
)

@Composable
fun FilePickerDialog(
    title: String = "Select Folder",
    initialPath: String? = null,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    // 如果初始路径是受限目录，则使用一个安全的默认路径
    val safeInitialPath = if (initialPath != null && !isPathRestrictedForSelection(initialPath)) {
        initialPath
    } else {
        // 使用外部存储目录作为默认路径（它会显示内容但禁止选择）
        Environment.getExternalStorageDirectory().absolutePath
    }
    
    var currentPath by remember { mutableStateOf(safeInitialPath) }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var targetPath by remember { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val strCannotAccess = stringResource(R.string.filepicker_cannot_access)

    fun loadFiles(path: String) {
        targetPath = path
        isLoading = true
        error = null
        scope.launch(Dispatchers.IO) {
            try {
                val dir = File(path)
                if (dir.exists() && dir.isDirectory && dir.canRead()) {
                    val allItems = dir.listFiles()?.toList() ?: emptyList()
                    val items = allItems.mapNotNull { file ->
                        // 跳过应该隐藏的目录
                        if (file.isDirectory && shouldHideDirectory(file)) {
                            return@mapNotNull null
                        }
                        
                        try {
                            FileItem(
                                name = file.name,
                                path = file.absolutePath,
                                isDirectory = file.isDirectory,
                                size = if (file.isFile) file.length() else 0,
                                lastModified = file.lastModified()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    
                    if (targetPath == path) {
                        files = items
                        isLoading = false
                        currentPath = path
                    }
                } else {
                    if (targetPath == path) {
                        files = emptyList()
                        isLoading = false
                        error = strCannotAccess
                    }
                }
            } catch (e: Exception) {
                if (targetPath == path) {
                    files = emptyList()
                    isLoading = false
                    error = e.message ?: "Unknown error"
                }
            }
        }
    }

    LaunchedEffect(currentPath) {
        loadFiles(currentPath)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .height(600.dp),
            shape = DialogShapes,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp).size(28.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { loadFiles(currentPath) }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.extraSmall)
                        .background(colors.surfaceContainer)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canGoBack = currentPath != (initialPath ?: externalStorageDirPrefix)

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                enabled = canGoBack,
                                onClick = {
                                    File(currentPath).parent?.let { loadFiles(it) }
                                },
                                onLongClick = {
                                    loadFiles(initialPath ?: externalStorageDirPrefix)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.action_back),
                            tint = if (canGoBack) colors.onSurface else colors.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    Text(
                        text = currentPath,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(AppShapes.extraSmall)
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                Modifier.align(Alignment.Center),
                                color = colors.primary
                            )
                        }
                        error != null -> {
                            Text(
                                text = error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                            )
                        }
                        files.isEmpty() -> {
                            Text(
                                text = stringResource(R.string.filepicker_empty_folder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                            )
                        }
                        else -> {
                            LazyColumn {
                                items(files.filter { it.isDirectory }) { file ->
                                    FileListItem(
                                        item = file,
                                        onClick = { loadFiles(file.path) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = AppShapes.extraSmall
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Button(
                        onClick = {
                            if (isPathRestrictedForSelection(currentPath)) {
                                error = "无法选择该目录，请选择其他路径"
                                return@Button
                            }
                            Log.d("FilePickerDialog", "Selected path: $currentPath")
                            onSelect(currentPath)
                        },
                        enabled = !isPathRestrictedForSelection(currentPath),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = AppShapes.extraSmall
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_select))
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListItem(
    item: FileItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
