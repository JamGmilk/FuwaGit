package jamgmilk.fuwagit.ui.screen.myrepos

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.model.git.CloneOptions
import jamgmilk.fuwagit.ui.components.FilePickerDialog
import jamgmilk.fuwagit.ui.screen.credentials.CredentialSelectDialog
import jamgmilk.fuwagit.ui.screen.credentials.CredentialType
import jamgmilk.fuwagit.ui.theme.AppShapes
import kotlinx.coroutines.delay

@Composable
internal fun CloneContent(
    myReposViewModel: MyReposViewModel,
    onCloneComplete: (String) -> Unit
) {
    val context = LocalContext.current
    // val scope = rememberCoroutineScope()
    // val colors = MaterialTheme.colorScheme

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

    var showFolderPicker by remember { mutableStateOf(false) }
    var showCredentialDialog by remember { mutableStateOf(false) }

    var isDirectoryEmptyState by remember { mutableStateOf(true) }

    var cloneAllBranches by remember { mutableStateOf(true) }
    var enableShallowClone by remember { mutableStateOf(false) }
    var shallowDepth by remember { mutableStateOf("50") }

    val colors = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        httpsCredentials = myReposViewModel.getHttpsCredentials()
        sshKeys = myReposViewModel.getSshKeys()
    }

    LaunchedEffect(cloneUrl) {
        delay(500)
        debouncedUrl = cloneUrl
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
        Column(modifier = Modifier.fillMaxWidth()) {
            // Section Header
            Text(
                text = stringResource(R.string.clone_remote_url_header),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            // TODO: 清除焦点
            OutlinedTextField(
                value = cloneUrl,
                onValueChange = { cloneUrl = it },
                label = { Text(stringResource(R.string.clone_repository_url_label)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                // TODO: Fix overlap
                trailingIcon = {
                    if (showCredentialSection) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.secondaryContainer
                        ) {
                            Text(
                                text = if (isHttps) stringResource(R.string.clone_protocol_https) else stringResource(R.string.clone_protocol_ssh),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSecondaryContainer,
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
                            // TODO: why !!
                            text = validationResult.errorMessage!!,
                            color = colors.error
                        )
                    }
                },
                singleLine = true,
                shape = AppShapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isHttps && httpsCredentials.isNotEmpty()) {
            val selectedCred = httpsCredentials.find { it.uuid == selectedHttpsUuid }
            CredentialSelector(
                label = if (selectedCred != null) stringResource(R.string.clone_credential_selected_format, selectedCred.username) else stringResource(R.string.clone_credential_selector_https),
                isEnabled = true,
                onClick = { showCredentialDialog = true }
            )
        }

        if (isSsh && sshKeys.isNotEmpty()) {
            val selectedKey = sshKeys.find { it.uuid == selectedSshUuid }
            CredentialSelector(
                label = if (selectedKey != null) stringResource(R.string.clone_credential_selected_ssh, selectedKey.name) else stringResource(R.string.clone_credential_selector_ssh),
                isEnabled = true,
                onClick = { showCredentialDialog = true }
            )
        }

        TargetFolderSelector(
            localPath = localPath,
            suggestedFolderName = suggestedFolderName,
            isDirectoryEmpty = localPath.isBlank() || isDirectoryEmptyState,
            onPickFolder = { showFolderPicker = true }
        )

        CloneOptionsSection(
            cloneAllBranches = cloneAllBranches,
            onCloneAllBranchesChange = { cloneAllBranches = it },
            enableShallowClone = enableShallowClone,
            onEnableShallowCloneChange = {
                enableShallowClone = it
                if (!it) shallowDepth = "50"
            },
            shallowDepth = shallowDepth,
            onShallowDepthChange = { shallowDepth = it }
        )

        if (error != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.error.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        val isCloneButtonEnabled = validationResult.isValid &&
                localPath.isNotBlank() &&
                isDirectoryEmptyState &&
                !isLoading &&
                (!isSsh || selectedSshUuid != null) &&
                (!isHttps || httpsCredentials.isEmpty() || selectedHttpsUuid != null)

        Button(
            onClick = {
                val repoName = extractRepoName(cloneUrl)
                val targetPath = if (localPath.endsWith("/")) localPath else "$localPath/"
                val fullPath = "${targetPath}$repoName"

                error = null
                isLoading = true

                val httpsUuid = if (isHttps) selectedHttpsUuid else null
                val sshUuid = if (isSsh) selectedSshUuid else null

                val options = CloneOptions(
                    branch = null,
                    cloneAllBranches = cloneAllBranches,
                    depth = if (enableShallowClone) shallowDepth.toIntOrNull()?.takeIf { it > 0 } else null
                )

                myReposViewModel.cloneWithCredentials(
                    uri = cloneUrl,
                    localPath = fullPath,
                    branch = null,
                    httpsCredentialUuid = httpsUuid,
                    sshKeyUuid = sshUuid,
                    cloneOptions = options
                ) { result ->
                    isLoading = false
                    result.onSuccess {
                        Toast.makeText(context, context.getString(R.string.clone_clone_success), Toast.LENGTH_SHORT).show()
                        onCloneComplete(fullPath)
                    }.onError { e ->
                        error = e.message
                        if (e.message?.contains("401") == true) {
                            error = context.getString(R.string.clone_auth_failed)
                        }
                    }
                }
            },
            enabled = isCloneButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.clone_cloning), fontSize = 16.sp)
            } else {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.clone_repository_button), fontSize = 16.sp)
            }
        }
    }

    if (showFolderPicker) {
        FilePickerDialog(
            title = stringResource(R.string.clone_select_destination),
            onDismiss = { showFolderPicker = false },
            onSelect = { path ->
                localPath = path
                showFolderPicker = false
            }
        )
    }

    if (showCredentialDialog) {
        CredentialSelectDialog(
            title = stringResource(R.string.clone_select_credential),
            httpsCredentials = httpsCredentials,
            sshKeys = sshKeys,
            onDismiss = { showCredentialDialog = false },
            onSelect = { uuid, type ->
                when (type) {
                    CredentialType.HTTPS -> selectedHttpsUuid = uuid
                    CredentialType.SSH -> selectedSshUuid = uuid
                    CredentialType.BOTH -> {}
                }
                showCredentialDialog = false
            }
        )
    }
}

