package jamgmilk.obsigit.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KawaiiFileManager(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // The "external storage" for the app
    val baseDir = remember { context.getExternalFilesDir(null) }
    var fileList by remember { mutableStateOf(baseDir?.listFiles()?.toList() ?: emptyList()) }

    // Selection State
    var selectedFiles by remember { mutableStateOf(setOf<File>()) }
    val isSelectionMode by remember { derivedStateOf { selectedFiles.isNotEmpty() } }

    // Dialog States
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun refreshFiles() {
        fileList = baseDir?.listFiles()?.toList()?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // --- 1. Title Area ---
        Text(
            text = "Storage Manager",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = DeepSakura,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // --- 2. File List ---
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 420.dp)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.3f)),
            elevation = CardDefaults.elevatedCardElevation(0.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (fileList.isEmpty()) {
                    item {
                        Text(
                            text = "No files or folders in app storage yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                items(fileList, key = { it.absolutePath }) { file ->
                    val isSelected = selectedFiles.contains(file)
                    FileItemRow(
                        file = file,
                        isSelected = isSelected,
                        onLongClick = { if (!isSelectionMode) selectedFiles = setOf(file) },
                        onClick = {
                            if (isSelectionMode) {
                                selectedFiles = if (isSelected) selectedFiles - file else selectedFiles + file
                            }
                        }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = SakuraPink)

        // --- 3. Action Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "Create", tint = DeepSakura)
                }

                AnimatedVisibility(visible = isSelectionMode) {
                    Row {
                        IconButton(onClick = {
                            selectedFiles = if (selectedFiles.size == fileList.size) emptySet() else fileList.toSet()
                        }) {
                            Icon(if (selectedFiles.size == fileList.size) Icons.Default.RemoveDone else Icons.Default.DoneAll, "Select All", tint = DeepSakura)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF5350))
                        }
                    }
                }
            }

            // Selection Counter
            AnimatedContent(targetState = selectedFiles.size, label = "counter") { count ->
                Text(
                    text = if (count > 0) "$count selected" else "${fileList.size} items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepSakura,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // --- Dialogs ---
    if (showCreateDialog) {
        CreateFileDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, isFolder ->
                val newFile = File(baseDir, name)
                if (isFolder) newFile.mkdir() else newFile.createNewFile()
                refreshFiles()
                showCreateDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete, nya?") },
            text = { Text("Are you sure you want to delete these ${selectedFiles.size} items? Master, they will be gone forever!") },
            confirmButton = {
                TextButton(onClick = {
                    selectedFiles.forEach { it.deleteRecursively() }
                    selectedFiles = emptySet()
                    refreshFiles()
                    showDeleteDialog = false
                }) { Text("Yes, Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    file: File,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) SakuraPink.copy(alpha = 0.6f) else Color.Transparent,
        label = "color"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (file.isDirectory) DeepSakura else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray
        )
    }
}

@Composable
fun CreateFileDialog(onDismiss: () -> Unit, onCreate: (String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var isFolder by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Item") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFolder, onCheckedChange = { isFolder = it })
                    Text("Is this a folder, nya?")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, isFolder) }, enabled = name.isNotBlank()) {
                Text("Create!")
            }
        }
    )
}
