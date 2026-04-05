package jamgmilk.fuwagit.ui.screen.myrepos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.ui.components.FilePickerDialog
import jamgmilk.fuwagit.ui.theme.AppShapes
import jamgmilk.fuwagit.ui.theme.FuwaGitThemeExtras
import kotlinx.coroutines.launch
import java.io.File

@Composable
internal fun LocalContent(
    myReposViewModel: MyReposViewModel,
    onAddRepository: (path: String, alias: String?) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var path by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var remoteUrl by remember { mutableStateOf("") }
    var isGitRepo by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    var remotes by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedRemoteIndex by remember { mutableStateOf(0) }
    var showNonEmptyWarning by remember { mutableStateOf(false) }

    LaunchedEffect(path) {
        if (path.isBlank()) {
            isGitRepo = false
            showNonEmptyWarning = false
            return@LaunchedEffect
        }

        val file = File(path)
        isGitRepo = File(file, ".git").exists()

        if (isGitRepo) {
            remotes = myReposViewModel.getRemotes(path)
            if (remotes.isNotEmpty()) remoteUrl = remotes[0].second
        } else {
            showNonEmptyWarning = !myReposViewModel.isDirectoryEmpty(path)
        }

        if (alias.isBlank()) {
            alias = path.substringAfterLast(File.separator)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp) // More breathing room
    ) {
        TargetFolderSelector(
            localPath = path,
            suggestedFolderName = if (!isGitRepo) alias else "",
            isDirectoryEmpty = !showNonEmptyWarning,
            onPickFolder = { showFolderPicker = true }
        )

        AnimatedVisibility(
            visible = path.isNotBlank(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                shape = AppShapes.medium,
                color = colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Header
                    Text(
                        text = if (isGitRepo) "Repository Detected" else "New Repository Setup",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isGitRepo) colorScheme.primary else colorScheme.secondary
                    )

                    // Alias Input
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("Friendly alias for FuwaGit") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) }
                    )

                    if (isGitRepo) {
                        // Remote Info
                        if (remotes.isNotEmpty()) {
                            RemoteSelectorDropdown(
                                remotes = remotes,
                                selectedIndex = selectedRemoteIndex,
                                onSelected = { index ->
                                    selectedRemoteIndex = index
                                    remoteUrl = remotes[index].second
                                }
                            )
                        }
                    } else {
                        // Remote URL
                        OutlinedTextField(
                            value = remoteUrl,
                            onValueChange = { remoteUrl = it },
                            label = { Text("Remote URL (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Link, null) }
                        )
                    }
                }
            }
        }

        if (path.isBlank()) {
            InfoGuideCard(message = "Select a local folder to begin managing it with Git.")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch {
                    myReposViewModel.addLocalRepository(path, alias.ifBlank { null }, remoteUrl.ifBlank { null })
                    onAddRepository(path, alias)
                }
            },
            enabled = path.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = AppShapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(12.dp))
            Text(if (isGitRepo) "Add" else "Initialize", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showFolderPicker) {
        FilePickerDialog(
            title = "Select Folder",
            onDismiss = { showFolderPicker = false },
            onSelect = { path = it; showFolderPicker = false }
        )
    }
}

@Composable
private fun InfoGuideCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}


@Composable
private fun RemoteSelectorDropdown(
    remotes: List<Pair<String, String>>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Remote (${remotes.size} found)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            remotes.forEachIndexed { index, (name, url) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(index) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (index == selectedIndex) Icons.Default.CheckCircle else Icons.Default.Link,
                        contentDescription = null,
                        tint = if (index == selectedIndex) FuwaGitThemeExtras.colors.mizuiroAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteUrlDisplay(
    remoteName: String,
    remoteUrl: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = null,
                tint = FuwaGitThemeExtras.colors.mizuiroAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = remoteName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = remoteUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
        color = when {
            path.isBlank() -> colors.surfaceVariant.copy(alpha = 0.5f)
            isGitRepo -> FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.08f)
            else -> FuwaGitThemeExtras.colors.mizuiroAccentLight.copy(alpha = 0.08f)
        }
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
                            isGitRepo -> FuwaGitThemeExtras.colors.mizuiroAccent
                            else -> FuwaGitThemeExtras.colors.mizuiroAccentLight
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
                            isGitRepo -> FuwaGitThemeExtras.colors.mizuiroAccent
                            else -> FuwaGitThemeExtras.colors.mizuiroAccentLight
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
                    .background(FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = "Pick folder",
                    tint = FuwaGitThemeExtras.colors.mizuiroAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun RepositoryInfoCard(
    path: String,
    repoInfo: Map<String, String>
) {
    val colors = MaterialTheme.colorScheme
    val repoName = path.substringAfterLast("/")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FuwaGitThemeExtras.colors.mizuiroAccent.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = FuwaGitThemeExtras.colors.mizuiroAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Repository: $repoName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = FuwaGitThemeExtras.colors.mizuiroAccent
                )
            }

            repoInfo["user.name"]?.let { userName ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "User: $userName",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            repoInfo["HEAD"]?.let { head ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "HEAD: ${head.take(12)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
