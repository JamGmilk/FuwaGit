package jamgmilk.fuwagit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.git.ConflictStatus
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitConflict
import jamgmilk.fuwagit.domain.model.git.GitResetMode

/**
 * 鍗遍櫓鎿嶄綔绫诲瀷
 */
enum class DangerousOperationType {
    DELETE_BRANCH,
    DISCARD_CHANGES,
    CLEAN_UNTRACKED,
    MERGE,
    REBASE,
    FORCE_PUSH,
    RESET_HARD
}

/**
 * 鎿嶄綔缁撴灉鐘舵€?
 */
sealed class OperationResult {
    data class Success(val message: String) : OperationResult()
    data class Failure(val error: String, val suggestion: String = "") : OperationResult()
    data class Conflict(val conflictingFiles: List<String>, val message: String) : OperationResult()
}

/**
 * 鍙岀‘璁ゅ璇濇 - 鐢ㄤ簬鍗遍櫓鎿嶄綔
 * 
 * @param operationType 鎿嶄綔绫诲瀷
 * @param targetName 鎿嶄綔鐩爣鍚嶇О锛堝鍒嗘敮鍚嶃€佹枃浠跺悕锛?
 * @param description 鎿嶄綔鎻忚堪
 * @param warningMessage 璀﹀憡淇℃伅
 * @param confirmText 纭鎸夐挳鏂囨湰锛堝 "DELETE"锛?
 * @param onConfirm 纭鍥炶皟
 * @param onDismiss 鍙栨秷鍥炶皟
 */
