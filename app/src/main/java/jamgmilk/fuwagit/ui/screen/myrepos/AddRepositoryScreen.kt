package jamgmilk.fuwagit.ui.screen.myrepos

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import jamgmilk.fuwagit.ui.components.CredentialDropdown
import jamgmilk.fuwagit.ui.components.FilePickerDialog
import jamgmilk.fuwagit.ui.components.SubSettingsTemplate
import java.io.File

sealed class AddRepoTab {
    data object Clone : AddRepoTab()
    data object Local : AddRepoTab()
}

@Composable
fun AddRepositoryScreen(
    onBack: () -> Unit,
    onCloneComplete: (String) -> Unit,
    onAddRepository: (path: String, alias: String?) -> Unit,
    modifier: Modifier = Modifier,
    myReposViewModel: MyReposViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf<AddRepoTab>(AddRepoTab.Clone) }

    SubSettingsTemplate(
        title = "Add Repository",
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AddRepoTabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "add_repo_tab_transition"
            ) { tab ->
                when (tab) {
                    is AddRepoTab.Clone -> {
                        CloneContent(
                            myReposViewModel = myReposViewModel,
                            onCloneComplete = onCloneComplete
                        )
                    }
                    is AddRepoTab.Local -> {
                        LocalContent(
                            onAddRepository = { path, alias ->
                                onAddRepository(path, alias)
                                Toast.makeText(context, "Repository added", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRepoTabSelector(
    selectedTab: AddRepoTab,
    onTabSelected: (AddRepoTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AddRepoTabChip(
            label = "Clone",
            icon = Icons.Default.CloudDownload,
            selected = selectedTab is AddRepoTab.Clone,
            accentColor = Color(0xFF2196F3),
            onClick = { onTabSelected(AddRepoTab.Clone) },
            modifier = Modifier.weight(1f)
        )

        AddRepoTabChip(
            label = "Local",
            icon = Icons.Default.Folder,
            selected = selectedTab is AddRepoTab.Local,
            accentColor = Color(0xFF4CAF50),
            onClick = { onTabSelected(AddRepoTab.Local) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AddRepoTabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accentColor.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, accentColor)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentColor else colors.onSurface
            )
        }
    }
}

@Composable
private fun CloneContent(
    myReposViewModel: MyReposViewModel,
    onCloneComplete: (String) -> Unit
) {
    val context = LocalContext.current

    var cloneUrl by remember { mutableStateOf("") }
    var localPath by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val httpsCredentials = remember { mutableStateListOf<HttpsCredentialItem>() }
    val sshKeys = remember { mutableStateListOf<SshKeyItem>() }
    var selectedHttpsUuid by remember { mutableStateOf<String?>(null) }
    var selectedSshUuid by remember { mutableStateOf<String?>(null) }
    var useCredential by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    var isDirectoryEmptyState by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        httpsCredentials.clear()
        sshKeys.clear()
        httpsCredentials.addAll(myReposViewModel.getHttpsCredentials())
        sshKeys.addAll(myReposViewModel.getSshKeys())
    }

    LaunchedEffect(localPath) {
        isDirectoryEmptyState = localPath.isBlank() || myReposViewModel.isDirectoryEmpty(localPath)
    }

    val isHttps = cloneUrl.startsWith("http://") || cloneUrl.startsWith("https://")
    val isSsh = cloneUrl.startsWith("git@") || cloneUrl.startsWith("ssh://")
    val showCredentials = useCredential && ((isHttps && httpsCredentials.isNotEmpty()) || (isSsh && sshKeys.isNotEmpty()))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally)
                .background(
                    color = Color(0xFF2196F3).copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Enter the repository URL and select a target folder to clone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cloneUrl,
            onValueChange = { cloneUrl = it },
            label = { Text("Repository URL") },
            leadingIcon = {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (isHttps || isSsh) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isHttps) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFF2196F3).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isHttps) "HTTPS" else "SSH",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isHttps) Color(0xFF4CAF50) else Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                focusedLabelColor = Color(0xFF2196F3),
                cursorColor = Color(0xFF2196F3)
            )
        )

        if ((isHttps && httpsCredentials.isNotEmpty()) || (isSsh && sshKeys.isNotEmpty())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = useCredential,
                    onCheckedChange = { useCredential = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2196F3))
                )
                Text(
                    text = "Use saved credentials",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (showCredentials) {
                if (isHttps && httpsCredentials.isNotEmpty()) {
                    CredentialDropdown(
                        label = "HTTPS Credential",
                        items = httpsCredentials.map { it.uuid to it.displayName },
                        selectedUuid = selectedHttpsUuid,
                        onSelected = { selectedHttpsUuid = it },
                        accentColor = Color(0xFF4CAF50)
                    )
                }

                if (isSsh && sshKeys.isNotEmpty()) {
                    CredentialDropdown(
                        label = "SSH Key",
                        items = sshKeys.map { it.uuid to it.displayName },
                        selectedUuid = selectedSshUuid,
                        onSelected = { selectedSshUuid = it },
                        accentColor = Color(0xFF2196F3)
                    )
                }
            }
        }

        TargetFolderSelector(
            localPath = localPath,
            isDirectoryEmpty = localPath.isBlank() || isDirectoryEmptyState,
            onPickFolder = { showFolderPicker = true }
        )

        if (error != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE53935).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE53935)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                val repoName = cloneUrl.substringAfterLast("/").substringBefore(".git").ifBlank { "repo" }
                val targetPath = if (localPath.endsWith("/")) localPath else "$localPath/"
                val fullPath = "${targetPath}$repoName"

                error = null
                isLoading = true

                val httpsUuid = if (isHttps && useCredential && selectedHttpsUuid != null) selectedHttpsUuid else null
                val sshUuid = if (!isHttps && useCredential && selectedSshUuid != null) selectedSshUuid else null

                myReposViewModel.cloneWithCredentials(
                    uri = cloneUrl,
                    localPath = fullPath,
                    branch = null,
                    httpsCredentialUuid = httpsUuid,
                    sshKeyUuid = sshUuid
                ) { result ->
                    isLoading = false
                    result.onSuccess {
                        Toast.makeText(context, "Repository cloned successfully", Toast.LENGTH_SHORT).show()
                        onCloneComplete(fullPath)
                    }.onFailure { e ->
                        error = e.message
                    }
                }
            },
            enabled = cloneUrl.isNotBlank() && localPath.isNotBlank() && isDirectoryEmptyState && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Cloning...", fontSize = 16.sp)
            } else {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Clone Repository", fontSize = 16.sp)
            }
        }
    }

    if (showFolderPicker) {
        FilePickerDialog(
            title = "Select Clone Destination",
            onDismiss = { showFolderPicker = false },
            onSelect = { path ->
                localPath = path
                showFolderPicker = false
            }
        )
    }
}

