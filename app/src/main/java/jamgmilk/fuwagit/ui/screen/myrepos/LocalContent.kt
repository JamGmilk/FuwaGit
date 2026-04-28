package jamgmilk.fuwagit.ui.screen.myrepos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.ui.components.FilePickerDialog
import jamgmilk.fuwagit.ui.components.SectionCard
import jamgmilk.fuwagit.ui.theme.AppShapes
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
    var selectedRemoteIndex by remember { mutableIntStateOf(0) }
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
            alias = File(path).name
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionCard(title = if (isGitRepo) stringResource(R.string.local_repo_detected) else stringResource(R.string.local_new_repo_setup)) {
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it },
                        label = { Text(stringResource(R.string.local_display_name_label)) },
                        placeholder = { Text(stringResource(R.string.local_display_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.extraSmall,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) }
                    )
                }

                if (isGitRepo && remotes.isNotEmpty()) {
                    SectionCard(title = stringResource(R.string.local_remote_info)) {
                        RemoteSelectorDropdown(
                            remotes = remotes,
                            selectedIndex = selectedRemoteIndex,
                            onSelected = { index ->
                                selectedRemoteIndex = index
                                remoteUrl = remotes[index].second
                            }
                        )
                    }
                } else if (!isGitRepo) {
                    SectionCard(title = stringResource(R.string.local_remote_url_header)) {
                        OutlinedTextField(
                            value = remoteUrl,
                            onValueChange = { remoteUrl = it },
                            label = { Text(stringResource(R.string.local_remote_url_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShapes.extraSmall,
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Link, null) }
                        )
                    }
                }
            }
        }

        if (path.isBlank()) {
            InfoGuideCard(message = stringResource(R.string.local_info_guide))
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
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = AppShapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(12.dp))
            Text(if (isGitRepo) stringResource(R.string.action_add) else stringResource(R.string.action_initialize), style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showFolderPicker) {
        FilePickerDialog(
            title = stringResource(R.string.local_select_folder),
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
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = pluralStringResource(R.plurals.remote_count, remotes.size),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
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
                        tint = if (index == selectedIndex) colors.primary else colors.onSurfaceVariant,
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
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
