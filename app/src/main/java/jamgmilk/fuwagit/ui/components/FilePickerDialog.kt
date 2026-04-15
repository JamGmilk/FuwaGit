package jamgmilk.fuwagit.ui.components

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import jamgmilk.fuwagit.BuildConfig
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.core.util.PathUtils
import jamgmilk.fuwagit.ui.theme.AppShapes
import jamgmilk.fuwagit.ui.theme.DialogShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private val restrictedPaths: Set<String> by lazy {
    val externalStorage = PathUtils.getExternalStorageDir()
    setOf(
        externalStorage,
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

private val hiddenNames = setOf(
    "Android", "Pictures", "DCIM", "Download", "Downloads",
    "Movies", "Music", "Notifications", "Alarms",
    "Ringtones", "Podcasts", "Documents", "LOST.DIR",
    "obb", "data"
)

private fun isPathRestrictedForSelection(path: String): Boolean {
    val normalizedPath = path.trimEnd(File.separatorChar)
    return restrictedPaths.any { restricted ->
        normalizedPath == restricted.trimEnd(File.separatorChar)
    }
}

private fun shouldHideDirectory(file: File): Boolean {
    val externalStorage = PathUtils.getExternalStorageDir()
    if (file.absolutePath == externalStorage) {
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
    val externalStorage = PathUtils.getExternalStorageDir()
    val safeInitialPath = if (initialPath != null && !isPathRestrictedForSelection(initialPath)) {
        initialPath
    } else {
        externalStorage
    }

    var currentPath by remember { mutableStateOf(safeInitialPath) }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var targetPath by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var createFolderError by remember { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val strCannotAccess = stringResource(R.string.filepicker_cannot_access)
    val strCreateFolder = stringResource(R.string.filepicker_create_folder)
    val strFolderName = stringResource(R.string.filepicker_folder_name)
    val strInvalidFolderName = stringResource(R.string.filepicker_invalid_folder_name)

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

    fun createFolder(name: String) {
        if (name.isBlank()) {
            createFolderError = strInvalidFolderName
            return
        }
        val invalidChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        if (name.any { it in invalidChars }) {
            createFolderError = strInvalidFolderName
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val newDir = File(currentPath, name)
                if (newDir.exists()) {
                    createFolderError = strInvalidFolderName
                    return@launch
                }
                if (newDir.mkdir()) {
                    showCreateFolderDialog = false
                    newFolderName = ""
                    createFolderError = null
                    loadFiles(currentPath)
                } else {
                    createFolderError = strCannotAccess
                }
            } catch (e: Exception) {
                createFolderError = strCannotAccess
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
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
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row {
                        IconButton(onClick = {
                            showCreateFolderDialog = true
                            newFolderName = ""
                            createFolderError = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.filepicker_create_folder),
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { loadFiles(currentPath) }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.action_refresh),
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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
                    val canGoBack = currentPath != (initialPath ?: externalStorage)

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
                                    loadFiles(initialPath ?: externalStorage)
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
                                error = "choose a different path"
                                return@Button
                            }
                            if (BuildConfig.DEBUG) Log.d("FilePickerDialog", "Selected path: $currentPath")
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

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                newFolderName = ""
                createFolderError = null
            },
            title = { Text(strCreateFolder) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = {
                            newFolderName = it
                            createFolderError = null
                        },
                        label = { Text(strFolderName) },
                        singleLine = true,
                        isError = createFolderError != null,
                        supportingText = if (createFolderError != null) {
                            { Text(createFolderError!!, color = MaterialTheme.colorScheme.error) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { createFolder(newFolderName) },
                    enabled = newFolderName.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_create))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateFolderDialog = false
                        newFolderName = ""
                        createFolderError = null
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
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