@Composable
private fun LocalContent(
    onAddRepository: (path: String, alias: String?) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    var path by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var isGitRepo by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    LaunchedEffect(path) {
        if (path.isNotBlank()) {
            isGitRepo = File(path, ".git").exists()
            if (alias.isBlank() || alias == path.substringAfterLast("/")) {
                alias = path.substringAfterLast("/")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.CenterHorizontally)
                .background(
                    color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Select a local Git repository folder to add to your repository list.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        FolderSelectorCard(
            path = path,
            isGitRepo = isGitRepo,
            onPickFolder = { showFolderPicker = true }
        )

        if (path.isNotBlank()) {
            RepositoryInfoCard(
                path = path,
                isGitRepo = isGitRepo
            )
        }

        OutlinedTextField(
            value = alias,
            onValueChange = { alias = it },
            label = { Text("Alias (optional)") },
            placeholder = { Text("Enter a friendly name for this repository") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CAF50),
                focusedLabelColor = Color(0xFF4CAF50),
                cursorColor = Color(0xFF4CAF50)
            )
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                if (path.isNotBlank()) {
                    onAddRepository(path, alias.ifBlank { null })
                }
            },
            enabled = path.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Add Repository", fontSize = 16.sp)
        }

        if (path.isBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Tap the folder icon above to select a repository folder",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showFolderPicker) {
        FilePickerDialog(
            title = "Select Repository Folder",
            onDismiss = { showFolderPicker = false },
            onSelect = { selectedPath ->
                path = selectedPath
                showFolderPicker = false
            }
        )
    }
}

@Composable
private fun TargetFolderSelector(
    localPath: String,
    isDirectoryEmpty: Boolean,
    onPickFolder: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Target Folder",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (localPath.isBlank()) colors.surfaceVariant.copy(alpha = 0.5f)
                else if (isDirectoryEmpty) Color(0xFF4CAF50).copy(alpha = 0.1f)
                else Color(0xFFE53935).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (localPath.isBlank()) Icons.Default.FolderOpen
                        else if (isDirectoryEmpty) Icons.Default.CheckCircle
                        else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (localPath.isBlank()) colors.onSurfaceVariant
                        else if (isDirectoryEmpty) Color(0xFF4CAF50)
                        else Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (localPath.isBlank()) "Select target folder"
                        else if (isDirectoryEmpty) "Folder is empty"
                        else "Folder is not empty",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (localPath.isBlank()) colors.onSurfaceVariant
                        else if (isDirectoryEmpty) Color(0xFF4CAF50)
                        else Color(0xFFE53935)
                    )
                }
            }

            IconButton(
                onClick = onPickFolder,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2196F3).copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = "Pick folder",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (localPath.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = localPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Repository will be cloned to the selected folder with its name",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FolderSelectorCard(
    path: String,
    isGitRepo: Boolean,
    onPickFolder: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (path.isBlank()) colors.surfaceVariant.copy(alpha = 0.5f)
        else if (isGitRepo) Color(0xFF4CAF50).copy(alpha = 0.08f)
        else Color(0xFFFF9800).copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when {
                            path.isBlank() -> Icons.Default.FolderOpen
                            isGitRepo -> Icons.Default.CheckCircle
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when {
                            path.isBlank() -> colors.onSurfaceVariant
                            isGitRepo -> Color(0xFF4CAF50)
                            else -> Color(0xFFFF9800)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            path.isBlank() -> "No folder selected"
                            isGitRepo -> "Git repository detected"
                            else -> "Not a Git repository"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            path.isBlank() -> colors.onSurfaceVariant
                            isGitRepo -> Color(0xFF4CAF50)
                            else -> Color(0xFFFF9800)
                        }
                    )
                }

                if (path.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                        color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onPickFolder,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = "Pick folder",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun RepositoryInfoCard(
    path: String,
    isGitRepo: Boolean
) {
    val colors = MaterialTheme.colorScheme

    if (!isGitRepo) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFF9800).copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "This folder is not a Git repository. You can still add it, but Git operations may not work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800)
                )
            }
        }
    } else {
        val repoName = path.substringAfterLast("/")
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF4CAF50).copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Repository: $repoName",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