@Composable
private fun CredentialSelector(
    label: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        enabled = isEnabled,
        shape = AppShapes.extraSmall,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        ),
        border = CardDefaults.outlinedCardBorder(enabled = isEnabled)
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
                    imageVector = if (label.startsWith("Using:")) Icons.Default.CheckCircle else Icons.Default.Key,
                    contentDescription = null,
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
internal fun TargetFolderSelector(
    localPath: String,
    suggestedFolderName: String,
    isDirectoryEmpty: Boolean,
    onPickFolder: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val isSelected = localPath.isNotBlank()
    val statusColor = when {
        !isSelected -> colorScheme.outline
        isDirectoryEmpty -> colorScheme.primary
        else -> colorScheme.error
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.clone_destination_label),
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            onClick = onPickFolder,
            shape = AppShapes.medium,
            color = colorScheme.surfaceContainerHigh,
            border = if (!isDirectoryEmpty && isSelected)
                BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.5f))
            else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(statusColor.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Folder else Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSelected) stringResource(R.string.clone_target_path) else stringResource(R.string.clone_no_folder_selected),
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                        if (isSelected) {
                            Text(
                                text = localPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = statusColor.copy(alpha = 0.12f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isDirectoryEmpty) Icons.Default.CheckCircle else Icons.Default.ReportProblem,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isDirectoryEmpty) {
                                    if (suggestedFolderName.isNotBlank()) stringResource(R.string.clone_creating_format, suggestedFolderName) else stringResource(R.string.clone_ready_to_clone)
                                } else stringResource(R.string.clone_directory_not_empty),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloneOptionsSection(
    cloneAllBranches: Boolean,
    onCloneAllBranchesChange: (Boolean) -> Unit,
    enableShallowClone: Boolean,
    onEnableShallowCloneChange: (Boolean) -> Unit,
    shallowDepth: String,
    onShallowDepthChange: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header
        Text(
            text = stringResource(R.string.clone_advanced_options),
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            //.padding(16.dp)
        ) {
            // Clone All Branches
            CloneOptionToggleRow(
                title = stringResource(R.string.clone_all_branches),
                description = stringResource(R.string.clone_all_branches_desc),
                checked = cloneAllBranches,
                onCheckedChange = onCloneAllBranchesChange,
                icon = Icons.Default.AccountTree
            )

            HorizontalDivider(
                // modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Shallow Clone
            CloneOptionToggleRow(
                title = stringResource(R.string.clone_shallow_clone),
                description = stringResource(R.string.clone_shallow_clone_desc),
                checked = enableShallowClone,
                onCheckedChange = onEnableShallowCloneChange,
                icon = Icons.Default.Speed
            )

            // Depth Input
            AnimatedVisibility(
                visible = enableShallowClone,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = shallowDepth,
                        onValueChange = { if (it.all { char -> char.isDigit() }) onShallowDepthChange(it) },
                        label = { Text(stringResource(R.string.clone_commit_depth_label)) },
                        placeholder = { Text(stringResource(R.string.clone_commit_depth_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        }
    }
}

@Composable
private fun CloneOptionToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (checked) colorScheme.primary.copy(alpha = 0.1f) else colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) colorScheme.primary else colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
