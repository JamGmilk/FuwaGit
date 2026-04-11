package jamgmilk.fuwagit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jamgmilk.fuwagit.R
import jamgmilk.fuwagit.domain.model.git.ConflictResult
import jamgmilk.fuwagit.domain.model.git.ConflictStatus
import jamgmilk.fuwagit.domain.model.git.GitCommit
import jamgmilk.fuwagit.domain.model.git.GitConflict
import jamgmilk.fuwagit.domain.model.git.GitResetMode
import jamgmilk.fuwagit.ui.util.ViewModelMessagesMapper

enum class DangerousOperationType {
    DELETE_BRANCH,
    DISCARD_CHANGES,
    CLEAN_UNTRACKED,
    MERGE,
    REBASE,
    FORCE_PUSH,
    RESET_HARD,
    DELETE_TAG,
    PUSH_TAG
}

sealed class OperationResult {
    data class Success(val message: String) : OperationResult()
    data class Failure(val error: String, val suggestion: String = "") : OperationResult()
    data class Conflict(val conflictingFiles: List<String>, val message: String) : OperationResult()
}

/**
 * Double Confirmation Dialog - Used for hazardous operations.
 * @param operationType The type of action being performed.
 * @param targetName Name of the operation target (e.g., branch name, filename).
 * @param description A brief description of the operation.
 * @param warningMessage A message highlighting potential risks.
 * @param confirmText Text displayed on the confirmation button (e.g., "DELETE").
 * @param onConfirm Callback invoked when the action is confirmed.
 * @param onDismiss Callback invoked when the action is canceled or dismissed.
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
    val colors = MaterialTheme.colorScheme

    val iconData = Icons.Default.Warning
    val iconColor = colors.tertiary

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
                text = stringResource(R.string.dialog_confirm_dangerous_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )

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
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = iconColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

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
                            GitResetMode.SOFT -> colors.primary
                            GitResetMode.MIXED -> colors.tertiary
                            GitResetMode.HARD -> colors.error
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
                        GitResetMode.SOFT -> colors.primary
                        GitResetMode.MIXED -> colors.tertiary
                        GitResetMode.HARD -> colors.error
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_reset_confirm_title_format, mode.name),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_reset_to_commit),
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
                                text = stringResource(R.string.dialog_reset_hash_label),
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
                        GitResetMode.SOFT -> colors.primary
                        GitResetMode.MIXED -> colors.tertiary
                        GitResetMode.HARD -> colors.error
                    }.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (mode) {
                                GitResetMode.SOFT -> stringResource(R.string.dialog_reset_soft)
                                GitResetMode.MIXED -> stringResource(R.string.dialog_reset_mixed)
                                GitResetMode.HARD -> stringResource(R.string.dialog_reset_hard)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (mode) {
                                GitResetMode.SOFT -> colors.primary
                                GitResetMode.MIXED -> colors.tertiary
                                GitResetMode.HARD -> colors.error
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
                                text = stringResource(R.string.dialog_reset_hard_will),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.error
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dialog_reset_hard_discard_all),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error
                                )
                                Text(
                                    text = stringResource(R.string.dialog_reset_hard_remove_staged),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error
                                )
                                Text(
                                    text = stringResource(R.string.dialog_reset_hard_reset_working),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.error.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.dialog_reset_hard_cannot_undo),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.error,
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
                        GitResetMode.SOFT -> colors.primary
                        GitResetMode.MIXED -> colors.tertiary
                        GitResetMode.HARD -> colors.error
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
                Text(stringResource(R.string.dialog_reset_button_format, mode.name))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun OperationResultDialog(
    result: OperationResult,
    operationType: DangerousOperationType,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val (icon, iconColor, title) = when (result) {
        is OperationResult.Success -> Triple(Icons.Default.CheckCircle, colors.primary, stringResource(R.string.dialog_operation_successful))
        is OperationResult.Failure -> Triple(Icons.Default.Warning, colors.error, stringResource(R.string.dialog_operation_failed))
        is OperationResult.Conflict -> Triple(Icons.Default.Warning, colors.tertiary, stringResource(R.string.dialog_merge_conflicts))
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
                        val messageResId = ViewModelMessagesMapper.mapMessageToResource(result.message)
                        Text(
                            text = stringResource(messageResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                    is OperationResult.Failure -> {
                        val errorResId = ViewModelMessagesMapper.mapMessageToResource(result.error)
                        Text(
                            text = stringResource(R.string.dialog_error_format, stringResource(errorResId)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error
                        )
                        if (result.suggestion.isNotBlank()) {
                            val suggestionResId = ViewModelMessagesMapper.mapMessageToResource(result.suggestion)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dialog_suggestion_format, stringResource(suggestionResId)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is OperationResult.Conflict -> {
                        val messageResId = ViewModelMessagesMapper.mapMessageToResource(result.message)
                        Text(
                            text = stringResource(messageResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.dialog_conflict_files_message),
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
                Text(stringResource(R.string.action_ok))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun CleanPreviewDialog(
    untrackedFiles: List<String>,
    message: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isError = message?.startsWith("Failed") == true || message?.contains("error", ignoreCase = true) == true
    val isLoading = message == stringResource(R.string.dialog_scanning_files)
    val isInfo = message != null && !isError && untrackedFiles.isEmpty() && !isLoading

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        when {
                            isLoading -> colors.secondary
                            isInfo -> colors.tertiary
                            isError -> colors.error
                            else -> colors.secondary
                        }.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = colors.secondary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = when {
                            isInfo -> Icons.Default.Info
                            isError -> Icons.Default.Warning
                            else -> Icons.Default.Clear
                        },
                        contentDescription = null,
                        tint = when {
                            isInfo -> colors.tertiary
                            isError -> colors.error
                            else -> colors.secondary
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = when {
                    isLoading -> stringResource(R.string.dialog_scanning_files)
                    isInfo -> stringResource(R.string.dialog_clean_info)
                    isError -> stringResource(R.string.dialog_clean_failed)
                    else -> stringResource(R.string.dialog_clean_untracked_title)
                },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 显示消息（如果有）
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) colors.error else colors.onSurfaceVariant
                    )
                }

                // 显示文件列表（如果有）
                if (untrackedFiles.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.dialog_clean_files_to_delete_format, untrackedFiles.size),
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
                            text = stringResource(R.string.dialog_reset_hard_cannot_undo),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onErrorContainer
                        )
                    }
                } else if (!isInfo) {
                    // 空状态但非信息消息
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_clean_no_files),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isLoading && untrackedFiles.isNotEmpty() && !isError) {
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
                    Text(stringResource(R.string.dialog_delete_all))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

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
                    .background(colors.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_clean_completed),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_clean_deleted_count_format, cleanedFiles.size),
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
                            text = stringResource(R.string.dialog_clean_no_files_deleted),
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
                Text(stringResource(R.string.action_ok))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

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
            .background(colors.tertiary.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
        ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = colors.tertiary,
            modifier = Modifier.size(28.dp)
        )
        }
        },

        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_merge_conflict_title_format, conflictResult.operationType),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.dialog_conflict_count_format, conflictResult.unresolvedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 操作说明
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
                            text = stringResource(R.string.dialog_how_to_resolve),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.dialog_resolve_step1),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.dialog_resolve_step2),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.dialog_resolve_step3),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // 冲突文件列表
                Text(
                    text = stringResource(R.string.dialog_conflicting_files),
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
                        colors.primary
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
                Text(stringResource(R.string.action_finish))
            }
        },
        dismissButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onAbort) {
                    Text(stringResource(R.string.dialog_abort_format, conflictResult.operationType))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
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
                colors.primary.copy(alpha = 0.1f)
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
                        tint = if (isResolved) colors.primary else colors.tertiary,
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
                        text = stringResource(
                            R.string.dialog_conflict_status_format,
                            when (conflict.status) {
                                ConflictStatus.UNRESOLVED -> stringResource(R.string.dialog_conflict_status_unresolved)
                                ConflictStatus.RESOLVED -> stringResource(R.string.dialog_conflict_status_resolved)
                                ConflictStatus.STAGED -> stringResource(R.string.dialog_conflict_status_staged)
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (conflict.status) {
                            ConflictStatus.UNRESOLVED -> colors.tertiary
                            ConflictStatus.RESOLVED -> colors.primary
                            ConflictStatus.STAGED -> colors.secondary
                        }
                        )

                        if (!isResolved) {
                        Button(
                            onClick = onMarkResolved,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_mark_as_resolved))
                        }
                    }
                }
            }
        }
    }
}
