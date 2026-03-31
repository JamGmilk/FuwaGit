package jamgmilk.fuwagit.ui.screen.myrepos

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.ui.components.SubSettingsTemplate
import jamgmilk.fuwagit.ui.theme.Sakura80
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AddLocalRepositoryScreen(
    initialPath: String = "",
    onBack: () -> Unit,
    onAddRepository: (path: String, alias: String?) -> Unit,
    onPickFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var path by remember { mutableStateOf(initialPath) }
    var alias by remember { mutableStateOf("") }
    var isGitRepo by remember { mutableStateOf(false) }

    LaunchedEffect(initialPath) {
        if (initialPath.isNotBlank()) {
            path = initialPath
            if (alias.isBlank()) {
                alias = initialPath.substringAfterLast("/")
            }
            isGitRepo = File(initialPath, ".git").exists()
        }
    }

    LaunchedEffect(path) {
        if (path.isNotBlank()) {
            isGitRepo = File(path, ".git").exists()
            if (alias.isBlank() || alias == path.substringAfterLast("/")) {
                alias = path.substringAfterLast("/")
            }
        }
    }

    SubSettingsTemplate(
        title = "Add Local Repository",
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(
                        color = Sakura80.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Sakura80,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Select a local Git repository folder to add to your repository list.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            FolderSelectorCard(
                path = path,
                isGitRepo = isGitRepo,
                onPickFolder = onPickFolder
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
                    focusedBorderColor = Sakura80,
                    focusedLabelColor = Sakura80,
                    cursorColor = Sakura80
                )
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (path.isNotBlank()) {
                        onAddRepository(path, alias.ifBlank { null })
                        Toast.makeText(context, "Repository added", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = path.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Sakura80)
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
                    .background(Sakura80.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = "Pick folder",
                    tint = Sakura80,
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
