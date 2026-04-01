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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.ui.components.FilePickerDialog
import jamgmilk.fuwagit.ui.components.SubSettingsTemplate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

sealed class AddRepoTab {
    data object Clone : AddRepoTab()
    data object Local : AddRepoTab()
}

enum class UrlProtocol {
    NONE, HTTPS, SSH
}

data class UrlValidationResult(
    val isValid: Boolean,
    val protocol: UrlProtocol,
    val errorMessage: String? = null
)

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
                            myReposViewModel = myReposViewModel,
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
            label = "Clone Remote",
            icon = Icons.Default.CloudDownload,
            selected = selectedTab is AddRepoTab.Clone,
            accentColor = Color(0xFF2196F3),
            onClick = { onTabSelected(AddRepoTab.Clone) },
            modifier = Modifier.weight(1f)
        )

        AddRepoTabChip(
            label = "Add Local",
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

private fun validateUrl(url: String): UrlValidationResult {
    if (url.isBlank()) {
        return UrlValidationResult(false, UrlProtocol.NONE)
    }

    return when {
        url.startsWith("https://") || url.startsWith("http://") -> {
            if (url.contains(" ") || !url.contains(".") || url.length < 10) {
                UrlValidationResult(false, UrlProtocol.HTTPS, "Invalid HTTPS URL format")
            } else {
                UrlValidationResult(true, UrlProtocol.HTTPS)
            }
        }
        url.startsWith("git@") -> {
            val gitHostPattern = Regex("^git@[a-zA-Z0-9.-]+:[a-zA-Z0-9._/-]+$")
            if (gitHostPattern.matches(url)) {
                UrlValidationResult(true, UrlProtocol.SSH)
            } else {
                UrlValidationResult(false, UrlProtocol.SSH, "Invalid SSH format (expected: git@host:path)")
            }
        }
        url.startsWith("ssh://") -> {
            UrlValidationResult(true, UrlProtocol.SSH)
        }
        else -> {
            UrlValidationResult(false, UrlProtocol.NONE, "URL must start with https://, http://, git@, or ssh://")
        }
    }
}

private fun extractRepoName(url: String): String {
    return url
        .substringAfterLast("/")
        .substringBefore(".git")
        .substringBefore("?")
        .ifBlank { "repository" }
}

@Composable
private fun CloneContent(
    myReposViewModel: MyReposViewModel,
    onCloneComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cloneUrl by remember { mutableStateOf("") }
    var debouncedUrl by remember { mutableStateOf("") }
    var localPath by remember { mutableStateOf("") }
    var suggestedFolderName by remember { mutableStateOf("") }
    var validationResult by remember { mutableStateOf<UrlValidationResult>(UrlValidationResult(false, UrlProtocol.NONE)) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var httpsCredentials by remember { mutableStateOf<List<HttpsCredential>>(emptyList()) }
    var sshKeys by remember { mutableStateOf<List<SshKey>>(emptyList()) }
    var selectedHttpsUuid by remember { mutableStateOf<String?>(null) }
    var selectedSshUuid by remember { mutableStateOf<String?>(null) }

    var useAnonymousHttps by remember { mutableStateOf(true) }
    var useCredential by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    var isDirectoryEmptyState by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        httpsCredentials = myReposViewModel.getHttpsCredentials()
        sshKeys = myReposViewModel.getSshKeys()
    }

    LaunchedEffect(cloneUrl) {
        val job = Job()
        kotlinx.coroutines.coroutineScope {
            launch {
                delay(500)
                debouncedUrl = cloneUrl
            }
        }
    }

    LaunchedEffect(debouncedUrl) {
        validationResult = validateUrl(debouncedUrl)
        if (validationResult.isValid && debouncedUrl.isNotBlank()) {
            suggestedFolderName = extractRepoName(debouncedUrl)
        } else {
            suggestedFolderName = ""
        }
    }

    LaunchedEffect(localPath) {
        isDirectoryEmptyState = localPath.isBlank() || myReposViewModel.isDirectoryEmpty(localPath)
    }

    val isHttps = validationResult.protocol == UrlProtocol.HTTPS
    val isSsh = validationResult.protocol == UrlProtocol.SSH
    val hasCredentials = (isHttps && httpsCredentials.isNotEmpty()) || (isSsh && sshKeys.isNotEmpty())
    val showCredentialSection = isHttps || isSsh

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
            placeholder = { Text("https://github.com/user/repo.git or git@github.com:user/repo.git") },
            leadingIcon = {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (showCredentialSection) {
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
            isError = validationResult.errorMessage != null,
            supportingText = {
                if (validationResult.errorMessage != null) {
                    Text(
                        text = validationResult.errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isHttps && !useCredential && useAnonymousHttps) {
                    Text(
                        text = "Anonymous download - no credential required",
                        color = Color(0xFF4CAF50)
                    )
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

        if (showCredentialSection && isHttps) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = useAnonymousHttps,
                    onCheckedChange = {
                        useAnonymousHttps = it
                        if (it) useCredential = false
                    },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50))
                )
                Column {
                    Text(
                        text = "Anonymous download",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "For public repositories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!useAnonymousHttps && hasCredentials) {
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
            }
        }

        if (showCredentialSection && isSsh && hasCredentials) {
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
                    text = "Use saved SSH key",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (showCredentialSection && useCredential) {
            if (isHttps && httpsCredentials.isNotEmpty()) {
                val selectedCred = httpsCredentials.find { it.uuid == selectedHttpsUuid }
                CredentialSelectionButton(
                    label = if (selectedCred != null) "Using: ${selectedCred.username}" else "Select HTTPS Credential",
                    isEnabled = true,
                    onClick = {
                        scope.launch {
                            val selected = myReposViewModel.showHttpsCredentialSelector()
                            selected?.let { selectedHttpsUuid = it }
                        }
                    }
                )
            }

            if (isSsh && sshKeys.isNotEmpty()) {
                val selectedKey = sshKeys.find { it.uuid == selectedSshUuid }
                CredentialSelectionButton(
                    label = if (selectedKey != null) "Using: ${selectedKey.name}" else "Select SSH Key",
                    isEnabled = true,
                    onClick = {
                        scope.launch {
                            val selected = myReposViewModel.showSshKeySelector()
                            selected?.let { selectedSshUuid = it }
                        }
                    }
                )
            }
        }

        TargetFolderSelector(
            localPath = localPath,
            suggestedFolderName = suggestedFolderName,
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

        val isCloneButtonEnabled = validationResult.isValid &&
            localPath.isNotBlank() &&
            isDirectoryEmptyState &&
            !isLoading &&
            (!useCredential || (isHttps && selectedHttpsUuid != null) || (isSsh && selectedSshUuid != null) || (isHttps && useAnonymousHttps))

        Button(
            onClick = {
                val repoName = extractRepoName(cloneUrl)
                val targetPath = if (localPath.endsWith("/")) localPath else "$localPath/"
                val fullPath = "${targetPath}$repoName"

                error = null
                isLoading = true

                val httpsUuid = if (isHttps && useCredential && !useAnonymousHttps && selectedHttpsUuid != null) {
                    selectedHttpsUuid
                } else null
                val sshUuid = if (isSsh && useCredential && selectedSshUuid != null) {
                    selectedSshUuid
                } else null

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
                        if (e.message?.contains("401") == true) {
                            error = "Authentication failed. Please provide credentials."
                            useAnonymousHttps = false
                            useCredential = true
                        }
                    }
                }
            },
            enabled = isCloneButtonEnabled,
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
private fun CredentialSelectionButton(
    label: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                Color(0xFF2196F3).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (label.startsWith("Using:")) Icons.Default.CheckCircle else Icons.Default.Key,
                    contentDescription = null,
                    tint = if (isEnabled) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (isEnabled) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LocalContent(
    myReposViewModel: MyReposViewModel,
    onAddRepository: (path: String, alias: String?) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var path by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var remoteUrl by remember { mutableStateOf("") }
    var isGitRepo by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var repoInfo by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var remotes by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var selectedRemoteIndex by remember { mutableStateOf(0) }
    var showNonEmptyWarning by remember { mutableStateOf(false) }

    LaunchedEffect(path) {
        if (path.isNotBlank()) {
            val gitDir = File(path, ".git")
            isGitRepo = gitDir.exists()

            if (isGitRepo) {
                repoInfo = myReposViewModel.getRepoInfo(path)
                remotes = myReposViewModel.getRemotes(path)
                if (remotes.isNotEmpty() && selectedRemoteIndex == 0) {
                    remoteUrl = remotes[0].second
                }
            } else {
                val isEmpty = myReposViewModel.isDirectoryEmpty(path)
                showNonEmptyWarning = !isEmpty
                repoInfo = emptyMap()
                remotes = emptyList()
            }

            if (alias.isBlank() || alias == path.substringAfterLast("/")) {
                alias = path.substringAfterLast("/")
            }
        } else {
            isGitRepo = false
            repoInfo = emptyMap()
            remotes = emptyList()
            showNonEmptyWarning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
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
            if (isGitRepo) {
                RepositoryInfoCard(
                    path = path,
                    repoInfo = repoInfo
                )

                if (remotes.size > 1) {
                    RemoteSelectorDropdown(
                        remotes = remotes,
                        selectedIndex = selectedRemoteIndex,
                        onSelected = { index ->
                            selectedRemoteIndex = index
                            remoteUrl = remotes[index].second
                        }
                    )
                } else if (remotes.isNotEmpty()) {
                    RemoteUrlDisplay(
                        remoteName = remotes[0].first,
                        remoteUrl = remotes[0].second
                    )
                }
            } else {
                if (showNonEmptyWarning) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFF9800).copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Folder is not empty. Git init will be executed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = remoteUrl,
                    onValueChange = { remoteUrl = it },
                    label = { Text("Remote URL (optional)") },
                    placeholder = { Text("https://github.com/user/repo.git") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        focusedLabelColor = Color(0xFF4CAF50),
                        cursorColor = Color(0xFF4CAF50)
                    )
                )
            }
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
                    scope.launch {
                        myReposViewModel.addLocalRepository(
                            path = path,
                            alias = alias.ifBlank { null },
                            remoteUrl = if (remoteUrl.isNotBlank() && !isGitRepo) remoteUrl else null
                        )
                        onAddRepository(path, alias.ifBlank { null })
                    }
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
            Text(
                text = if (isGitRepo) "Add Repository" else "Initialize Git",
                fontSize = 16.sp
            )
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
            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
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
                        tint = if (index == selectedIndex) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant,
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
            containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
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
                tint = Color(0xFF2196F3),
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
private fun TargetFolderSelector(
    localPath: String,
    suggestedFolderName: String,
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
                color = when {
                    localPath.isBlank() -> colors.surfaceVariant.copy(alpha = 0.5f)
                    isDirectoryEmpty -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else -> Color(0xFFE53935).copy(alpha = 0.1f)
                }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when {
                            localPath.isBlank() -> Icons.Default.FolderOpen
                            isDirectoryEmpty -> Icons.Default.CheckCircle
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = when {
                            localPath.isBlank() -> colors.onSurfaceVariant
                            isDirectoryEmpty -> Color(0xFF4CAF50)
                            else -> Color(0xFFE53935)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            localPath.isBlank() -> "Select target folder"
                            isDirectoryEmpty -> if (suggestedFolderName.isNotBlank()) "Will create: $suggestedFolderName" else "Folder is empty"
                            else -> "Folder is not empty"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            localPath.isBlank() -> colors.onSurfaceVariant
                            isDirectoryEmpty -> Color(0xFF4CAF50)
                            else -> Color(0xFFE53935)
                        }
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
            isGitRepo -> Color(0xFF4CAF50).copy(alpha = 0.08f)
            else -> Color(0xFFFF9800).copy(alpha = 0.08f)
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
    repoInfo: Map<String, String>
) {
    val colors = MaterialTheme.colorScheme
    val repoName = path.substringAfterLast("/")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.08f)
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
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Repository: $repoName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
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
