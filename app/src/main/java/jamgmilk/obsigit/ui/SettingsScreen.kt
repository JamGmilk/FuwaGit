package jamgmilk.obsigit.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jamgmilk.obsigit.ui.theme.ObsiGitTheme

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var showPathScans by rememberSaveable { mutableStateOf(false) }
    var autoSync by rememberSaveable { mutableStateOf(false) }
    var credentialsStored by rememberSaveable { mutableStateOf(false) }
    var conflictSafeMode by rememberSaveable { mutableStateOf(true) }
    var backupBeforeSync by rememberSaveable { mutableStateOf(true) }

    AnimatedContent(
        targetState = showPathScans,
        transitionSpec = {
            fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(200))
        },
        label = "settings_path_scans_transition"
    ) { inPathScans ->
        if (inPathScans) {
            PathScansScreen(
                viewModel = viewModel,
                onBack = { showPathScans = false },
                modifier = modifier
            )
            return@AnimatedContent
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = colors.primary
            )

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
                elevation = CardDefaults.elevatedCardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        "Storage",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )

                    ListItem(
                        headlineContent = { Text("Path Scans") },
                        supportingContent = { Text("Manage scanned folders and permission grants") },
                        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                        modifier = Modifier
                            .animateContentSize()
                            .clickable { showPathScans = true },
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
                elevation = CardDefaults.elevatedCardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        "Future Settings",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )

                    ListItem(
                        headlineContent = { Text("Store Git credentials") },
                        supportingContent = { Text("Use secure token or keychain provider") },
                        leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Switch(
                                checked = credentialsStored,
                                onCheckedChange = { credentialsStored = it }
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Auto sync schedule") },
                        supportingContent = { Text("Run periodic pull/push in background") },
                        leadingContent = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Switch(
                                checked = autoSync,
                                onCheckedChange = { autoSync = it }
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Safe conflict strategy") },
                        supportingContent = { Text("Prefer no-overwrite during merge") },
                        leadingContent = { Icon(Icons.Default.Sync, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Switch(
                                checked = conflictSafeMode,
                                onCheckedChange = { conflictSafeMode = it }
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Backup before sync") },
                        supportingContent = { Text("Create snapshot before pull/rebase") },
                        leadingContent = { Icon(Icons.Default.Backup, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Switch(
                                checked = backupBeforeSync,
                                onCheckedChange = { backupBeforeSync = it }
                            )
                        }
                    )
                }
            }
        }
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ObsiGitTheme {
        SettingsScreen(viewModel = AppViewModel())
    }
}
