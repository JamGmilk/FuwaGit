package jamgmilk.obsigit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.obsigit.ui.theme.CatNight
import jamgmilk.obsigit.ui.theme.Sakura30

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun GitTerminalScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isRepo by viewModel.isGitRepo.collectAsState()
    val statusText by viewModel.gitStatusText.collectAsState()
    val terminalLogs by viewModel.terminalOutput.collectAsState()
    val targetPath by viewModel.targetPath.collectAsState()
    val availablePaths by viewModel.availableTargetPaths.collectAsState()

    var showCommitDialog by remember { mutableStateOf(false) }
    var showPathMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(280)) + slideInVertically(
                initialOffsetY = { -it / 5 },
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            )
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, colors.outline.copy(alpha = 0.35f)), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isRepo) colors.surface.copy(alpha = 0.92f) else colors.errorContainer.copy(alpha = 0.86f)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRepo) "Repository Active" else "Repository Not Found",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.primary
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box {
                        OutlinedButton(onClick = { showPathMenu = true }) {
                            Text(text = targetPath ?: "Select vault folder")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = showPathMenu, onDismissRequest = { showPathMenu = false }) {
                            if (availablePaths.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No selectable local folders") },
                                    onClick = { showPathMenu = false }
                                )
                            } else {
                                availablePaths.forEach { path ->
                                    DropdownMenuItem(
                                        text = { Text(path) },
                                        onClick = {
                                            viewModel.setTargetPath(path)
                                            showPathMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isRepo) {
                Button(
                    onClick = { viewModel.initRepo() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(16.dp),
                    enabled = targetPath != null
                ) { Text("git init") }
            } else {
                PinkActionButton("status", onClick = { viewModel.showStatusInTerminal() })
                PinkActionButton("stage all", onClick = { viewModel.stageAll() })
                PinkActionButton("unstage", onClick = { viewModel.unstageAll() }, outlined = true)
                PinkActionButton("commit", onClick = { showCommitDialog = true })
                PinkActionButton("pull", onClick = { viewModel.pullRepo() })
                PinkActionButton("push", onClick = { viewModel.pushRepo() })
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Terminal Logs",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onBackground,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )

            val listState = rememberLazyListState()
            LaunchedEffect(terminalLogs.size) {
                if (terminalLogs.isNotEmpty()) {
                    listState.animateScrollToItem(terminalLogs.size - 1)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(CatNight)
                    .padding(12.dp)
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(terminalLogs) { log ->
                        Text(
                            text = log,
                            color = Sakura30,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .animateItem()
                        )
                    }
                }
            }
        }
    }

    if (showCommitDialog) {
        var commitMessage by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCommitDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = colors.surface,
            title = { Text("Commit Message", color = colors.primary) },
            text = {
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    placeholder = { Text("Describe your changes") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.commitChanges(commitMessage)
                        showCommitDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) { Text("Commit") }
            }
        )
    }
}

@Composable
fun PinkActionButton(text: String, onClick: () -> Unit, outlined: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            border = BorderStroke(1.5.dp, colors.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.animateContentSize()
        ) {
            Text(text, color = colors.primary, fontWeight = FontWeight.Bold)
        }
    } else {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.animateContentSize()
        ) {
            Text(text, color = colors.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}