@Composable
fun TwoStepConfirmDialog(
    operationType: DangerousOperationType,
    targetName: String,
    description: String,
    warningMessage: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var confirmInput by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    val iconData = Icons.Default.Warning
    val iconColor = Color(0xFFFFA726)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconData,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Confirm Dangerous Operation",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 鎿嶄綔鎻忚堪
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                // 鐩爣鍚嶇О楂樹寒
                if (targetName.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = targetName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }

                // 璀﹀憡淇℃伅
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = warningMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onErrorContainer
                    )
                }

                // 绗簩姝ワ細杈撳叆纭
                if (step >= 2) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Type \"$confirmText\" to confirm:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.error
                    )
                    OutlinedTextField(
                        value = confirmInput,
                        onValueChange = { confirmInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.error,
                            focusedLabelColor = colors.error
                        )
                    )
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = { step = 2 },
                    colors = ButtonDefaults.buttonColors(containerColor = iconColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("I Understand")
                }
            } else {
                Button(
                    onClick = onConfirm,
                    enabled = confirmInput == confirmText,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Reset 纭瀵硅瘽妗?
 */
@Composable
fun ResetConfirmDialog(
    commit: GitCommit,
    mode: GitResetMode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        when (mode) {
                            GitResetMode.SOFT -> Color(0xFF4CAF50)
                            GitResetMode.MIXED -> Color(0xFFFF9800)
                            GitResetMode.HARD -> Color(0xFFF44336)
                        }.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (mode) {
                        GitResetMode.SOFT -> Icons.Default.ArrowUpward
                        GitResetMode.MIXED -> Icons.Default.Replay
                        GitResetMode.HARD -> Icons.Default.DeleteForever
                    },
                    contentDescription = null,
                    tint = when (mode) {
                        GitResetMode.SOFT -> Color(0xFF4CAF50)
                        GitResetMode.MIXED -> Color(0xFFFF9800)
                        GitResetMode.HARD -> Color(0xFFF44336)
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Confirm ${when (mode) {
                    GitResetMode.SOFT -> "Soft"
                    GitResetMode.MIXED -> "Mixed"
                    GitResetMode.HARD -> "Hard"
                }} Reset",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Reset current branch to commit:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row {
                            Text(
                                text = "Hash: ",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                text = commit.shortHash,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Text(
                            text = commit.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (mode) {
                        GitResetMode.SOFT -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        GitResetMode.MIXED -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        GitResetMode.HARD -> Color(0xFFF44336).copy(alpha = 0.1f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (mode) {
                                GitResetMode.SOFT -> "Soft Reset: Move HEAD only"
                                GitResetMode.MIXED -> "Mixed Reset: Move HEAD and unstage changes"
                                GitResetMode.HARD -> "Hard Reset: Discard ALL changes"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (mode) {
                                GitResetMode.SOFT -> Color(0xFF2E7D32)
                                GitResetMode.MIXED -> Color(0xFFE65100)
                                GitResetMode.HARD -> Color(0xFFC62828)
                            }
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )

                        if (mode == GitResetMode.HARD) {
                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "This will:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "鈥?Discard ALL uncommitted changes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    text = "鈥?Remove ALL staged changes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    text = "鈥?Reset working directory to match the selected commit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF44336).copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "鈿狅笍 This action CANNOT be undone!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (mode) {
                        GitResetMode.SOFT -> Color(0xFF4CAF50)
                        GitResetMode.MIXED -> Color(0xFFFF9800)
                        GitResetMode.HARD -> Color(0xFFF44336)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = when (mode) {
                        GitResetMode.SOFT -> Icons.Default.ArrowUpward
                        GitResetMode.MIXED -> Icons.Default.Replay
                        GitResetMode.HARD -> Icons.Default.DeleteForever
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Reset ${when (mode) {
                    GitResetMode.SOFT -> "Soft"
                    GitResetMode.MIXED -> "Mixed"
                    GitResetMode.HARD -> "Hard"
                }}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}/**
 * 鎿嶄綔缁撴灉瀵硅瘽妗?- 鏄剧ず鎴愬姛銆佸け璐ユ垨鍐茬獊淇℃伅
 */
@Composable
fun OperationResultDialog(
    result: OperationResult,
    operationType: DangerousOperationType,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val (icon, iconColor, title) = when (result) {
        is OperationResult.Success -> Triple(Icons.Default.CheckCircle, Color(0xFF4CAF50), "Operation Successful")
        is OperationResult.Failure -> Triple(Icons.Default.Warning, Color(0xFFFF5722), "Operation Failed")
        is OperationResult.Conflict -> Triple(Icons.Default.Warning, Color(0xFFFF9800), "Merge Conflicts Detected")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (result) {
                    is OperationResult.Success -> {
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                    is OperationResult.Failure -> {
                        Text(
                            text = "Error: ${result.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error
                        )
                        if (result.suggestion.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Suggestion: ${result.suggestion}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is OperationResult.Conflict -> {
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = "The following files have conflicts:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.error
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.errorContainer.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                result.conflictingFiles.forEach { file ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = null,
                                            tint = colors.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = file,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("OK")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Clean 鎿嶄綔棰勮瀵硅瘽妗?
 */
@Composable
fun CleanPreviewDialog(
    untrackedFiles: List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF2196F3).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Clean Untracked Files",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "The following ${untrackedFiles.size} untracked file(s) will be permanently deleted:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .background(colors.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        untrackedFiles.forEach { file ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = file,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "鈿狅笍 This action cannot be undone!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onErrorContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Delete All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Clean 鎿嶄綔缁撴灉瀵硅瘽妗?- 鏄剧ず宸插垹闄ょ殑鏂囦欢鍒楄〃
 */
@Composable
fun CleanResultDialog(
    cleanedFiles: List<String>,
    onSuccess: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Clean Completed",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Successfully deleted ${cleanedFiles.size} file(s):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

                if (cleanedFiles.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .background(colors.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            cleanedFiles.forEach { file ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DeleteForever,
                                        contentDescription = null,
                                        tint = colors.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = file,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "No files were deleted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSuccess()
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("OK")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * 鍐茬獊瑙ｅ喅瀵硅瘽妗?
 */
@Composable
fun ConflictResolutionDialog(
    conflictResult: ConflictResult,
    onResolveConflict: (String) -> Unit,
    onFinish: () -> Unit,
    onAbort: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var expandedConflict by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFFF9800).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${conflictResult.operationType} Conflicts",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${conflictResult.unresolvedCount} conflict(s) need resolution",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 鎿嶄綔璇存槑
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "How to resolve:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = "1. Edit each file to resolve conflict markers (<<<<<<, ======, >>>>>>)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = "2. Click 'Mark as Resolved' for each file after editing",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = "3. Click 'Finish' when all conflicts are resolved",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // 鍐茬獊鏂囦欢鍒楄〃
                Text(
                    text = "Conflicting Files:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    conflictResult.conflicts.forEach { conflict ->
                        ConflictFileItem(
                            conflict = conflict,
                            isExpanded = expandedConflict == conflict.path,
                            onExpand = { expandedConflict = if (expandedConflict == conflict.path) null else conflict.path },
                            onMarkResolved = { onResolveConflict(conflict.path) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onFinish,
                enabled = conflictResult.allResolved || conflictResult.allStaged,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (conflictResult.allResolved || conflictResult.allStaged) {
                        Color(0xFF4CAF50)
                    } else {
                        colors.primary.copy(alpha = 0.5f)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Finish")
            }
        },
        dismissButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onAbort) {
                    Text("Abort ${conflictResult.operationType}")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ConflictFileItem(
    conflict: GitConflict,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onMarkResolved: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isResolved = conflict.status != ConflictStatus.UNRESOLVED

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isResolved) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                colors.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpand() }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isResolved) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = conflict.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = conflict.path,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                IconButton(
                    onClick = { onExpand() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()

                    Text(
                        text = "Status: ${when (conflict.status) {
                            ConflictStatus.UNRESOLVED -> "Unresolved"
                            ConflictStatus.RESOLVED -> "Resolved"
                            ConflictStatus.STAGED -> "Staged"
                        }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (conflict.status) {
                            ConflictStatus.UNRESOLVED -> Color(0xFFFF9800)
                            ConflictStatus.RESOLVED -> Color(0xFF4CAF50)
                            ConflictStatus.STAGED -> Color(0xFF2196F3)
                        }
                    )

                    if (!isResolved) {
                        Button(
                            onClick = onMarkResolved,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Mark as Resolved")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CleanLoadingDialog() {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = {},
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF2196F3).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF2196F3),
                    strokeWidth = 2.5.dp
                )
            }
        },
        title = {
            Text(
                text = "Scanning Files",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "Analyzing untracked files in working directory...",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        },
        confirmButton = {},
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun CleanMessageDialog(
    message: String,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isError = message.startsWith("Failed") || message.contains("error", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isError) Color(0xFFE53935).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isError) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isError) Color(0xFFE53935) else Color(0xFFFF9800),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = if (isError) "Clean Failed" else "Clean Info",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) colors.error else colors.onSurfaceVariant
                )

                if (!isError) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "The repository is already clean 鈥?no untracked files found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("OK")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
