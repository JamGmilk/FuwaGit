package jamgmilk.obsigit.ui

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.obsigit.ui.theme.ObsiGitTheme
import jamgmilk.obsigit.ui.theme.ObsiGitThemeExtras

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun GitTerminalScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val uiColors = ObsiGitThemeExtras.colors
    val isRepo by viewModel.isGitRepo.collectAsState()
    val statusText by viewModel.gitStatusText.collectAsState()
    val terminalLogs by viewModel.terminalOutput.collectAsState()
    val targetPath by viewModel.targetPath.collectAsState()

    var showCommitDialog by remember { mutableStateOf(false) }

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
                    .border(
                        BorderStroke(1.dp, uiColors.cardBorder),
                        RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isRepo) uiColors.cardContainer else colors.errorContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRepo) "Repository Active" else "Repository Not Found",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.primary
                        )
                        Spacer(Modifier.height(8.dp))
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
                    shape = RoundedCornerShape(16.dp),   // TODO: 统一 App 里的全部 Button 圆角喵~
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
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
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
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(uiColors.terminalBackground)
                    //.background(colors.surfaceVariant.copy(alpha = 0.88f))
                    .padding(12.dp)


            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(uiColors.terminalBackground),
                    contentPadding = PaddingValues(4.dp)
                    ) {
                    items(terminalLogs) { log ->
                        Text(
                            text = log,
                            color = uiColors.terminalText,
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
            shape = RoundedCornerShape(24.dp),
            containerColor = colors.surface,
            title = { Text("Commit Message", color = colors.primary) },
            text = {
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    placeholder = { Text("Describe your changes") },
                    // TODO: 这里可以使用模板，包含时间
                    shape = RoundedCornerShape(12.dp),
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
            // TODO: 这里给按钮来个图标喵~
            border = BorderStroke(1.5.dp, colors.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.animateContentSize()
        ) {
            Text(text, color = colors.primary, fontWeight = FontWeight.Bold)
        }
    } else {
        Button(
            onClick = onClick,
            // TODO: 这里给按钮来个图标喵~
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.animateContentSize()
        ) {
            Text(text, color = colors.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun GitTerminalScreenPreview() {
    ObsiGitTheme {
        GitTerminalScreen(viewModel = AppViewModel())
    }
}
