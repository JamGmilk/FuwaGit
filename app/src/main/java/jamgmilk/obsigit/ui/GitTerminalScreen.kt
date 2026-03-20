package jamgmilk.obsigit.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel


// --- Kawaii Color Palette ---
val SakuraPink = Color(0xFFFFD1DC)
val DeepSakura = Color(0xFFFF91A4)
val LavenderMist = Color(0xFFE6E6FA)
val CatPurrDark = Color(0xFF2D242F) // Soft dark purple for terminal
val TextWhite = Color(0xFFFFF5F7)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun GitTerminalScreen(
    modifier: Modifier = Modifier,
    viewModel: GitTerminalViewModel = viewModel()
) {
    val isRepo by viewModel.isGitRepo.collectAsState()
    val statusText by viewModel.gitStatusText.collectAsState()
    val terminalLogs by viewModel.terminalOutput.collectAsState()
    var showCommitDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LavenderMist, SakuraPink)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Header & Status ---
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + expandVertically()
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.White, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    // CHANGE THIS LINE:
                    // Use Color.Transparent for totally clear,
                    // or Color.White.copy(alpha = 0.2f) for a soft "glass" look!
                    containerColor = if (isRepo) Color.White.copy(alpha = 0.3f) else Color(0xFFFFEBEE).copy(alpha = 0.5f)
                ),
                // Optional: Reduce elevation to 0 if you don't want a shadow
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRepo) "Repo Active" else "No Repo Found... nya?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepSakura
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = DeepSakura,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // --- 2. Action Buttons (Modern Pill Style) ---
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val buttonModifier = Modifier.animateContentSize()

            if (!isRepo) {
                Button(
                    onClick = { viewModel.initRepo() },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSakura),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("git init ✨") }
            } else {
                PinkActionButton("status", onClick = { viewModel.showStatusInTerminal() })
                PinkActionButton("stage all", onClick = { viewModel.stageAll() })
                PinkActionButton("unstage", onClick = { viewModel.unstageAll() }, outlined = true)
                PinkActionButton("commit", onClick = { showCommitDialog = true })
                PinkActionButton("pull", onClick = { viewModel.pullRepo() })
                PinkActionButton("push", onClick = { viewModel.pushRepo() })
            }
        }

        // --- 3. Terminal Output Panel (Frosted Dark Style) ---
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Terminal Logs",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }

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
                    .background(CatPurrDark)
                    .padding(12.dp)
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(terminalLogs) { log ->
                        Text(
                            text = log,
                            color = SakuraPink, // Pink text for that catgirl terminal look
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .animateItem( // Use this instead!
                                    fadeInSpec = spring(),
                                    fadeOutSpec = spring(),
                                    placementSpec = spring()
                                )
                        )
                    }
                }
            }
        }
    }

    // --- Commit Dialog ---
    if (showCommitDialog) {
        var commitMessage by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCommitDialog = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            title = { Text("Write a message! 🐾", color = DeepSakura) },
            text = {
                OutlinedTextField(
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    placeholder = { Text("What did you change, nya?") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSakura)
                ) { Text("Commit!") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class) // Needed for the LazyColumn animation
@Composable
fun PinkActionButton(text: String, onClick: () -> Unit, outlined: Boolean = false) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            // FIXED: Capital B and Correct order (Width, Color)
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DeepSakura),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text, color = DeepSakura, fontWeight = FontWeight.Bold)
        }
    } else {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = DeepSakura),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}